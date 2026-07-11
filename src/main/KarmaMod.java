package com.karmamod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KarmaMod.MODID, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        g.drawCenteredString(mc.font, "FORM: " + KarmaSystem.currentMobForm, mc.getWindow().getGuiScaledWidth() / 2, 10, 0xFFFFAA00);
        g.drawCenteredString(mc.font, "KARMA: %" + KarmaSystem.karmaBar, mc.getWindow().getGuiScaledWidth() / 2, 22, 0xFFFFFF00);
    }

    // İstemci tarafında oyuncu modelinin boyutsal ve görsel uyarlaması
    @SubscribeEvent
    public static void onPreRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity == Minecraft.getInstance().player) {
            String form = KarmaSystem.currentMobForm.toLowerCase();
            if (form.contains("iron_golem") || form.contains("warden")) {
                event.getPoseStack().pushPose();
                event.getPoseStack().scale(1.3F, 1.3F, 1.3F); // Golem/Warden için boyutu büyüt
            } else if (form.contains("bat") || form.contains("silverfish")) {
                event.getPoseStack().pushPose();
                event.getPoseStack().scale(0.4F, 0.4F, 0.4F); // Küçük moblar için küçült
            }
        }
    }

    @SubscribeEvent
    public static void onPostRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() == Minecraft.getInstance().player) {
            String form = KarmaSystem.currentMobForm.toLowerCase();
            if (form.contains("iron_golem") || form.contains("warden") || form.contains("bat") || form.contains("silverfish")) {
                event.getPoseStack().popPose();
            }
        }
    }
}
