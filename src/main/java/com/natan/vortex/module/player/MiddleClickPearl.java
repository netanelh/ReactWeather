package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.util.Inv;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class MiddleClickPearl extends Module {
    private boolean wasDown;

    public MiddleClickPearl() { super("MiddleClickPearl", "Middle mouse throws an ender pearl from anywhere in your hotbar", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
        if (down && !wasDown && mc.gui.screen() == null) {
            int slot = Inv.findHotbar(s -> s.is(Items.ENDER_PEARL));
            if (slot != -1) {
                int prev = Inv.selected();
                Inv.select(slot);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                Inv.select(prev);
            }
        }
        wasDown = down;
    }
}
