package com.kartik.kodanda;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CycleAstraPayload() implements CustomPacketPayload {
    public static final Type<CycleAstraPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(KodandaMod.MODID, "cycle_astra"));
    public static final StreamCodec<ByteBuf, CycleAstraPayload> CODEC = StreamCodec.unit(new CycleAstraPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
