package com.natan.vortex.module;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.client.ClickGui;
import com.natan.vortex.module.client.Hud;
import com.natan.vortex.module.combat.*;
import com.natan.vortex.module.movement.*;
import com.natan.vortex.module.pathing.*;
import com.natan.vortex.module.player.*;
import com.natan.vortex.module.render.*;
import com.natan.vortex.module.world.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    /** O(1) lookup; filled once in the constructor, then only read (safe from render threads). */
    private final java.util.Map<Class<?>, Module> byClass = new java.util.IdentityHashMap<>();
    private final ClickGui clickGui;

    public ModuleManager() {
        // Combat
        add(new KillAura()); add(new TriggerBot()); add(new Criticals()); add(new Velocity());
        add(new AutoTotem()); add(new AutoArmor()); add(new AutoWeapon()); add(new AimAssist()); add(new BowAimbot());
        add(new AutoClicker()); add(new Reach()); add(new Hitboxes()); add(new Surround()); add(new AutoTrap());
        add(new AutoPearl()); add(new AutoGap()); add(new AntiBot()); add(new CrystalAura()); add(new Offhand());
        add(new com.natan.vortex.module.combat.AutoWeb()); add(new com.natan.vortex.module.combat.AutoCity()); add(new com.natan.vortex.module.combat.AnchorAura());
        add(new com.natan.vortex.module.combat.BedAura()); add(new com.natan.vortex.module.combat.SuperKnockback()); add(new com.natan.vortex.module.combat.MaceSmash());
        add(new com.natan.vortex.module.combat.SpearKill()); add(new com.natan.vortex.module.combat.TotemPops()); add(new com.natan.vortex.module.combat.ArrowDodge());
        add(new com.natan.vortex.module.movement.EntityControl()); add(new com.natan.vortex.module.movement.Glide()); add(new com.natan.vortex.module.movement.Anchor()); add(new com.natan.vortex.module.movement.NoPush()); add(new com.natan.vortex.module.movement.AntiLevitation());
        add(new com.natan.vortex.module.render.Freecam()); add(new com.natan.vortex.module.render.FreeLook()); add(new com.natan.vortex.module.render.Tracers());
        add(new com.natan.vortex.module.render.NoRender()); add(new com.natan.vortex.module.render.ServerLag());
        add(new com.natan.vortex.module.player.FakePlayer()); add(new com.natan.vortex.module.player.XCarry()); add(new com.natan.vortex.module.player.AutoMount()); add(new com.natan.vortex.module.player.AutoNametag());
        add(new com.natan.vortex.module.world.EndermanLook());
        add(new com.natan.vortex.module.client.PaperSafe()); add(new com.natan.vortex.module.client.Macros());
        add(new com.natan.vortex.module.client.Performance()); add(new com.natan.vortex.module.client.AntiCrash());
        add(new AutoLog()); add(new ShieldBreaker()); add(new AutoShield()); add(new BowSpam()); add(new SelfTrap());
        // Movement
        add(new Flight()); add(new Speed()); add(new Sprint()); add(new NoFall()); add(new Step());
        add(new Spider()); add(new Jesus()); add(new AirJump()); add(new HighJump()); add(new SafeWalk());
        add(new Parkour()); add(new AutoWalk()); add(new ElytraFly()); add(new BoatFly()); add(new LongJump()); add(new Blink());
        add(new NoSlow()); add(new FastClimb()); add(new AntiVoid()); add(new AutoJump()); add(new Strafe()); add(new ReverseStep());
        add(new Sneak()); add(new IceSpeed()); add(new AquaSpeed()); add(new EntitySpeed()); add(new ClickTP()); add(new TridentBoost());
        add(new GuiMove()); add(new NoJumpDelay()); add(new Jetpack());
        // Render
        add(new Fullbright()); add(new Xray()); add(new Esp()); add(new NoWeather()); add(new Radar()); add(new TargetHud());
        add(new DangerAlert()); add(new NoOverlay()); add(new Zoom()); add(new PotionHud()); add(new InventoryHud());
        add(new Keystrokes()); add(new PingHud()); add(new ClockHud()); add(new BlockInfo()); add(new NearbyPlayers());
        add(new Compass()); add(new ServerInfo()); add(new TotemCounter()); add(new LogoutSpots()); add(new BlockRadar());
        // Player
        add(new AutoEat()); add(new AutoTool()); add(new ChestStealer()); add(new AutoRespawn());
        add(new AntiAfk()); add(new AutoFish()); add(new FastPlace()); add(new FastBreak()); add(new AutoReplenish());
        add(new InventoryCleaner()); add(new AutoMend()); add(new MiddleClickPearl()); add(new AntiHunger());
        add(new AutoReconnect()); add(new AutoSmelt()); add(new AutoTrade()); add(new Spammer()); add(new ChestSwap()); add(new AntiDrown()); add(new Mlg()); add(new Notifier());
        add(new ItemMagnet()); add(new SpeedMine()); add(new Derp());
        // World
        add(new Nuker()); add(new Scaffold()); add(new AutoFarm());
        add(new AutoBreed()); add(new AutoShear()); add(new VeinMiner()); add(new TreeAura()); add(new LiquidFiller()); add(new AutoTorch());
        add(new StashFinder()); add(new AirPlace()); add(new HoleFiller()); add(new com.natan.vortex.module.world.Timer());
        // Pathing (our own navigator)
        add(new NavSettings()); add(new AutoMine()); add(new FarmBot()); add(new Follow()); add(new Explore()); add(new Tunnel()); add(new Hunt()); add(new Collect());
        // Client
        add(clickGui = new ClickGui()); add(new Hud()); add(new com.natan.vortex.module.client.Panic()); add(new com.natan.vortex.module.client.FriendsModule()); add(new com.natan.vortex.module.client.ChatSuffix());
    }

    private void add(Module m) { modules.add(m); byClass.put(m.getClass(), m); }

    public List<Module> all() { return Collections.unmodifiableList(modules); }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        Module m = byClass.get(type);
        if (m != null) return (T) m;
        throw new IllegalArgumentException("No module " + type.getSimpleName());
    }

    public boolean isActive(Class<? extends Module> type) { return get(type).isEnabled(); }

    public ClickGui clickGui() { return clickGui; }

    /** The navigator stopped (finished, failed or replaced) a process: switch off the module that started it. */
    public void pathingStopped(com.natan.vortex.nav.Process p) {
        for (Module m : modules) if (m instanceof ProcessModule pm) pm.processEnded(p);
    }

    public void onTick() {
        if (VortexClient.mc.player == null || VortexClient.mc.level == null) return;
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try {
                m.onTick();
            } catch (Throwable t) {
                VortexClient.LOG.error("Module {} crashed during tick, disabling", m.name, t);
                m.setEnabled(false);
            }
        }
    }
}
