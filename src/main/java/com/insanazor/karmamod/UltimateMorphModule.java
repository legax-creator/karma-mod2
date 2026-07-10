package com.karma.karmamod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod(KarmaMod.MODID)
public class KarmaMod {
    public static final String MODID = "karmamod";

    private static int karmaBar = 50;
    private static String currentTask = "Gorev bekleniyor...";
    private static int taskTimerTicks = 0, killCount = 0, fatigueCooldown = 0;
    private static boolean isTaskCompleted = false;
    private static final Random random = new Random();

    public KarmaMod() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ClientModEvents());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            player.setMaxUpStep(1.25F);
            
            // 2 Slot Envanter Sınırı
            for (int i = 2; i < player.getInventory().getContainerSize(); i++) {
                if (!player.getInventory().getItem(i).isEmpty()) {
                    player.drop(player.getInventory().getItem(i), true, false);
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }

            String name = player.getType().getDescriptionId().toLowerCase();

            // Yaşlı Gardiyan: 1 dk (1200 tick) cooldown ile Madenci Yorgunluğu
            if (name.contains("elder_guardian")) {
                if (fatigueCooldown <= 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 600, 2));
                    fatigueCooldown = 1200;
                } else {
                    fatigueCooldown--;
                }
            }

            // Enderman Göz Teması Uyarısı (Yavaşlık verilmez)
            if (name.contains("enderman")) {
                var hit = player.pick(5.0, 1.0F, false);
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    player.sendSystemMessage(Component.literal("§5[!] Enderman'in gozlerine bakiyorsun!"));
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            taskTimerTicks++;
            if (taskTimerTicks >= 2400) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayers().isEmpty() ? null : event.getServer().getPlayerList().getPlayers().get(0);
                if (player != null) {
                    if (!isTaskCompleted) karmaBar = Math.max(0, karmaBar - 10);
                    else karmaBar = Math.min(100, karmaBar + 5);
                    isTaskCompleted = false; killCount = 0; taskTimerTicks = 0;
                    assignTask(player);
                }
            }
        }
    }

    private static void assignTask(ServerPlayer player) {
        String name = player.getType().getDescriptionId().toLowerCase();
        if (name.contains("warden") || (name.contains("wither") && !name.contains("skeleton")))
            currentTask = "KORKU SAL: 60 canli yok et! (" + killCount + "/60)";
        else currentTask = "Hayatta kal ve Karma'ni koru!";
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        // Golem Formu Hedefe Kilitlenme (Yavaşlık efekti yok, sadece bakış çevirme)
        if (event.getEntity() instanceof ServerPlayer player) {
            String name = player.getType().getDescriptionId().toLowerCase();
            if (name.contains("iron_golem") && event.getSource().getEntity() instanceof LivingEntity attacker) {
                player.lookAt(attacker.getCommandSenderWorld().dimension(), attacker.position());
                player.sendSystemMessage(Component.literal("§4[!] HEDEF KILITLENDI: " + attacker.getName().getString()));
            }
        }
        
        // Golem 2 Blok Vuruş Mesafesi Kısıtlaması (3 blok ve üstü iptal)
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            String name = player.getType().getDescriptionId().toLowerCase();
            if (name.contains("iron_golem") && player.distanceToSqr(event.getEntity()) > 4.0) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            String name = player.getType().getDescriptionId().toLowerCase();
            if (name.contains("warden") || (name.contains("wither") && !name.contains("skeleton"))) {
                killCount++;
                if (killCount >= 60) isTaskCompleted = true;
            }
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            karmaBar = Math.max(0, karmaBar - 10);
            player.sendSystemMessage(Component.literal("§c[!] Olum cezasi! Karma: %" + karmaBar));
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (random.nextInt(100) < 15) player.sendSystemMessage(Component.literal("§d[!] Sans eseri bebek olarak dogdun!"));
            assignTask(player);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            GuiGraphics g = event.getGuiGraphics();
            g.drawCenteredString(mc.font, "GOREV: " + currentTask, mc.getWindow().getGuiScaledWidth() / 2, 10, 0xFFFFAA00);
            g.drawCenteredString(mc.font, "KARMA: %" + karmaBar, mc.getWindow().getGuiScaledWidth() / 2, 22, 0xFFFFFF00);
        }
    }
}
