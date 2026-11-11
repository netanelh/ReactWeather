package com.natan.vortex.nav;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/** Weighted A* over the block grid. Runs on the navigator's background thread. */
public final class PathFinder {
    public record Config(boolean allowBreak, boolean allowPlace, int maxFall, boolean sprint, boolean avoidWater,
                         int maxNodes, long timeoutMs, int throwaways, boolean parkour, boolean useMemory) {}

    public record Result(List<Step> steps, boolean complete, int expanded, long millis) {}

    private static final double SQRT2 = Math.sqrt(2);
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private static final class Node {
        final int x, y, z;
        double g, f;
        Node parent;
        Step.Move move;
        List<BlockPos> breaks;
        BlockPos place;
        boolean closed;
        Node(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    }

    private record Entry(Node node, double f) {}

    private final Config cfg;
    private final double walk;

    public PathFinder(Config cfg) {
        this.cfg = cfg;
        this.walk = cfg.sprint() ? 3.564 : 4.633;
    }

    private static long key(int x, int y, int z) { return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (y & 0xFFFL); }

    public Result find(BlockPos start, Goal goal, AtomicBoolean cancel) {
        long t0 = System.currentTimeMillis();
        Map<Long, Node> nodes = new HashMap<>();
        PriorityQueue<Entry> open = new PriorityQueue<>((a, b) -> Double.compare(a.f, b.f));
        Node s = new Node(start.getX(), start.getY(), start.getZ());
        s.move = Step.Move.START;
        s.f = goal.heuristic(s.x, s.y, s.z);
        nodes.put(key(s.x, s.y, s.z), s);
        open.add(new Entry(s, s.f));

        Node best = s;
        double bestH = s.f;
        int expanded = 0;

        while (!open.isEmpty()) {
            if (cancel.get()) return null;
            if (expanded >= cfg.maxNodes() || System.currentTimeMillis() - t0 > cfg.timeoutMs()) break;
            Node cur = open.poll().node;
            if (cur.closed) continue;
            cur.closed = true;
            expanded++;
            if (goal.isGoal(cur.x, cur.y, cur.z)) return new Result(build(cur), true, expanded, System.currentTimeMillis() - t0);
            double h = goal.heuristic(cur.x, cur.y, cur.z);
            if (h < bestH) { bestH = h; best = cur; }

            for (Candidate c : neighbors(cur)) {
                long k = key(c.x, c.y, c.z);
                Node n = nodes.get(k);
                double g = cur.g + c.cost + World.hostilePenalty(c.x, c.y, c.z);
                if (n == null) {
                    n = new Node(c.x, c.y, c.z);
                    nodes.put(k, n);
                } else if (n.closed || g >= n.g) continue;
                n.g = g;
                n.parent = cur;
                n.move = c.move;
                n.breaks = c.breaks;
                n.place = c.place;
                n.f = g + goal.heuristic(c.x, c.y, c.z) * 1.15; // slightly greedy = much faster, still near-optimal
                open.add(new Entry(n, n.f));
            }
        }
        // no full path: return the best partial path so the navigator can make progress and re-plan
        if (best == s) return new Result(List.of(), false, expanded, System.currentTimeMillis() - t0);
        return new Result(build(best), false, expanded, System.currentTimeMillis() - t0);
    }

    private List<Step> build(Node end) {
        List<Step> out = new ArrayList<>();
        for (Node n = end; n != null; n = n.parent)
            out.add(new Step(new BlockPos(n.x, n.y, n.z), n.move, n.breaks == null ? List.of() : n.breaks, n.place));
        Collections.reverse(out);
        return out;
    }

    private record Candidate(int x, int y, int z, double cost, Step.Move move, List<BlockPos> breaks, BlockPos place) {}

    /** Cost to clear a cell for the body, or -1 if impossible. Adds the cell to breaks if mining is needed. */
    private double clear(int x, int y, int z, List<BlockPos> breaks) {
        if (World.passable(x, y, z)) return World.inWater(x, y, z) ? (cfg.avoidWater() ? 8 : 2) : 0;
        if (!cfg.allowBreak()) return -1;
        double c = World.breakCost(x, y, z);
        if (c < 0) return -1;
        breaks.add(new BlockPos(x, y, z));
        return c;
    }

    private Candidate memoryStep(int x, int y, int z, int nx, int nz) {
        int surf = com.natan.vortex.nav.NavMemory.INSTANCE.surfaceY(nx, nz);
        if (surf == Integer.MIN_VALUE) return null;                 // unknown terrain: don't guess
        if (com.natan.vortex.nav.NavMemory.INSTANCE.dangerAt(nx, nz)) return null;
        int ny = surf + 1;                                          // feet above the remembered floor
        int dy = ny - y;
        if (dy > 4 || dy < -cfg.maxFall() - 8) return null;         // too big a change to trust blindly
        // extra cost so A* prefers loaded, verified routes and treats memory as a last resort
        return new Candidate(nx, ny, nz, (walk + Math.abs(dy) * 2) * 1.4 + 6, Step.Move.WALK, null, null);
    }

    private List<Candidate> neighbors(Node n) {
        List<Candidate> out = new ArrayList<>(12);
        int x = n.x, y = n.y, z = n.z;
        boolean inWater = World.inWater(x, y, z);

        for (int[] d : DIRS) {
            int nx = x + d[0], nz = z + d[1];
            boolean diagonal = d[0] != 0 && d[1] != 0;
            if (!World.loaded(nx, nz)) {
                // beyond loaded chunks: step across the remembered walk-map, re-planned once it loads
                if (cfg.useMemory() && !diagonal) {
                    Candidate mc = memoryStep(x, y, z, nx, nz);
                    if (mc != null) out.add(mc);
                }
                continue;
            }

            if (diagonal) {
                // no mining or bridging on diagonals; both corners must be open (no clipping through edges)
                if (!World.passable(nx, y, nz) || !World.passable(nx, y + 1, nz)) continue;
                if (!World.passable(x + d[0], y, z) || !World.passable(x + d[0], y + 1, z)) continue;
                if (!World.passable(x, y, z + d[1]) || !World.passable(x, y + 1, z + d[1])) continue;
                if (!World.standable(nx, y - 1, nz)) continue;
                out.add(new Candidate(nx, y, nz, walk * SQRT2 * (inWater ? 2 : 1), Step.Move.DIAGONAL, null, null));
                continue;
            }

            // --- flat walk (optionally mining and/or bridging) ---
            List<BlockPos> br = new ArrayList<>(2);
            double c1 = clear(nx, y, nz, br), c2 = c1 < 0 ? -1 : clear(nx, y + 1, nz, br);
            if (c1 >= 0 && c2 >= 0) {
                if (World.standable(nx, y - 1, nz)) {
                    out.add(new Candidate(nx, y, nz, walk + c1 + c2, Step.Move.WALK, br, null));
                } else if (cfg.allowPlace() && cfg.throwaways() > 0 && World.replaceable(nx, y - 1, nz)
                    && !World.isLavaAt(nx, y - 2, nz)) {
                    // bridge: place a block under the next cell
                    out.add(new Candidate(nx, y, nz, walk + c1 + c2 + 22, Step.Move.WALK, br, new BlockPos(nx, y - 1, nz)));
                }
            }

            // --- parkour: sprint-jump over a 1-2 block gap ---
            if (cfg.parkour() && !inWater && World.standable(x, y - 1, z) && World.passable(x, y + 2, z)
                && World.passable(nx, y, nz) && World.passable(nx, y + 1, nz) && World.passable(nx, y + 2, nz)
                && !World.standable(nx, y - 1, nz) && !World.inWater(nx, y - 1, nz)) {
                for (int gap = 2; gap <= 3; gap++) {
                    int lx = x + d[0] * gap, lz = z + d[1] * gap;
                    if (!World.loaded(lx, lz)) break;
                    if (!World.passable(lx, y, lz) || !World.passable(lx, y + 1, lz)) break;
                    if (World.standable(lx, y - 1, lz)) {
                        out.add(new Candidate(lx, y, lz, walk * gap + 6, Step.Move.PARKOUR, null, null));
                        break;
                    }
                    if (!World.passable(lx, y + 2, lz)) break;
                }
            }

            // --- step up one block ---
            if (World.standable(nx, y, nz) && !World.inWater(nx, y, nz)) {
                List<BlockPos> ub = new ArrayList<>(3);
                double a = clear(x, y + 2, z, ub);
                double b = a < 0 ? -1 : clear(nx, y + 1, nz, ub);
                double e = b < 0 ? -1 : clear(nx, y + 2, nz, ub);
                if (a >= 0 && b >= 0 && e >= 0)
                    out.add(new Candidate(nx, y + 1, nz, walk + 2.5 + a + b + e, Step.Move.ASCEND, ub, null));
            }

            // --- step/fall down ---
            if (World.passable(nx, y, nz) && World.passable(nx, y + 1, nz) && !World.standable(nx, y - 1, nz)) {
                for (int k = 1; k <= Math.max(cfg.maxFall(), 1) + 20; k++) {
                    int fy = y - k;
                    if (fy < -64) break;
                    if (World.inWater(nx, fy, nz)) { // landing in water is always safe
                        out.add(new Candidate(nx, fy, nz, walk + k * 1.2 + 2, Step.Move.DESCEND, null, null));
                        break;
                    }
                    if (World.standable(nx, fy - 1, nz)) {
                        if (k <= cfg.maxFall()) out.add(new Candidate(nx, fy, nz, walk + k * 1.2, Step.Move.DESCEND, null, null));
                        break;
                    }
                    if (!World.passable(nx, fy, nz)) break;
                }
            }
        }

        // --- vertical moves ---
        if (inWater && World.passable(x, y + 1, z) && World.passable(x, y + 2, z))
            out.add(new Candidate(x, y + 1, z, 3, Step.Move.SWIM_UP, null, null));

        // climb up/down ladders, vines and scaffolding
        boolean onClimb = World.isClimbable(x, y, z);
        if (onClimb && World.passable(x, y + 1, z) && World.passable(x, y + 2, z))
            out.add(new Candidate(x, y + 1, z, 5, Step.Move.CLIMB, null, null));
        if (World.isClimbable(x, y - 1, z))
            out.add(new Candidate(x, y - 1, z, 3, Step.Move.CLIMB, null, null));

        if (cfg.allowPlace() && cfg.throwaways() > 0 && !inWater && World.standable(x, y - 1, z)) {
            List<BlockPos> pb = new ArrayList<>(1);
            double c = clear(x, y + 2, z, pb);
            if (c >= 0) out.add(new Candidate(x, y + 1, z, 26 + c, Step.Move.PILLAR, pb, new BlockPos(x, y, z)));
        }

        if (cfg.allowBreak() && !inWater) {
            double c = World.breakCost(x, y - 1, z);
            if (c > 0 && World.standable(x, y - 2, z))
                out.add(new Candidate(x, y - 1, z, c + 3, Step.Move.DIG_DOWN, List.of(new BlockPos(x, y - 1, z)), null));
        }
        return out;
    }
}
