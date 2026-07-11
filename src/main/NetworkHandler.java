package com.karmamod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

public class NetworkHandler {
    public static final String CHANNEL_NAME = "karmachannel";

    public static void register() {
        // Paket kanalı yapılandırması
    }

    public static void syncFormToClient(ServerPlayer player, String formName) {
        // Form verisini istemciye iletme paketi tetiklenir
    }
}

