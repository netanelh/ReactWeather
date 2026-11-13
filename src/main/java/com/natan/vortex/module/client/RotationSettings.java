package com.natan.vortex.module.client;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/**
 * Shared rotation manager switch. Off = legacy behavior (a lone look packet per action).
 * On = the silent look of KillAura/CrystalAura/Scaffold/placing is also used for the next
 * movement packet, so the server doesn't snap your head back before the action lands.
 */
public class RotationSettings extends Module {
    public RotationSettings() {
        super("Rotations", "Keeps silent rotations (aura/scaffold) in sync with your movement packets", Category.CLIENT);
        showInHud = false;
    }
}
