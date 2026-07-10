package com.insanazor.karmamod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.Frog;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod("karmamod")
public class UltimateMorphModule {

    private static String chosenMob = "NORMAL"; 
    private static int timer = 0;

    public UltimateMorphModule() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void setChosenMob(String mob) {
        chosenMob = mob;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        timer++;
        if (timer % 20 == 0) { 
            applyMobAbilities(player);
        }
    }

    private void applyMobAbilities(Player player) {
        AABB area = player.getBoundingBox().inflate(5.0);

        if (chosenMob.equals("KURBAGA")) {
            List<Frog> frogs = player.level().getEntitiesOfClass(Frog.class, area);
            for (Frog frog : frogs) {
                if (player.level().getBlockState(frog.blockPosition()).is(Blocks.MAGMA_BLOCK)) {
                    player.getInventory().add(new ItemStack(Items.FROGSPAWN));
                    player.sendSystemMessage(Component.literal("Kurbağa magma bloğu yedi, yumurta kazandın!"));
                }
            }
        }

        if (chosenMob.equals("KOYUN")) {
            List<Sheep> sheepList = player.level().getEntitiesOfClass(Sheep.class, area);
            for (Sheep sheep : sheepList) {
                if (sheep.isSheared()) {
                    player.hurt(player.damageSources().freeze(), 1.0f);
                    player.sendSystemMessage(Component.literal("Kırpılmış koyun üşüyor, sen de dondun!"));
                }
            }
        }

        if (chosenMob.equals("KOYLU")) {
            List<Villager> villagers = player.level().getEntitiesOfClass(Villager.class, area);
            for (Villager villager : villagers) {
                List<LightningBolt> lightnings = player.level().getEntitiesOfClass(LightningBolt.class, villager.getBoundingBox().inflate(1.0));
                if (!lightnings.isEmpty()) {
                    Witch witch = EntityType.WITCH.create(player.level());
                    if (witch != null) {
                        witch.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), villager.getXRot());
                        player.level().addFreshEntity(witch);
                        villager.discard();
                        player.sendSystemMessage(Component.literal("Köylüye yıldırım çarptı ve cadıya dönüştü!"));
                    }
                }
            }
        }
        
        float maxHealth = 20.0f;
        if (chosenMob.equals("DEMIR_GOLEM")) {
            maxHealth = 100.0f;
        }

        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue((double)maxHealth);
    }
            }

