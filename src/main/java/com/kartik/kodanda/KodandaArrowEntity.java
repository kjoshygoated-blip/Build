package com.kartik.kodanda;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class KodandaArrowEntity extends AbstractArrow {
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(KodandaArrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SECONDARY =
            SynchedEntityData.defineId(KodandaArrowEntity.class, EntityDataSerializers.BOOLEAN);

    private int age;
    private int stuckTicks;
    private boolean stuck;
    private final Set<Integer> hitIds = new HashSet<>();

    public KodandaArrowEntity(EntityType<? extends KodandaArrowEntity> type, Level level) {
        super(type, level);
    }

    public KodandaArrowEntity(Level level, LivingEntity owner, ArrowKind kind, boolean secondary) {
        super(ModEntities.KODANDA_ARROW.get(), owner, level, new ItemStack(Items.ARROW), null);
        this.entityData.set(DATA_KIND, kind.ordinal());
        this.entityData.set(DATA_SECONDARY, secondary);
        this.pickup = AbstractArrow.Pickup.DISALLOWED;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, 0);
        builder.define(DATA_SECONDARY, false);
    }

    public ArrowKind kind() {
        return ArrowKind.byIndex(this.entityData.get(DATA_KIND));
    }

    public boolean isSecondary() {
        return this.entityData.get(DATA_SECONDARY);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (kind() == ArrowKind.FLYING) {
            // golden arrows fly through enemies, hitting each one once (never the shooter or their pets)
            if (hitIds.contains(target.getId())) return false;
            if (target == getOwner()) return false;
            if (target instanceof TamableAnimal pet && getOwner() instanceof LivingEntity o && pet.isOwnedBy(o)) return false;
            return super.canHitEntity(target);
        }
        if (isSecondary() && target == getOwner()) return false;
        return super.canHitEntity(target);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (level().isClientSide) {
            trail();
            return;
        }
        ArrowKind k = kind();
        int max = isSecondary() ? 80 : (k == ArrowKind.FLYING ? 35 : 100);
        if (age > max) {
            discard();
            return;
        }
        if (stuck && ++stuckTicks > 10) {
            discard();
        }
    }

    private void trail() {
        ArrowKind k = kind();
        boolean light = isSecondary() || k == ArrowKind.FLYING;
        ParticleOptions dust = new DustParticleOptions(Astras.rgb(k.color), light ? 0.9f : 1.5f);
        Vec3 mv = getDeltaMovement();
        if (light) {
            if (age % 2 == 0) level().addParticle(dust, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        for (int i = 0; i < 3; i++) {
            double t = i / 3.0;
            level().addParticle(dust, getX() - mv.x * t, getY() - mv.y * t, getZ() - mv.z * t, 0, 0, 0);
        }
        ParticleOptions extra = switch (k) {
            case INDRASTRA -> ParticleTypes.ELECTRIC_SPARK;
            case VARUNASTRA -> ParticleTypes.FALLING_WATER;
            case AGNEYASTRA -> ParticleTypes.FLAME;
            case VAYAVYASTRA -> ParticleTypes.CLOUD;
            case NARAYANASTRA -> ParticleTypes.SOUL_FIRE_FLAME;
            default -> ParticleTypes.END_ROD;
        };
        level().addParticle(extra, getX(), getY(), getZ(), 0, 0, 0);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        if (kind() == ArrowKind.FLYING) {
            hitIds.add(target.getId());
            target.invulnerableTime = 0; // so every single arrow lands damage
            target.hurt(damageSources().arrow(this, getOwner()), 8f);
            return; // keep flying through
        }
        ArrowKind k = kind();
        if (k != ArrowKind.VARUNASTRA) {
            target.hurt(damageSources().arrow(this, getOwner()), isSecondary() ? 7f : 10f);
        }
        if (!isSecondary()) {
            Astras.detonate((ServerLevel) level(), result.getLocation(), k, getOwner());
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (level().isClientSide || kind() == ArrowKind.FLYING) {
            super.onHitBlock(result);
            if (kind() == ArrowKind.FLYING) stuck = true;
            return;
        }
        if (!isSecondary()) {
            Vec3 n = Vec3.atLowerCornerOf(result.getDirection().getNormal()).scale(0.5);
            Astras.detonate((ServerLevel) level(), result.getLocation().add(n), kind(), getOwner());
        }
        discard();
    }
}
