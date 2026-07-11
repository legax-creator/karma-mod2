package com.karmamod;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FeedingManager {
    public static boolean isCorrectFood(String mob, ItemStack item) {
        if ((mob.contains("salmon") || mob.contains("cod")) && item.is(Items.KELP)) return true;
        if ((mob.contains("villager") || mob.contains("wandering_trader")) && item.is(Items.BREAD)) return true;
        if (mob.contains("frog") && (item.is(Items.COD) || item.is(Items.SALMON))) return true;
        if (mob.contains("silverfish") && item.is(Items.STONE)) return true;
        if ((mob.contains("donkey") || mob.contains("horse")) && item.is(Items.GOLDEN_CARROT)) return true;
        if ((mob.contains("parrot") || mob.contains("chicken")) && item.is(Items.WHEAT_SEEDS)) return true;
        if (mob.contains("bee") && item.is(Items.POPPY)) return true;
        if ((mob.contains("hoglin") || mob.contains("piglin")) && item.is(Items.CARROT)) return true;
        if ((mob.contains("zombie") || mob.contains("skeleton")) && item.is(Items.BEEF)) return true;
        if (mob.contains("spider") && item.is(Items.STRING)) return true;
        if (mob.contains("enderman") && item.is(Items.WARPED_FUNGUS)) return true;
        if (mob.contains("iron_golem") && item.is(Items.IRON_INGOT)) return true;
        return false;
    }
}
