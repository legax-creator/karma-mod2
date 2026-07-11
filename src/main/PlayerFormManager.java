package com.karmamod;

import net.minecraft.server.level.ServerPlayer;

public class PlayerFormManager {
    public static void applyDimensions(ServerPlayer player, String mob) {
        player.setMaxUpStep(1.0F);
        if (mob.contains("spider") || mob.contains("hoglin")) {
            player.setBoundingBox(player.getBoundingBox().inflate(0.5, 0.0, 0.5));
        } else if (mob.contains("warden") || mob.contains("iron_golem")) {
            player.setMaxUpStep(2.0F);
        }
    }
}

