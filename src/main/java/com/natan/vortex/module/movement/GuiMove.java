package com.natan.vortex.module.movement;

import com.mojang.blaze3d.platform.InputConstants;
import com.natan.vortex.mixin.KeyMappingAccessor;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/** Walk, jump and sprint while inventories and menus are open. */
public class GuiMove extends Module {
    private final BoolSetting sneak = add(new BoolSetting("Sneak", "Allow sneaking too", false));

    public GuiMove() { super("GUIMove", "Keep walking while menus and inventories are open", Category.MOVEMENT); }

    @Override
    public void onTick() {
        // only container screens without a text box: typing in chat, signs, anvils, creative search
        // or the ClickGUI search must never walk the player
        var s = mc.gui.screen();
        if (!inGame() || !(s instanceof AbstractContainerScreen<?>) || s instanceof AnvilScreen
            || s instanceof CreativeModeInventoryScreen) return;
        var o = mc.options;
        pass(o.keyUp); pass(o.keyDown); pass(o.keyLeft); pass(o.keyRight); pass(o.keyJump);
        // toggle keys flip on every setDown(true): leave them alone when toggle sprint/sneak is on
        if (!o.toggleSprint().get()) pass(o.keySprint);
        if (sneak.on() && !o.toggleCrouch().get()) pass(o.keyShift);
    }

    private void pass(KeyMapping k) {
        InputConstants.Key key = ((KeyMappingAccessor) k).vortex$getKey();
        if (key.getType() != InputConstants.Type.KEYSYM) return;
        k.setDown(InputConstants.isKeyDown(mc.getWindow(), key.getValue()));
    }
}
