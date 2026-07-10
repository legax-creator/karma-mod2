
package com.insanazor.karmamod;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "karmamod")
public class UltimateMorphModule {

    private static final Map<UUID, Integer> playerKarma = new HashMap<>();
    private static final Map<UUID, ServerBossEvent> karmaBars = new HashMap<>();
    private static final Map<UUID, String> playerCurrentForm = new HashMap<>();
    private static final Map<UUID, Boolean> isBabyForm = new HashMap<>();
    
    // Süt, Çorba ve Yetenek Sayaçları
    private static final Map<UUID, Integer> milkBar = new HashMap<>();
    private static final Map<UUID, Integer> soupBar = new HashMap<>();
    private static final Map<UUID, Integer> freezeTimer = new HashMap<>();
    private static final Map<UUID, Long> lastAbilityTime = new HashMap<>();
    private static final Map<UUID, Integer> activePotionIndex = new HashMap<>(); // Cadı iksir seçimi için

    private static final Random random = new Random();

    // MOB KATEGORİ LİSTELERİ
    private static final List<String> LEVEL_0 = Arrays.asList("SOMON", "MORINA", "KOYLU", "LAVGEZER", "KURBAGA");
    private static final List<String> LEVEL_10 = Arrays.asList("KIRPI_BALIGI", "SILVERFISH", "AT", "ESEK", "PAPAGAN", "SNIFFER", "TUCCAR", "YARASA", "ARI");
    private static final List<String> LEVEL_20 = Arrays.asList("HOGLIN", "ZOMBI_PIGLIN", "PIGLIN", "LAMA");
    private static final List<String> LEVEL_30 = Arrays.asList("ZOMBI", "ISKELET", "WITHER_ISKELETI");
    private static final List<String> LEVEL_40 = Arrays.asList("FANTOM", "DEVE", "AKSOLOTL", "MAGMA_KUPU", "SLIME");
    private static final List<String> LEVEL_50 = Arrays.asList("TAVUK", "INEK", "KOYUN", "MOONTAR", "KAR_GOLEMI", "DOMUZ", "TAVSAN");
    private static final List<String> LEVEL_60 = Arrays.asList("GHAST");
    private static final List<String> LEVEL_70 = Arrays.asList("ORUMCEK", "MAGARA_ORUMCEGI");
    private static final List<String> LEVEL_80 = Arrays.asList("ENDERMAN");
    private static final List<String> LEVEL_90 = Arrays.asList("DEMIR_GOLEM");
    private static final List<String> LEVEL_100 = Arrays.asList("WARDEN", "WITHER", "YASLI_GARDIYAN");

    // --- EVRENSEL KISITLAMA: ENVANTER, BLOK, ZIPLAMA ---
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!playerCurrentForm.getOrDefault(event.getPlayer().getUUID(), "NORMAL").equals("NORMAL")) {
            event.setCanceled(true); // Canlılar blok alamaz, kıramaz
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player) event.setCanceled(true); // Canlılar blok koyamaz
    }

    // --- KÖYLÜYE YILDIRIM ÇARPINCA CADI OLMA MEKANİĞİ ---
    @SubscribeEvent
    public static void onLightningStrike(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String form = playerCurrentForm.getOrDefault(player.getUUID(), "NORMAL");
            if (form.equals("KOYLU") && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)) {
                playerCurrentForm.put(player.getUUID(), "CADI");
                activePotionIndex.put(player.getUUID(), 0);
                player.sendSystemMessage(Component.literal("§5Karanlık güçler uyandı! Yıldırım çarpmasıyla CADIYA dönüştün!"));
                player.sendSystemMessage(Component.literal("§dEğilerek (Shift) İksir Değiştir, Sol Tık ile İksir Fırlat!"));
                event.setAmount(0); // Yıldırım hasarını sıfırla ki ölmesin
            }
        }
    }

    // --- BESLENME, ÖZEL SAĞ TIK MEKANİKLERİ VE YETENEKLER ---
    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        String form = playerCurrentForm.getOrDefault(uuid, "NORMAL");
        ItemStack item = event.getItemStack();

        if (!form.equals("NORMAL")) {
            // Envanter kontrolü: Yemek slotu simülasyonu
            if (player.getInventory().getContainerSize() > 2) {
                // Mod mekaniği gereği ilk 2 slot harici kullanımı engelleme mantığı
            }

            boolean fed = false;

            // Kurbağa -> Magma Bloğu yiyerek tek atma ve fulleme
            if (form.equals("KURBAGA") && item.is(Items.MAGMA_BLOCK)) {
                player.getFoodData().eat(20, 1.0F);
                fed = true;
            }
            // Köylü & Tüccar -> Ekmek
            else if ((form.equals("KOYLU") || form.equals("TUCCAR")) && item.is(Items.BREAD)) fed = true;
            // Lavgezer -> Kırmızı Sarmaşık
            else if (form.equals("LAVGEZER") && item.is(Items.WEEPING_VINES)) fed = true;
            // At & Eşek & Hoglin & Piglin -> Elma, Havuç, Altın Havuç, Altın Elma
            else if (Arrays.asList("AT", "ESEK", "HOGLIN", "PIGLIN", "ZOMBI_PIGLIN").contains(form)) {
                if (item.is(Items.APPLE) || item.is(Items.CARROT) || item.is(Items.GOLDEN_CARROT) || item.is(Items.GOLDEN_APPLE)) fed = true;
            }
            // Papağan & Tavuk -> Tohum
            else if ((form.equals("PAPAGAN") || form.equals("TAVUK")) && item.is(Items.WHEAT_SEEDS)) fed = true;
            // Sniffer -> Meşale Çiçeği Tohumu
            else if (form.equals("SNIFFER") && item.is(Items.TORCHFLOWER_SEEDS)) fed = true;
            // Örümcek -> İp
            else if ((form.equals("ORUMCEK") || form.equals("MAGARA_ORUMCEGI")) && item.is(Items.STRING)) fed = true;
            // Enderman -> Mavi Orman Bitkileri
            else if (form.equals("ENDERMAN") && (item.is(Items.WARPED_ROOTS) || item.is(Items.NETHER_SPROUTS))) fed = true;

            if (fed) {
                player.getFoodData().eat(4, 0.5F);
                item.shrink(1);
                player.sendSystemMessage(Component.literal("§aFormuna uygun beslendin."));
                event.setCanceled(false);
                return;
            }

            // Muafiyetler hariç yanlış yemek engelleme
            if (!Arrays.asList("WARDEN", "WITHER", "FANTOM", "CADI").contains(form)) {
                event.setCanceled(true);
            }
        }
    }

    // --- SİLVERFİSH TAŞ YEME, İNEK/MÖÖNTAR SAĞILMA VE BLOK ETKİLEŞİMLERİ ---
    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        String form = playerCurrentForm.getOrDefault(uuid, "NORMAL");
        net.minecraft.world.level.block.state.BlockState state = player.level().getBlockState(event.getPos());

        if (form.equals("SILVERFISH") && (state.is(Blocks.STONE) || state.is(Blocks.STONE_BRICKS))) {
            player.level().destroyBlock(event.getPos(), false);
            player.getFoodData().eat(4, 0.5F);
            player.sendSystemMessage(Component.literal("§7Taş kemirdin!"));
        }
    }

    // İnek ve Mööntar sağma kontrolü (Başka oyuncu sağarken tetiklenir)
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Player targetPlayer) {
            UUID targetUuid = targetPlayer.getUUID();
            String form = playerCurrentForm.getOrDefault(targetUuid, "NORMAL");
            ItemStack item = event.getItemStack();

            if (form.equals("INEK") && item.is(Items.BUCKET)) {
                int milk = milkBar.getOrDefault(targetUuid, 100);
                if (milk >= 20) {
                    milkBar.put(targetUuid, milk - 20);
                    event.getEntity().setItemInHand(event.getHand(), new ItemStack(Items.MILK_BUCKET));
                } else {
                    targetPlayer.hurt(targetPlayer.damageSources().generic(), 1.0F); // Süt yoksa yarım kalp hasar
                    targetPlayer.sendSystemMessage(Component.literal("§cSütün yokken sağıldığın için canın acıdı!"));
                }
            }
            else if (form.equals("MOONTAR") && item.is(Items.BOWL)) {
                int soup = soupBar.getOrDefault(targetUuid, 100);
                if (soup >= 25) {
                    soupBar.put(targetUuid, soup - 25);
                    event.getEntity().setItemInHand(event.getHand(), new ItemStack(Items.MUSHROOM_STEW));
                } else {
                    targetPlayer.hurt(targetPlayer.damageSources().generic(), 2.0F); // Çorba yoksa 1 kalp hasar
                    targetPlayer.sendSystemMessage(Component.literal("§cMantarın kalmadığı için hasar yedin!"));
                }
            }
        }
    }

    // --- SOL TIK YETENEKLERİ (GHAST, KAR GOLEMİ, CADI) ---
    @SubscribeEvent
    public static void onPlayerLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        String form = playerCurrentForm.getOrDefault(uuid, "NORMAL");
        long now = System.currentTimeMillis();
        long last = lastAbilityTime.getOrDefault(uuid, 0L);

        if (player.level().isClientSide) return;

        // CADI İKSİR FIRLATMA MEKANİĞİ
        if (form.equals("CADI")) {
            if (player.isCrouching()) {
                // Shift + Sol Tık iksir değiştirir
                int idx = (activePotionIndex.getOrDefault(uuid, 0) + 1) % 3;
                activePotionIndex.put(uuid, idx);
                String pName = idx == 0 ? "Zayıflık" : idx == 1 ? "Zehir" : "Anında Sağlık";
                player.sendSystemMessage(Component.literal("§dSeçilen İksir: " + pName));
                return;
            }

            if (now - last >= 1500) { // Kısa cooldown
                ThrownPotion potion = new ThrownPotion(player.level(), player);
                ItemStack potionItem = new ItemStack(Items.SPLASH_POTION);
                int idx = activePotionIndex.getOrDefault(uuid, 0);
                if (idx == 0) PotionUtils.setPotion(potionItem, Potions.WEAKNESS);
                else if (idx == 1) PotionUtils.setPotion(potionItem, Potions.POISON);
                else PotionUtils.setPotion(potionItem, Potions.HEALING);
                
                potion.setItem(potionItem);
                Vec3 look = player.getLookAngle();
                potion.shoot(look.x, look.y, look.z, 0.75F, 1.0F);
                player.level().addFreshEntity(potion);
                lastAbilityTime.put(uuid, now);
            }
        }
        // GHAST (5 Saniye Cooldown)
        else if (form.equals("GHAST") && (now - last >= 5000)) {
            Vec3 lookVec = player.getLookAngle();
            LargeFireball fireball = new LargeFireball(player.level(), player, lookVec.x, lookVec.y, lookVec.z, 1);
            fireball.setPos(player.getX(), player.getY() + 1.5D, player.getZ());
            player.level().addFreshEntity(fireball);
            lastAbilityTime.put(uuid, now);
        }
        // KAR GOLEMİ (1 Saniye Cooldown)
        else if (form.equals("KAR_GOLEMI") && (now - last >= 1000)) {
            Snowball snowball = new Snowball(player.level(), player);
            Vec3 look = player.getLookAngle();
            snowball.shoot(look.x, look.y, look.z, 1.0F, 1.0F);
            player.level().addFreshEntity(snowball);
            lastAbilityTime.put(uuid, now);
        }
    }

    // --- ANLIK TICK MEKANİKLERİ VE KURAL SIMÜLASYONLARI ---
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            String form = playerCurrentForm.getOrDefault(uuid, "NORMAL");

            if (!form.equals("NORMAL")) {
                // 1. EVRENSEL KURAL: KENDİ ZIPLAYAMAZ, OTOMATİK BLOK GEÇER
                player.setDiscardFriction(false);
                AttributeInstance stepHeight = player.getAttribute(Attributes.MOVEMENT_SPEED); // Temsili/Step height Forge'da forge event ile veya setStepHeight ile yönetilir
                player.setMaxUpStep(1.25F); // Blokları otomatik tırmanır

                // 2. HIZ AYARLAMALARI (Orijinal Mob Hızları Simülasyonu)
                AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speed != null) {
                    if (form.equals("SILVERFISH") || form.equals("ENDERMAN")) speed.setBaseValue(0.3F);
                    else if (form.equals("DEMIR_GOLEM") || form.equals("WARDEN")) speed.setBaseValue(0.15F);
                    else speed.setBaseValue(0.25F); // Standart insan hızı dengesi
                }

                // 3. FANTOM KABUSU (Gece uçar, Gündüz sürünür)
                if (form.equals("FANTOM")) {
                    if (player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        if (speed != null) speed.setBaseValue(0.15F); // Golem hızıyla sürünme
                    } else {
                        player.getAbilities().mayfly = true; // Sadece gece/gölgede uçuş
                    }
                }

                // 4. ENDERMAN SU HASARI VE IŞINLANMA
                if (form.equals("ENDERMAN")) {
                    if (player.isInWater() && player.tickCount % 20 == 0) {
                        player.hurt(player.damageSources().magic(), 2.0F); // Saniyede 1 kalp hasar
                        // Rastgele ışınlanma
                        player.teleportTo(player.getX() + random.nextInt(10) - 5, player.getY(), player.getZ() + random.nextInt(10) - 5);
                    }
                }

                // 5. BOSS CAN YENİLEME MEKANİKLERİ (5 Saniyede Yarım Kalp)
                if ((form.equals("WITHER") || form.equals("WARDEN")) && player.tickCount % 100 == 0) {
                    player.heal(1.0F); 
                }

                // 6. ÖRÜMCEK AĞ BIRAKMA VE TIRMANMA (10 Saniyede Bir)
                if (form.equals("ORUMCEK") || form.equals("MAGARA_ORUMCEGI")) {
                    if (player.horizontalCollision) {
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.2D, player.getDeltaMovement().z); // Duvara tırmanma
                    }
                    if (player.tickCount % 200 == 0) {
                        player.level().setBlockAndUpdate(player.blockPosition(), Blocks.COBWEB.defaultBlockState());
                    }
                }

                // 7. KOYUN DONMA LANETİ
                if (form.equals("KOYUN") && player.tickCount % 20 == 0) {
                    int fTime = freezeTimer.getOrDefault(uuid, 0);
                    // Gece veya gölgede ise süre ilerler
                    if (!player.level().isDay() || !player.level().canSeeSky(player.blockPosition())) {
                        fTime++;
                        if (fTime >= 180) { // 3 dakika dolduysa
                            player.hurt(player.damageSources().freeze(), 1.0F); // Donma hasarı başlar
                            if (fTime >= 480) player.die(player.damageSources().freeze()); // 5 dakika sonra ölüm
                        }
                        freezeTimer.put(uuid, fTime);
                    }
                }
            }
        }
    }

    // --- SALDIRI VE HASAR ALMA ANINDAKİ ÖZEL YETENEKLER ---
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // GOLEM 2 BLOK KISITLAMASI
        if (event.getSource().getEntity() instanceof Player attacker) {
            String form = playerCurrentForm.getOrDefault(attacker.getUUID(), "NORMAL");
            if (form.equals("DEMIR_GOLEM") && attacker.distanceToSqr(event.getEntity()) > 4.0D) {
                event.setCanceled(true); // 2 bloktan uzağa vuramaz
            }
            // HOGLIN FIRLATMA MEKANİĞİ
            if (form.equals("HOGLIN")) {
                event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().add(0, 0.55D, 0)); // 2-3 blok yukarı fırlatır
            }
            // WITHER İSKELETİ SOLGUNLUK
            if (form.equals("WITHER_ISKELETI")) {
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
            }
        }

        // WARDEN ŞOK DALGASI (Her 50 kalp = 100 can azaldığında)
        if (event.getEntity() instanceof Player victim && playerCurrentForm.getOrDefault(victim.getUUID(), "NORMAL").equals("WARDEN")) {
            if (victim.getHealth() <= 300 && victim.tickCount % 300 == 0) { // 15 Saniye Cooldown simülasyonu
                AABB area = victim.getBoundingBox().inflate(15.0D);
                List<LivingEntity> targets = victim.level().getEntitiesOfClass(LivingEntity.class, area);
                for (LivingEntity t : targets) {
                    if (t != victim) t.hurt(victim.damageSources().magic(), 30.0F); // Alan şok dalgası hasarı
                }
                victim.sendSystemMessage(Component.literal("§1Warden Sonik Şok Dalgası Fırlattı!"));
            }
        }
    }

    // --- EVRİM VE RESPAWN MATRİSİ (ŞANS VE BEBEK BAŞLAMA DAHİL) ---
    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();
        int karma = playerKarma.getOrDefault(uuid, 50);
        player.removeAllEffects();

        int tier = (karma / 10) * 10;
        if (tier > 100) tier = 100;

        String chosenMob = "SOMON";
        
        if (tier == 0 || random.nextFloat() < 0.50f) {
            List<String> mobs = getMobsByTier(tier);
            chosenMob = mobs.get(random.nextInt(mobs.size()));
        } else {
            List<String> lowerPool = new ArrayList<>();
            for (int t = 0; t < tier; t += 10) lowerPool.addAll(getMobsByTier(t));
            chosenMob = lowerPool.get(random.nextInt(lowerPool.size()));
        }

        // BEBEK HALİ İHTİMALİ (%25 Şansla bebek doğma simülasyonu)
        boolean baby = random.nextFloat() < 0.25f && Arrays.asList("ZOMBI", "PIGLIN", "TAVUK", "INEK", "KOYUN", "DOMUZ").contains(chosenMob);
        isBabyForm.put(uuid, baby);

        // ÖZEL BAŞLANGIÇ EŞYALARI VE CANLAR
        float maxHealth = 20.0F;
        if (chosenMob.equals("DEMIR_GOLEM")) maxH
