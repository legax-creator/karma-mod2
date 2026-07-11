    package com.karma.karmamod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod(KarmaMod.MODID)
public class KarmaMod {
    public static final String MODID = "karmamod";

    private static int karmaBar = 50;
    private static String currentTask = "Görev bekleniyor...";
    private static int taskTimerTicks = 0, killCount = 0, fatigueCooldown = 0;
    private static int nextRaidTargetTicks = 12000;
    private static int raidActiveTimer = -1;
    private static boolean isRaidActive = false;
    private static String activeScenarioOriginalMob = "";
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

            if (name.contains("elder_guardian")) {
                if (fatigueCooldown <= 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 600, 2));
                    fatigueCooldown = 1200;
                } else {
                    fatigueCooldown--;
                }
            }

            if (name.contains("enderman")) {
                var hit = player.pick(5.0, 1.0F, false);
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                    player.sendSystemMessage(Component.literal("§5[!] Enderman'in gözlerine bakıyorsun!"));
                }
            }

            // Yüksek Karma Pasif Kaçışı (Mob Navigasyonu Mob sınıfı üzerinden güvenli çağrılır)
            if (karmaBar >= 70) {
                var nearbyAnimals = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8.0),
                    e -> e instanceof Animal || e instanceof Villager);
                for (Mob animal : nearbyAnimals) {
                    double dx = animal.getX() - player.getX();
                    double dz = animal.getZ() - player.getZ();
                    animal.getNavigation().moveTo(animal.getX() + dx * 2, animal.getY(), animal.getZ() + dz * 2, 1.25D);
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            taskTimerTicks++;
            
            if (taskTimerTicks >= nextRaidTargetTicks && !isRaidActive) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayers().isEmpty() ? null : event.getServer().getPlayerList().getPlayers().get(0);
                if (player != null) {
                    triggerSpecialScenario(player);
                }
            }

            if (isRaidActive) {
                raidActiveTimer--;
                if (raidActiveTimer <= 0) {
                    ServerPlayer player = event.getServer().getPlayerList().getPlayers().isEmpty() ? null : event.getServer().getPlayerList().getPlayers().get(0);
                    if (player != null) {
                        failScenario(player);
                    }
                }
            }
        }
    }

    private static void triggerSpecialScenario(ServerPlayer player) {
        isRaidActive = true;
        raidActiveTimer = 3600;
        activeScenarioOriginalMob = player.getType().getDescriptionId();
        
        int scenario = random.nextInt(4);
        // Doğru sunucu seviyesi (ServerLevel) erişimi player.serverLevel() ile sağlanır
        ServerLevel level = player.serverLevel();
        
        if (scenario == 0) {
            currentTask = "SÜRPRESTE: Zombi formunda 3 dk hayatta kal (Arkanda 5 Golem var!)";
            player.teleportTo(level, 0, 100, 0, Set.of(), 0.0F, 0.0F);
            for (int i = 0; i < 5; i++) {
                IronGolem golem = new IronGolem(EntityType.IRON_GOLEM, level);
                golem.setPos(player.getX() + (random.nextDouble() - 0.5) * 4, player.getY(), player.getZ() + (random.nextDouble() - 0.5) * 4);
                level.addFreshEntity(golem);
            }
            player.sendSystemMessage(Component.literal("§4[!!!] Zombi yapıldın ve 5 Demir Golemle çöl ortasına bırakıldın!"));
        } else if (scenario == 1) {
            currentTask = "SÜRPRESTE: Somon formunda Ancient City'de 3 dk hayatta kal!";
            player.teleportTo(level, player.getX(), -50, player.getZ(), Set.of(), 0.0F, 0.0F);
            player.sendSystemMessage(Component.literal("§9[!!!] Somon yapılıp Ancient City'e ışınlandın!"));
        } else if (scenario == 2) {
            currentTask = "SÜRPRESTE: Tavuk formunda 20 tilkiye karşı 3 dk hayatta kal!";
            for (int i = 0; i < 20; i++) {
                Fox fox = new Fox(EntityType.FOX, level);
                fox.setPos(player.getX() + (random.nextDouble() - 0.5) * 6, player.getY(), player.getZ() + (random.nextDouble() - 0.5) * 6);
                level.addFreshEntity(fox);
            }
            player.sendSystemMessage(Component.literal("§e[!!!] Tavuk yapıldın, etrafın 20 tilkiyle sarıldı!"));
        } else {
            currentTask = "SÜRPRESTE: Koyun formunda 10 aç kurda karşı 3 dk hayatta kal!";
            for (int i = 0; i < 10; i++) {
                Wolf wolf = new Wolf(EntityType.WOLF, level);
                wolf.setPos(player.getX() + (random.nextDouble() - 0.5) * 5, player.getY(), player.getZ() + (random.nextDouble() - 0.5) * 5);
                wolf.setAggressive(true);
                level.addFreshEntity(wolf);
            }
            player.sendSystemMessage(Component.literal("§c[!!!] Koyun yapıldın, 10 aç kurt üstüne koşuyor!"));
        }
    }

    private static void failScenario(ServerPlayer player) {
        isRaidActive = false;
        karmaBar = Math.max(0, karmaBar - 20);
        currentTask = "Görev Başarısız! Karma -%20";
        player.sendSystemMessage(Component.literal("§c[!] Süre doldu/başaramadın! Karma: %" + karmaBar));
        scheduleNextRaid();
    }

    private static void scheduleNextRaid() {
        int randomMinutes = 1 + random.nextInt(20);
        nextRaidTargetTicks = taskTimerTicks + (randomMinutes * 1200);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String name = player.getType().getDescriptionId().toLowerCase();
            if (name.contains("iron_golem") && event.getSource().getEntity() instanceof LivingEntity attacker) {
                player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, attacker.position());
                player.sendSystemMessage(Component.literal("§4[!] HEDEF KİLİTLENDİ!"));
            }
        }
        
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            String name = player.getType().getDescriptionId().toLowerCase();
            if (name.contains("iron_golem") && player.distanceToSqr(event.getEntity()) > 4.0) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onMobTargetCheck(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof ServerPlayer player) {
            String playerName = player.getType().getDescriptionId().toLowerCase();
            String attackerName = event.getEntity().getType().getDescriptionId().toLowerCase();
            
            if (playerName.contains("zombie") && attackerName.contains("zombie")) {
                event.setCanceled(true);
            }
            if (playerName.contains("skeleton") && attackerName.contains("skeleton")) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            karmaBar = Math.max(0, karmaBar - 10);
            player.sendSystemMessage(Component.literal("§c[!] Ölüm cezası! Karma: %" + karmaBar));
            if (isRaidActive) failScenario(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var level = player.level();
            var biome = level.getBiome(player.blockPosition());
            String biomeName = biome.unwrapKey().map(key -> key.location().toString()).orElse("");
            String dimension = level.dimension().location().toString();
            
            String spawnedMob = "Yeryüzü Sakini";
            if (dimension.contains("nether") && random.nextBoolean()) {
                spawnedMob = "Enderman (Nether Dengesi)";
            } else if (!dimension.contains("nether") && random.nextBoolean()) {
                spawnedMob = "Enderman (Dünya Dengesi)";
            } else if (biomeName.contains("deep_dark")) {
                spawnedMob = "Warden";
            } else if (dimension.contains("nether")) {
                if (random.nextBoolean()) spawnedMob = "Piglin";
                else if (random.nextBoolean()) spawnedMob = "Ghast";
                else spawnedMob = "Wither İskeleti";
            } else if (biomeName.contains("ocean") || biomeName.contains("sea") || biomeName.contains("river")) {
                if (random.nextBoolean()) spawnedMob = "Yaşlı Gardiyan";
                else spawnedMob = "Balık";
            } else if (biomeName.contains("swamp")) {
                spawnedMob = "Kurbağa";
            } else if (biomeName.contains("flower") || biomeName.contains("meadow")) {
                spawnedMob = "Arı";
            } else {
                spawnedMob = "Koyun / İnek";
            }

            player.sendSystemMessage(Component.literal("§e[!] Doğum Yeri Analizi: §a" + spawnedMob + " §eformunda başlatıldın!"));
            if (random.nextInt(100) < 15) {
                player.sendSystemMessage(Component.literal("§d[!] Şans eseri bebek olarak doğdun!"));
            }
            scheduleNextRaid();
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRenderGui(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            GuiGraphics g = event.getGuiGraphics();
            g.drawCenteredString(mc.font, "GÖREV: " + currentTask, mc.getWindow().getGuiScaledWidth() / 2, 10, 0xFFFFAA00);
            g.drawCenteredString(mc.font, "KARMA: %" + karmaBar, mc.getWindow().getGuiScaledWidth() / 2, 22, 0xFFFFFF00);
            if (isRaidActive) {
                g.drawCenteredString(mc.font, "KALAN SÜRE: " + (raidActiveTimer / 20) + "s", mc.getWindow().getGuiScaledWidth() / 2, 34, 0xFFFF0000);
            }
        }
    }
                                     }
                                    
