package com.karmamod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class KarmaSystem {
    private static final Random random = new Random();
    public static int karmaBar = 50;
    public static String currentMobForm = "human";
    public static int beePoisonTimer = -1;
    public static int sheepFreezeTimer = -1;

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String mob = currentMobForm.toLowerCase();
            if (!(mob.contains("slime") || mob.contains("magma_cube"))) {
                karmaBar = Math.max(0, karmaBar - 20);
                assignNewMob(player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            assignNewMob(player);
        }
    }

    public static void assignNewMob(ServerPlayer player) {
        double roll = random.nextDouble() * 100;
        String chosen = (roll < karmaBar) ? getMobFromExactTier(karmaBar) : getLowerTierEqualShareMob(karmaBar);
        currentMobForm = chosen;
        player.sendSystemMessage(Component.literal("§e[!] Yeni Formunuz: §a" + chosen + " (Karma: %" + karmaBar + ")"));
        if (random.nextInt(100) < 15) {
            player.sendSystemMessage(Component.literal("§d[!] Şans eseri bebek formunda doğdunuz!"));
        }

        if (chosen.contains("bee")) beePoisonTimer = 24000;
        if (chosen.contains("sheep")) sheepFreezeTimer = 3600;
        NetworkHandler.syncFormToClient(player, chosen);
    }

    private static String getMobFromExactTier(int karma) {
        if (karma <= 0) return "Salmon / Morina / Köylü / Lavgezer / Kurbağa";
        if (karma <= 10) return "Kirpi Balığı / Silverfish / At / Eşek / Papağan / Sniffer / Tüccar / Yarasa / Arı";
        if (karma <= 20) return "Hoglin / Zombi Piglin / Piglin / Lama";
        if (karma <= 30) return "Zombi / İskelet / Wither İskeleti";
        if (karma <= 40) return "Fantom / Deve / Aksolotl / Magma Küpü / Slime";
        if (karma <= 50) return "Tavuk / İnek / Koyun / Mööntar / Kar Golemi / Domuz / Tavşan";
        if (karma <= 60) return "Ghast";
        if (karma <= 70) return "Örümcek / Mağara Örümceği";
        if (karma <= 80) return "Enderman";
        if (karma <= 90) return "Demir Golem";
        return "Warden / Wither / Yaşlı Gardiyan";
    }

    private static String getLowerTierEqualShareMob(int currentKarma) {
        List<String> availablePools = new ArrayList<>();
        if (currentKarma >= 10) availablePools.add("Kirpi Balığı");
        if (currentKarma >= 20) availablePools.add("Hoglin");
        if (currentKarma >= 30) availablePools.add("Zombi");
        if (currentKarma >= 40) availablePools.add("Fantom");
        if (currentKarma >= 50) availablePools.add("Koyun");
        if (currentKarma >= 60) availablePools.add("Ghast");
        if (currentKarma >= 70) availablePools.add("Örümcek");
        if (currentKarma >= 80) availablePools.add("Enderman");
        if (currentKarma >= 90) availablePools.add("Demir Golem");
        
        if (availablePools.isEmpty()) return "Kurbağa";
        return availablePools.get(random.nextInt(availablePools.size()));
    }
}
