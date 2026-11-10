package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;

public final class Targets {
    private Targets() {}

    public static boolean isHostile(Entity e) { return e.getType().getCategory() == MobCategory.MONSTER; }

    public static boolean isPassive(Entity e) {
        MobCategory c = e.getType().getCategory();
        return c != MobCategory.MONSTER && c != MobCategory.MISC;
    }

    private static boolean friendsProtected() {
        var m = com.natan.vortex.VortexClient.modules;
        return m == null || m.isActive(com.natan.vortex.module.client.FriendsModule.class);
    }

    private static boolean antiBot() {
        var m = com.natan.vortex.VortexClient.modules;
        return m != null && m.isActive(com.natan.vortex.module.combat.AntiBot.class);
    }

    /** Common filter used by KillAura / TriggerBot. */
    public static boolean valid(Entity e, boolean players, boolean hostiles, boolean passives) {
        Minecraft mc = Minecraft.getInstance();
        if (!(e instanceof LivingEntity living) || e == mc.player || !e.isAlive() || living.getHealth() <= 0) return false;
        if (e.isSpectator()) return false;
        if (e instanceof Player p) {
            if (!players || p.isCreative() || p.isSpectator()) return false;
            if (Friends.is(p.getName().getString()) && friendsProtected()) return false;
            if (antiBot() && mc.getConnection() != null && mc.getConnection().getPlayerInfo(p.getUUID()) == null) return false; // not in tab list = NPC/bot
            return true;
        }
        if (isHostile(e)) return hostiles;
        if (isPassive(e)) return passives;
        return false;
    }
}
