package com.kartik.kodanda;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(KodandaMod.MODID)
public class KodandaMod {
    public static final String MODID = "kodanda";

    public KodandaMod(IEventBus modBus) {
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        modBus.addListener(this::addCreative);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(Astras::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            KodandaClient.init(modBus);
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.KODANDA_BOW.get());
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(CycleAstraPayload.TYPE, CycleAstraPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) {
                        KodandaBowItem.cycle(sp);
                    }
                }));
    }

    /** Safety net: if flight was granted by the bow but the bow is no longer in use, take it back. */
    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.getPersistentData().getBoolean(Astras.FLY_TAG)) {
            boolean ok = sp.isUsingItem()
                    && sp.getUseItem().getItem() instanceof KodandaBowItem
                    && KodandaBowItem.getKind(sp.getUseItem()) == ArrowKind.FLYING;
            if (!ok) {
                Astras.disableFlight(sp);
            }
        }
    }
}
