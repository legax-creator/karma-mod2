package com.karmamod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod(KarmaMod.MODID)
public class KarmaMod {
    public static final String MODID = "karmamod";

    public KarmaMod() {
        MinecraftForge.EVENT_BUS.register(this);
        NetworkHandler.register();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        player.setMaxUpStep(1.0F);
        forceLoadAdvancements(player);

        // Envanter Kısıtlaması (Sadece 2 Slot)
        for (int i = 2; i < player.getInventory().getContainerSize(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                player.drop(player.getInventory().getItem(i), true, false);
                player.getInventory().setItem(i, ItemStack.EMPTY); // ItemStack import edilmeli
            }
        }

        String mob = player.getType().getDescriptionId().toLowerCase();
        PlayerFormManager.applyDimensions(player, mob);
    }

    private void forceLoadAdvancements(ServerPlayer player) {
        try {
            player.getAdvancements().getOrStartProgress(
                Objects.requireNonNull(player.server.getAdvancements().getAdvancement(new ResourceLocation(MODID, "root")))
            );
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KarmaSystem.assignNewMob(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KarmaSystem.assignNewMob(player);
        }
    }
}
