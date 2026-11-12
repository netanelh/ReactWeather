package com.natan.vortex.nav;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.pathing.NavSettings;
import com.natan.vortex.util.Chat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns the current process, runs path searches off-thread and drives the executor. */
public final class Navigator {
    public static final Navigator INSTANCE = new Navigator();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vortex-Pathfinder");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private Process process;
    private Executor executor;
    private Goal pathGoal;
    private AtomicBoolean cancel = new AtomicBoolean();
    private volatile boolean calculating;
    private boolean paused;
    private int failures;
    private int repathCooldown;
    private boolean wasDead;
    private String lastCalc = "";

    private Navigator() {}

    public boolean active() { return process != null; }
    public boolean paused() { return paused; }
    public Process process() { return process; }
    public Executor executor() { return executor; }

    public void start(Process p) {
        stop(false);
        process = p;
        failures = 0;
        paused = false;
        World.clearFailed();
        Chat.info("§a" + p.name() + "§r started. Type §e.stop§r to cancel.");
    }

    public void stop(boolean announce) {
        cancel.set(true);
        if (process != null) {
            process.onStop();
            if (announce) Chat.info(process.name() + " stopped.");
            VortexClient.modules.pathingStopped(process);
        }
        process = null;
        executor = null;
        pathGoal = null;
        calculating = false;
        Actions.releaseKeys();
        Safety.reset();
        Minecraft mc = Minecraft.getInstance();
        Actions.stopMining();
    }

    public void setPaused(boolean p) {
        paused = p;
        if (p) { Actions.releaseKeys(); Safety.reset(); }
        Chat.info(p ? "Paused." : "Resumed.");
    }

    public String hudStatus() {
        if (process == null) return null;
        StringBuilder sb = new StringBuilder(process.name());
        String st = process.status();
        if (!st.isEmpty()) sb.append(" · ").append(st);
        if (paused) sb.append(" · paused");
        else if (calculating) sb.append(" · thinking...");
        else if (executor != null) sb.append(" · ").append(executor.remaining()).append(" steps");
        return sb.toString();
    }

    public String lastCalc() { return lastCalc; }

    /** Goal the current path leads to (for the HUD arrow / distance). */
    public Goal currentGoal() {
        try { return process == null ? null : process.goal(); } catch (Throwable t) { return null; }
    }

    private void crashed(Throwable t) {
        VortexClient.LOG.error("Process {} crashed", process.name(), t);
        Chat.error(process.name() + " crashed: " + t.getMessage());
        stop(false);
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) { if (process != null) stop(false); return; }

        // automatic death waypoint
        boolean dead = mc.gui.screen() instanceof DeathScreen;
        if (dead && !wasDead) {
            Waypoints.INSTANCE.set("death", mc.player.blockPosition());
            if (process != null) stop(false);
        }
        wasDead = dead;

        if (process == null || paused) return;
        NavSettings s = VortexClient.modules.get(NavSettings.class);
        snapshotHostiles(mc, s);

        // ---- survival helpers that pre-empt any task ----
        if (Safety.tick(s)) { executor = null; return; }

        try {
            process.tick();
        } catch (Throwable t) {
            crashed(t);
            return;
        }
        if (process == null) return;
        if (process.isDone()) {
            Chat.info("§a" + process.name() + " finished.");
            stop(false);
            return;
        }

        Goal goal;
        try {
            goal = process.goal();
            if (goal == null) { if (executor != null) Actions.releaseKeys(); executor = null; pathGoal = null; return; } // process is acting in place

            BlockPos feet = feet();
            if (goal.isGoal(feet.getX(), feet.getY(), feet.getZ())) {
                executor = null; pathGoal = null;
                Actions.releaseKeys();
                process.atGoal();
                return;
            }
        } catch (Throwable t) {
            crashed(t);
            return;
        }

        // goal moved (e.g. following a player) -> re-plan
        if (pathGoal != null && !pathGoal.equals(goal) && executor != null && repathCooldown <= 0) {
            executor = null;
        }
        if (repathCooldown > 0) repathCooldown--;

        if (executor == null) {
            if (!calculating && repathCooldown <= 0) calculate(goal, s);
            Actions.releaseKeys();
            return;
        }
        Executor.Status status = executor.tick();
        if (status == Executor.Status.DONE) {
            executor = null; // partial path finished -> next tick plans the rest
        } else if (status == Executor.Status.FAILED) {
            // remember the step that failed so the next plan avoids repeating it
            World.markFailed(executor.steps().get(Math.min(executor.index(), executor.steps().size() - 1)).pos());
            executor = null;
            Actions.stopMining();
            Actions.releaseKeys();
            repathCooldown = 5;
        }
    }

    private void snapshotHostiles(Minecraft mc, NavSettings s) {
        if (!s.mobAvoid.on()) { World.setHostiles(new int[0]); return; }
        java.util.List<Integer> pts = new java.util.ArrayList<>();
        int count = 0;
        for (var e : mc.level.entitiesForRendering()) {
            if (count >= 40) break;
            if (!com.natan.vortex.util.Targets.isHostile(e) || !e.isAlive()) continue;
            if (e.distanceToSqr(mc.player) > 48 * 48) continue;
            pts.add(e.getBlockX()); pts.add(e.getBlockY()); pts.add(e.getBlockZ()); count++;
        }
        int[] arr = new int[pts.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = pts.get(i);
        World.setHostiles(arr);
    }

    private BlockPos feet() {
        var p = Minecraft.getInstance().player;
        BlockPos b = BlockPos.containing(p.getX(), p.getY() + 0.2, p.getZ());
        // standing on slabs/soul sand etc. rounds into the floor block
        if (!World.passable(b.getX(), b.getY(), b.getZ())) b = b.above();
        return b;
    }

    private void calculate(Goal goal, NavSettings s) {
        calculating = true;
        cancel = new AtomicBoolean();
        AtomicBoolean myCancel = cancel;
        BlockPos start = feet();
        PathFinder.Config cfg = new PathFinder.Config(s.allowBreak.on(), s.allowPlace.on(), s.maxFall.i(), s.sprint.on(),
            s.avoidWater.on(), s.maxNodes.i() * 1000, s.timeout.i() * 1000L, Actions.countThrowaways(), s.parkour.on(), s.longRange.on());
        Process forProcess = process;
        worker.submit(() -> {
            PathFinder.Result r;
            try {
                r = new PathFinder(cfg).find(start, goal, myCancel);
            } catch (Throwable t) {
                VortexClient.LOG.error("Pathfinder crashed", t);
                r = null;
            }
            PathFinder.Result result = r;
            Minecraft.getInstance().execute(() -> onResult(result, goal, forProcess, myCancel, s));
        });
    }

    private void onResult(PathFinder.Result r, Goal goal, Process forProcess, AtomicBoolean myCancel, NavSettings s) {
        if (myCancel.get() || process != forProcess) return;
        calculating = false;
        if (r == null || r.steps().size() < 2) {
            failures++;
            lastCalc = "no path (" + (r == null ? 0 : r.expanded()) + " nodes)";
            if (failures >= 4 && process.onPathFailed()) { failures = 0; repathCooldown = 5; return; }
            if (failures >= 4) {
                Chat.error("Can't find a path to " + goal.describe() + ". Try enabling Allow Break / Allow Place.");
                stop(false);
            } else repathCooldown = 20;
            return;
        }
        failures = 0;
        lastCalc = r.steps().size() + " steps, " + r.expanded() + " nodes, " + r.millis() + "ms" + (r.complete() ? "" : " (partial)");
        if (s.debug.on()) Chat.info("Path: " + lastCalc);
        executor = new Executor(r.steps(), s.sprint.on());
        pathGoal = goal;
    }
}
