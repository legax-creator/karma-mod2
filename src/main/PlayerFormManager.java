

import net.minecraft.server.level.ServerPlayer;

public class PlayerFormManager {
    public static void applyDimensions(ServerPlayer player, String mob) {
        player.setMaxUpStep(1.0F);
        
        // Zıplama kısıtlaması (Manuel zıplamayı kes)
        if (player.zza > 0 || player.xxa > 0) {
            player.setJumping(false);
        }

        if (mob.contains("spider") || mob.contains("hoglin")) {
            player.setBoundingBox(player.getBoundingBox().inflate(0.5, 0.0, 0.5));
        } else if (mob.contains("bat") || mob.contains("silverfish")) {
            player.setBoundingBox(player.getBoundingBox().deflate(0.4, 0.5, 0.4));
        } else if (mob.contains("warden") || mob.contains("iron_golem") || mob.contains("ghast") || mob.contains("strider") || mob.contains("camel")) {
            player.setMaxUpStep(2.0F);
        }
    }
}
