package com.karmamod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KarmaMod.MODID)
public class KarmaMod {
    public static final String MODID = "karmamod";

    public static void init() {
        // Alt sistemlerin ve network paketlerinin kaydı
        NetworkHandler.register();
    }

    public KarmaMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);
        init();
    }
}
