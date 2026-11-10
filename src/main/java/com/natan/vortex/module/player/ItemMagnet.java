package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Navigator;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Walks you over nearby dropped items when you're idle. */
public class ItemMagnet extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Max distance to items", 8, 2, 24, 1));
    private boolean steering;

    public ItemMagnet() { super("ItemMagnet", "Walks to nearby dropped items automatically", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || Navigator.INSTANCE.active() || mc.gui.screen() != null) { stop(); return; }
        if (com.natan.vortex.util.Move.isMoving() && !steering) return; // you're walking yourself
        ItemEntity best = null; double bd = range.d();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ItemEntity it)) continue;
            double d = it.distanceTo(mc.player);
            if (d < bd && Math.abs(it.getY() - mc.player.getY()) < 2.5) { bd = d; best = it; }
        }
        if (best == null) { stop(); return; }
        Vec3 t = best.position();
        double yaw = Math.toDegrees(Math.atan2(t.z - mc.player.getZ(), t.x - mc.player.getX())) - 90;
        mc.player.setYRot((float) yaw);
        mc.options.keyUp.setDown(true);
        if (mc.player.horizontalCollision && mc.player.onGround()) mc.player.jumpFromGround();
        steering = true;
    }

    private void stop() { if (steering) { mc.options.keyUp.setDown(false); steering = false; } }

    @Override public void onDisable() { stop(); }
}
