package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Actions;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.NavMemory;
import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.World;
import com.natan.vortex.util.Chat;
import com.natan.vortex.util.Inv;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Autonomous elytra travel to a far target: equips the elytra, takes off from the ground with a
 * firework, cruises at altitude boosting when slow, climbs over terrain ahead, then descends and
 * flares to land near the target. No OP. Needs an elytra worn/in hotbar and firework rockets.
 */
public class FlyProcess implements Process {
    private enum Phase { EQUIP, TAKEOFF, CRUISE, DESCEND, LAND, DONE }

    private final int tx, ty, tz;
    private final String label;
    private Phase phase = Phase.EQUIP;
    private int boostCd, takeoffTicks, stuck;
    private double lastDist = Double.MAX_VALUE;

    public FlyProcess(BlockPos target, String label) {
        this.tx = target.getX(); this.ty = target.getY(); this.tz = target.getZ(); this.label = label;
    }

    @Override public String name() { return "Fly"; }
    @Override public String status() {
        Minecraft mc = Minecraft.getInstance();
        int d = mc.player == null ? 0 : (int) Math.round(Math.hypot(tx - mc.player.getX(), tz - mc.player.getZ()));
        return label + " · " + phase.name().toLowerCase() + " · " + d + "m";
    }
    @Override public Goal goal() { return null; } // we fly directly, no block pathing
    @Override public boolean isDone() { return phase == Phase.DONE; }
    @Override public void atGoal() {}

    private boolean hasElytraWorn(Minecraft mc) {
        var chest = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        return chest.is(Items.ELYTRA);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) { phase = Phase.DONE; return; }
        var o = mc.options;
        double dx = tx + 0.5 - p.getX(), dz = tz + 0.5 - p.getZ();
        double horiz = Math.hypot(dx, dz);

        switch (phase) {
            case EQUIP -> {
                if (hasElytraWorn(mc)) { phase = Phase.TAKEOFF; takeoffTicks = 0; return; }
                int slot = Inv.findHotbar(s -> s.is(Items.ELYTRA));
                if (slot == -1) { Chat.error("No elytra worn or in the hotbar."); phase = Phase.DONE; return; }
                var chest = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                if (!chest.isEmpty()) { Chat.error("Take off your chestplate first (can't swap it automatically safely)."); phase = Phase.DONE; return; }
                Inv.select(slot);
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND); // equips the elytra to the chest slot
            }
            case TAKEOFF -> {
                if (p.isFallFlying()) { boostCd = 0; phase = Phase.CRUISE; return; } // first rocket fires in CRUISE
                takeoffTicks++;
                if (takeoffTicks > 60) { o.keyJump.setDown(false); Chat.error("Couldn't take off. Try from higher ground."); phase = Phase.DONE; return; }
                if (p.onGround()) { o.keyJump.setDown(true); return; }
                o.keyJump.setDown(false);
                if (Inv.findHotbar(s -> s.is(Items.FIREWORK_ROCKET)) == -1) { Chat.error("No firework rockets for takeoff."); phase = Phase.DONE; return; }
                // airborne and falling: ask to start gliding (every 5 ticks, no rockets wasted until we glide)
                if (p.getDeltaMovement().y < 0 && takeoffTicks % 5 == 0)
                    mc.getConnection().send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
            case CRUISE -> {
                if (!p.isFallFlying()) { // landed or stalled: retry takeoff if far, else finish
                    if (horiz < 6) { phase = Phase.LAND; return; }
                    phase = Phase.TAKEOFF; takeoffTicks = 0; return;
                }
                var chest = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                if (chest.is(Items.ELYTRA) && chest.getMaxDamage() - chest.getDamageValue() < 10) {
                    Chat.error("Elytra almost broken, landing."); phase = Phase.DESCEND; return;
                }
                int cruiseY = Math.max(ty, groundAhead(mc, dx, dz) + 32);
                aimTravel(p, dx, dz, cruiseY);
                Vec3 v = p.getDeltaMovement();
                double speed = Math.hypot(v.x, v.z);
                if (boostCd > 0) boostCd--;
                if (speed < 0.9 && boostCd == 0) { if (boostIfPossible(mc)) boostCd = 20; }
                if (horiz < Math.max(40, p.getY() - ty)) phase = Phase.DESCEND;
                if (horiz < lastDist - 0.01) { lastDist = horiz; stuck = 0; } else if (++stuck > 200) { Chat.error("Not making progress in flight."); phase = Phase.DESCEND; }
            }
            case DESCEND -> {
                if (!p.isFallFlying()) { phase = Phase.LAND; return; }
                double dy = ty - p.getY();
                float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
                p.setYRot(yaw);
                p.setXRot(dy < -3 ? 35 : 10); // nose down to lose height
                if (horiz < 8 && p.getY() - ty < 8) phase = Phase.LAND;
            }
            case LAND -> {
                if (p.getY() - groundBelow(mc) < 3 && p.isFallFlying()) p.setXRot(-40); // flare to slow
                if (p.onGround()) { Actions.releaseKeys(); phase = Phase.DONE; Chat.info("Landed near " + label + "."); }
                else if (!p.isFallFlying() && p.getY() - groundBelow(mc) < 1) { phase = Phase.DONE; }
            }
            default -> {}
        }
    }

    private void aimTravel(net.minecraft.client.player.LocalPlayer p, double dx, double dz, int cruiseY) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        p.setYRot(yaw);
        double dy = cruiseY - p.getY();
        float pitch = (float) Mth.clamp(-dy * 2, -35, 25); // climb if below cruise height
        p.setXRot(pitch);
    }

    private boolean boostIfPossible(Minecraft mc) {
        int fw = Inv.findHotbar(s -> s.is(Items.FIREWORK_ROCKET));
        if (fw == -1) return false;
        int prev = Inv.selected();
        Inv.select(fw);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        Inv.select(prev); // don't leave the player holding rockets after landing
        return true;
    }

    private int groundBelow(Minecraft mc) {
        var p = mc.player;
        for (int y = p.getBlockY(); y > mc.level.getMinY(); y--)
            if (World.standable(p.getBlockX(), y - 1, p.getBlockZ())) return y;
        return mc.level.getMinY();
    }

    /** Highest terrain between us and the target (loaded or remembered), for climb clearance. */
    private int groundAhead(Minecraft mc, double dx, double dz) {
        var p = mc.player;
        double len = Math.hypot(dx, dz);
        if (len < 1) return p.getBlockY();
        int top = ty;
        for (int step = 8; step <= Math.min(len, 96); step += 8) {
            int sx = (int) (p.getX() + dx / len * step), sz = (int) (p.getZ() + dz / len * step);
            int surf = NavMemory.INSTANCE.surfaceY(sx, sz);
            if (surf == Integer.MIN_VALUE && mc.level.getChunkSource().hasChunk(sx >> 4, sz >> 4))
                surf = mc.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sx, sz);
            if (surf != Integer.MIN_VALUE) top = Math.max(top, surf);
        }
        return top;
    }
}
