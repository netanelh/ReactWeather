package com.natan.vortex.nav;

import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Rotations;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Low-level player control shared by the executor and the processes (main thread only). */
public final class Actions {
    private Actions() {}

    private static Minecraft mc() { return Minecraft.getInstance(); }

    public static void releaseKeys() {
        var o = mc().options;
        o.keyUp.setDown(false); o.keyDown.setDown(false); o.keyLeft.setDown(false); o.keyRight.setDown(false);
        o.keyJump.setDown(false); o.keyShift.setDown(false); o.keySprint.setDown(false); o.keyAttack.setDown(false);
    }

    /** Turns the camera toward a point at most maxStep degrees per tick. Returns true when roughly facing it. */
    public static boolean lookAt(Vec3 target, float maxStep) {
        float[] a = Rotations.anglesTo(target);
        Rotations.stepClient(a, maxStep);
        return Math.abs(Mth.wrapDegrees(a[0] - mc().player.getYRot())) < 12 && Math.abs(a[1] - mc().player.getXRot()) < 15;
    }

    /** Turns only the yaw toward a point (keeps pitch level, for walking). */
    public static void faceYaw(Vec3 target, float maxStep) {
        var p = mc().player;
        double dx = target.x - p.getX(), dz = target.z - p.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float diff = Mth.wrapDegrees(yaw - p.getYRot());
        p.setYRot(p.getYRot() + Mth.clamp(diff, -maxStep, maxStep));
        if (Math.abs(p.getXRot()) > 30) p.setXRot(p.getXRot() * 0.7f);
    }

    public static void selectBestTool(BlockState state) {
        var inv = mc().player.getInventory();
        int best = Inv.selected();
        float bestSpeed = inv.getItem(best).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            var st = inv.getItem(i);
            if (st.isDamageableItem() && st.getMaxDamage() - st.getDamageValue() < 3) continue;
            float sp = st.getDestroySpeed(state);
            if (sp > bestSpeed) { bestSpeed = sp; best = i; }
        }
        Inv.select(best);
    }

    /** Mines a block a little each tick. Returns true once the block is gone. */
    public static boolean mine(BlockPos pos) {
        var mc = mc();
        BlockState st = mc.level.getBlockState(pos);
        if (st.isAir() || !st.getFluidState().isEmpty() && st.canBeReplaced()) return true;
        Vec3 center = Vec3.atCenterOf(pos);
        if (center.distanceTo(mc.player.getEyePosition()) > 5.2) return false;
        selectBestTool(st);
        lookAt(center, 60);
        Direction face = faceToward(pos);
        if (mc.gameMode.isDestroying()) mc.gameMode.continueDestroyBlock(pos, face);
        else mc.gameMode.startDestroyBlock(pos, face);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return mc.level.getBlockState(pos).isAir();
    }

    public static Direction faceToward(BlockPos pos) {
        Vec3 d = mc().player.getEyePosition().subtract(Vec3.atCenterOf(pos));
        double ax = Math.abs(d.x), ay = Math.abs(d.y), az = Math.abs(d.z);
        if (ay >= ax && ay >= az) return d.y > 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return d.x > 0 ? Direction.EAST : Direction.WEST;
        return d.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static boolean inventoryFull() {
        var inv = mc().player.getInventory();
        for (int i = 0; i < 36; i++) if (inv.getItem(i).isEmpty()) return false;
        return true;
    }

    public static int countThrowaways() {
        var inv = mc().player.getInventory();
        int c = 0;
        for (int i = 0; i < 36; i++) if (com.natan.vortex.util.Place.isThrowaway(inv.getItem(i))) c += inv.getItem(i).getCount();
        return c;
    }
}
