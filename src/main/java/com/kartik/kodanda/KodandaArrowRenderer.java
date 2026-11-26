package com.kartik.kodanda;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class KodandaArrowRenderer extends ArrowRenderer<KodandaArrowEntity> {
    private static final ResourceLocation TEX =
            ResourceLocation.withDefaultNamespace("textures/entity/projectiles/spectral_arrow.png");

    public KodandaArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(KodandaArrowEntity entity) {
        return TEX;
    }

    @Override
    protected int getBlockLightLevel(KodandaArrowEntity entity, BlockPos pos) {
        return 15; // glows in the dark
    }
}
