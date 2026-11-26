package com.kartik.kodanda;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class KodandaClient {
    public static final KeyMapping SWITCH = new KeyMapping("key.kodanda.switch_astra",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.kodanda");

    public static void init(IEventBus modBus) {
        modBus.addListener(KodandaClient::registerKeys);
        modBus.addListener(KodandaClient::registerRenderers);
        modBus.addListener(KodandaClient::clientSetup);
        NeoForge.EVENT_BUS.addListener(KodandaClient::clientTick);
    }

    private static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(SWITCH);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.KODANDA_ARROW.get(), KodandaArrowRenderer::new);
    }

    private static void clientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            var bow = ModItems.KODANDA_BOW.get();
            ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pull"), (stack, level, entity, seed) -> {
                if (entity == null || entity.getUseItem() != stack) return 0f;
                return (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20f;
            });
            ItemProperties.register(bow, ResourceLocation.withDefaultNamespace("pulling"), (stack, level, entity, seed) ->
                    entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
        });
    }

    private static void clientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (SWITCH.consumeClick()) {
            if (mc.player != null && (mc.player.getMainHandItem().getItem() instanceof KodandaBowItem
                    || mc.player.getOffhandItem().getItem() instanceof KodandaBowItem)) {
                PacketDistributor.sendToServer(new CycleAstraPayload());
            }
        }
    }
}
