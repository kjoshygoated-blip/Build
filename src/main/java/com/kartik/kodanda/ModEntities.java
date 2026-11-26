package com.kartik.kodanda;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, KodandaMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<KodandaArrowEntity>> KODANDA_ARROW =
            ENTITIES.register("kodanda_arrow", () -> EntityType.Builder
                    .<KodandaArrowEntity>of(KodandaArrowEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(4)
                    .updateInterval(5)
                    .noSave()
                    .build("kodanda_arrow"));
}
