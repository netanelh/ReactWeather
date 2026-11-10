package com.natan.vortex.module.client;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** FPS saver on top of Sodium: skips rendering far/unimportant entities and floods of particles. */
public class Performance extends Module {
    private final NumberSetting mobDistance = add(new NumberSetting("Mob Distance", "Don't render mobs farther than this (0 = off)", 64, 0, 256, 8));
    private final NumberSetting itemDistance = add(new NumberSetting("Item Distance", "Don't render dropped items farther than this", 24, 0, 128, 4));
    private final BoolSetting stands = add(new BoolSetting("Hide Far Decor", "Armor stands / item frames / paintings only within 16 blocks", true));
    private final NumberSetting particles = add(new NumberSetting("Particles/Tick", "Max new particles per tick (stops lag from farms and explosions)", 200, 10, 2000, 10));
    private int particlesThisTick;

    public Performance() { super("Performance", "Skips rendering far entities and particle floods for more FPS", Category.CLIENT); }

    @Override public void onTick() { particlesThisTick = 0; }

    public boolean hide(Entity e, double camX, double camY, double camZ) {
        if (!isEnabled() || e instanceof Player) return false;
        double d = e.distanceToSqr(camX, camY, camZ);
        if (e instanceof ItemEntity) return itemDistance.d() > 0 && d > itemDistance.d() * itemDistance.d();
        if (stands.on()) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            if ((id.equals("armor_stand") || id.contains("item_frame") || id.equals("painting")) && d > 256) return true;
        }
        return mobDistance.d() > 0 && d > mobDistance.d() * mobDistance.d();
    }

    public boolean allowParticle() {
        if (!isEnabled()) return true;
        return ++particlesThisTick <= particles.i();
    }
}
