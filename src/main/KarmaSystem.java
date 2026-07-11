package com.karmamod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public class KarmaSystem {
    private static final Random random = new Random();
    public static int karmaBar = 50;
    public static String currentMobForm = "human";

    public static void assignNewMob(ServerPlayer player) {
        double roll = random.nextDouble() * 100;
        String chosen = (roll < karmaBar) ? getMobFromExactTier(karmaBar) : getLowerTierEqualShareMob(karmaBar);
        currentMobForm = chosen;

        player.sendSystemMessage(Component.literal("§e[!] Yeni Formunuz: §a" + chosen + " (Karma: %" + karmaBar + ")"));
        if (random.nextInt(100) < 15) {
            player.sendSystemMessage(Component.literal("§d[!] Şans eseri bebek formunda doğdunuz!"));
        }

        // Sunucudan istemciye form değişikliğini ve boyutları senkronize et
        NetworkHandler.syncFormToClient(player, currentMobForm);
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
        List<String> pools = new ArrayList<>();
        if (currentKarma >= 10) pools.add("Kirpi Balığı");
        if (currentKarma >= 20) pools.add("Hoglin");
        if (currentKarma >= 30) pools.add("Zombi");
        if (currentKarma >= 50) pools.add("Koyun");
        if (currentKarma >= 80) pools.add("Enderman");
        if (pools.isEmpty()) return "Kurbağa";
        return pools.get(random.nextInt(pools.size()));
    }
}
