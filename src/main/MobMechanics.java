package com.karmamod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

public class MobMechanics {
    private static final Random random = new Random();
    private static int endermanWaterTimer = 0;
    private static long lastEndermanTp = 0;
    private static long lastSpiderWeb = 0;
    private static long lastPufferfishInflate = 0;
    private static long lastWardenSonic = 0;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        String mob = KarmaSystem.currentMobForm.toLowerCase();
        ItemStack hand = player.getMainHandItem();

        if (FeedingManager.isCorrectFoodForMob(mob, hand)) {
            player.getFoodData().eat(2, 0.3F);
            hand.shrink(1);
        }

        if (mob.contains("frog") && player.level().getBlockState(player.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) {
            player.heal(20.0F);
            player.getFoodData().setFoodLevel(20);
        }

        if (mob.contains("pufferfish") && System.currentTimeMillis() - lastPufferfishInflate > 10000) {
            lastPufferfishInflate = System.currentTimeMillis();
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }

        if (mob.contains("bee") && KarmaSystem.beePoisonTimer > 0) {
            KarmaSystem.beePoisonTimer--;
            if (KarmaSystem.beePoisonTimer == 24000) {
                player.sendSystemMessage(Component.literal("§c[!] Başarım: Kararlılığına Can Feda & Neden bunu yaptım!"));
            }
            if (KarmaSystem.beePoisonTimer % 1200 == 0 && player.getMaxHealth() > 2.0F) {
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(Math.max(2.0, player.getMaxHealth() - 2.0));
            }
            if (KarmaSystem.beePoisonTimer == 0) player.kill();
        }

        if (mob.contains("enderman")) {
            if (player.isInWater()) {
                endermanWaterTimer++;
                if (endermanWaterTimer % 20 == 0) player.hurt(player.damageSources().drown(), 2.0F);
                player.teleportTo(player.getX() + (random.nextDouble() - 0.5) * 10, player.getY(), player.getZ() + (random.nextDouble() - 0.5) * 10);
            } else if (System.currentTimeMillis() - lastEndermanTp > 5000) {
                lastEndermanTp = System.currentTimeMillis();
                Vec3 look = player.getLookAngle().scale(20);
                player.teleportTo(player.getX() + look.x, player.getY(), player.getZ() + look.z);
            }
        }

        if (mob.contains("iron_golem") && hand.is(Items.IRON_INGOT)) {
            player.heal(6.0F);
            hand.shrink(1);
        }

        if (mob.contains("spider") && System.currentTimeMillis() - lastSpiderWeb > 10000) {
            lastSpiderWeb = System.currentTimeMillis();
            player.level().setBlock(player.blockPosition(), net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState(), 3);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (KarmaSystem.currentMobForm.toLowerCase().contains("iron_golem") && event.getSource().getEntity() instanceof ServerPlayer && event.getSource().getEntity().distanceTo(event.getEntity()) > 2.0) {
            event.setCanceled(true);
        }
        if (KarmaSystem.currentMobForm.toLowerCase().contains("zombified_piglin") && event.getSource().getEntity() instanceof LivingEntity) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        String mob = KarmaSystem.currentMobForm.toLowerCase();
        if (mob.contains("horse") || mob.contains("donkey") || mob.contains("strider") || mob.contains("camel")) {
            event.getEntity().startRiding(event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        String mob = KarmaSystem.currentMobForm.toLowerCase();
        if (mob.contains("skeleton")) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.BOW));
        }
        if (mob.contains("wither_skeleton")) {
            event.getDrops().removeIf(drop -> drop.getItem().is(Items.STONE_SWORD) || drop.getItem().is(Items.GOLDEN_SWORD));
        }
    }
}

