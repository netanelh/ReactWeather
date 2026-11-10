package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Hold Left Alt and right-click to teleport onto the block you look at. */
public class ClickTP extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Max teleport distance (vanilla servers allow ~8-10)", 8, 2, 64, 1));
    private boolean wasDown;

    public ClickTP() { super("ClickTP", "Left Alt + right click teleports you to where you look", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.gui.screen() != null) return;
        long win = mc.getWindow().handle();
        boolean down = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS && GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            Vec3 eye = mc.player.getEyePosition();
            Vec3 end = eye.add(mc.player.getLookAngle().scale(range.d()));
            BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 to = Vec3.atBottomCenterOf(hit.getBlockPos().above());
                // split long jumps into hops the server accepts
                Vec3 from = mc.player.position();
                int hops = (int) Math.ceil(from.distanceTo(to) / 8.0);
                for (int i = 1; i <= hops; i++) {
                    Vec3 p = from.lerp(to, (double) i / hops);
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(p.x, p.y, p.z, true, false));
                }
                mc.player.setPos(to.x, to.y, to.z);
            }
        }
        wasDown = down;
    }
}
