package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import net.minecraft.client.CameraType;

/** Look around with the mouse without turning your player (great while flying or pathing). */
public class FreeLook extends Module {
    private final BoolSetting thirdPerson = add(new BoolSetting("Third Person", "Switch to third person while active", true));
    private float yaw, pitch;
    private CameraType before;

    public FreeLook() { super("FreeLook", "Move the camera without turning your player", Category.RENDER); }

    @Override
    public void onEnable() {
        if (!inGame()) { setEnabledSilently(false); return; }
        yaw = mc.player.getYRot(); pitch = mc.player.getXRot();
        before = mc.options.getCameraType();
        if (thirdPerson.on()) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    @Override public void onDisable() { if (before != null) mc.options.setCameraType(before); }

    public boolean turn(double dx, double dy) {
        if (!isEnabled()) return false;
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90, Math.min(90, pitch + (float) dy * 0.15f));
        return true;
    }

    public float yaw(float original) { return isEnabled() ? yaw : original; }
    public float pitch(float original) { return isEnabled() ? pitch : original; }
}
