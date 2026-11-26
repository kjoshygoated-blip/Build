package com.kartik.kodanda;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** All astra impact effects, sounds and the little scheduler that drives multi-tick effects. */
public final class Astras {
    /** Set true if you want the big explosions to destroy terrain. Default: entities only. */
    public static final boolean DESTROY_BLOCKS = false;
    public static final String FLY_TAG = "kodanda_fly";

    @FunctionalInterface
    private interface Task {
        boolean tick(); // return true to keep running
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private Astras() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        if (TASKS.isEmpty()) return;
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            boolean keep;
            try {
                keep = it.next().tick();
            } catch (Exception ex) {
                keep = false;
            }
            if (!keep) it.remove();
        }
    }

    // ---------- helpers ----------

    public static Vector3f rgb(int c) {
        return new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
    }

    /** Accepts either a SoundEvent or a Holder<SoundEvent> so it compiles regardless of how vanilla exposes them. */
    public static void sound(Level level, Vec3 p, Object snd, float vol, float pitch) {
        SoundEvent ev = snd instanceof Holder<?> h ? (SoundEvent) h.value() : (SoundEvent) snd;
        level.playSound(null, p.x, p.y, p.z, ev, SoundSource.PLAYERS, vol, pitch);
    }

    public static void launchSound(Level level, Vec3 p, ArrowKind kind) {
        sound(level, p, SoundEvents.ARROW_SHOOT, 1.2f, 0.8f);
        switch (kind) {
            case BRAHMASTRA -> sound(level, p, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.6f);
            case BRAHMASHIRA -> sound(level, p, SoundEvents.WITHER_SHOOT, 1.5f, 0.7f);
            case NARAYANASTRA -> sound(level, p, SoundEvents.BELL_BLOCK, 3f, 0.6f);
            case INDRASTRA -> sound(level, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, 1.8f);
            case VARUNASTRA -> sound(level, p, SoundEvents.GENERIC_SPLASH, 1.5f, 1.2f);
            case AGNEYASTRA -> sound(level, p, SoundEvents.BLAZE_SHOOT, 1.5f, 0.8f);
            case VAYAVYASTRA -> sound(level, p, SoundEvents.ENDER_DRAGON_FLAP, 2f, 1.2f);
            default -> sound(level, p, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        }
    }

    // ---------- impact dispatch ----------

    public static void detonate(ServerLevel level, Vec3 pos, ArrowKind kind, Entity owner) {
        switch (kind) {
            case BRAHMASTRA -> brahma(level, pos, owner, 5f, 1f);
            case BRAHMASHIRA -> brahma(level, pos, owner, 20f, 4f); // 4x radius
            case NARAYANASTRA -> rain(level, pos, owner);
            case INDRASTRA -> lightning(level, pos, owner);
            case VARUNASTRA -> varuna(level, pos);
            case AGNEYASTRA -> agni(level, pos, owner);
            case VAYAVYASTRA -> tornado(level, pos, owner);
            default -> { }
        }
    }

    private static Level.ExplosionInteraction interaction() {
        return DESTROY_BLOCKS ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
    }

    // 1 & 2: golden explosions
    private static void brahma(ServerLevel level, Vec3 c, Entity owner, float radius, float scale) {
        level.explode(owner, c.x, c.y, c.z, radius, interaction());
        DustParticleOptions gold = new DustParticleOptions(rgb(0xFFD700), 3f);
        double s = radius * 0.35;
        level.sendParticles(gold, c.x, c.y, c.z, (int) (250 * scale), s, s, s, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, (int) (120 * scale), 0.2, 0.2, 0.2, 0.5 * Math.sqrt(scale));
        level.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y, c.z, (int) (80 * scale), s * 0.5, s * 0.5, s * 0.5, 0.25);
        sound(level, c, SoundEvents.GENERIC_EXPLODE, 4f * scale, 0.8f);
        sound(level, c, SoundEvents.BEACON_ACTIVATE, 3f, 1.4f);
        sound(level, c, SoundEvents.TOTEM_USE, 2f * scale, 0.9f);
    }

    // 3: 1000 blue arrows from the sky (40 per tick for 25 ticks)
    private static void rain(ServerLevel level, Vec3 c, Entity owner) {
        if (!(owner instanceof LivingEntity shooter)) return;
        sound(level, c, SoundEvents.BEACON_POWER_SELECT, 4f, 0.8f);
        int[] n = {0};
        TASKS.add(() -> {
            for (int i = 0; i < 40; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(level.random.nextDouble()) * 11;
                KodandaArrowEntity arrow = new KodandaArrowEntity(level, shooter, ArrowKind.NARAYANASTRA, true);
                arrow.setPos(c.x + Math.cos(a) * r, c.y + 28 + level.random.nextDouble() * 6, c.z + Math.sin(a) * r);
                arrow.setDeltaMovement(level.random.nextGaussian() * 0.03, -2.6, level.random.nextGaussian() * 0.03);
                level.addFreshEntity(arrow);
            }
            if (n[0] % 5 == 0) sound(level, c, SoundEvents.ARROW_SHOOT, 3f, 0.6f);
            return ++n[0] < 25;
        });
    }

    // 4: yellow lightning + thunder
    private static void lightning(ServerLevel level, Vec3 c, Entity owner) {
        sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 10f, 0.9f);
        DustParticleOptions yellow = new DustParticleOptions(rgb(0xFFFF40), 2f);
        int[] n = {0};
        TASKS.add(() -> {
            if (n[0] % 2 == 0) {
                double x = c.x, z = c.z;
                if (n[0] > 0) {
                    x += (level.random.nextDouble() - 0.5) * 14;
                    z += (level.random.nextDouble() - 0.5) * 14;
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(x, c.y, z);
                    if (owner instanceof ServerPlayer sp) bolt.setCause(sp);
                    level.addFreshEntity(bolt);
                }
                level.sendParticles(yellow, x, c.y + 6, z, 80, 0.4, 6, 0.4, 0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, c.y + 1, z, 40, 1.5, 1, 1.5, 0.4);
                sound(level, new Vec3(x, c.y, z), SoundEvents.LIGHTNING_BOLT_THUNDER, 6f, 0.8f + level.random.nextFloat() * 0.4f);
            }
            return ++n[0] < 16;
        });
    }

    // 5: water splash + heal
    private static void varuna(ServerLevel level, Vec3 c) {
        AABB box = new AABB(c, c).inflate(7);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e instanceof Enemy) continue;
            e.heal(10f);
            e.clearFire();
            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
        }
        DustParticleOptions blue = new DustParticleOptions(rgb(0x33CCFF), 2.2f);
        level.sendParticles(blue, c.x, c.y + 1, c.z, 200, 3, 1.5, 3, 0.02);
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 1, c.z, 400, 3, 1.5, 3, 0.4);
        level.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 4, c.z, 250, 3, 1, 3, 0);
        level.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 1, c.z, 100, 3, 1.5, 3, 0.1);
        sound(level, c, SoundEvents.GENERIC_SPLASH, 3f, 0.9f);
        sound(level, c, SoundEvents.PLAYER_LEVELUP, 1.5f, 1.3f);
    }

    // 6: red fire explosion
    private static void agni(ServerLevel level, Vec3 c, Entity owner) {
        level.explode(owner, c.x, c.y, c.z, 6f, true, interaction());
        AABB box = new AABB(c, c).inflate(7);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != owner)) {
            e.setRemainingFireTicks(160);
        }
        DustParticleOptions red = new DustParticleOptions(rgb(0xFF3311), 3f);
        level.sendParticles(red, c.x, c.y + 1, c.z, 300, 2.5, 2, 2.5, 0.02);
        level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 1, c.z, 200, 1, 1, 1, 0.25);
        level.sendParticles(ParticleTypes.LAVA, c.x, c.y + 1, c.z, 50, 2, 1, 2, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 2, c.z, 80, 1.5, 1.5, 1.5, 0.05);
        sound(level, c, SoundEvents.GENERIC_EXPLODE, 3f, 0.7f);
        sound(level, c, SoundEvents.FIRECHARGE_USE, 3f, 0.6f);
        sound(level, c, SoundEvents.BLAZE_SHOOT, 2f, 0.5f);
    }

    // 7: green wind tornado (6 seconds)
    private static void tornado(ServerLevel level, Vec3 c, Entity owner) {
        sound(level, c, SoundEvents.ENDER_DRAGON_FLAP, 4f, 0.6f);
        DustParticleOptions green = new DustParticleOptions(rgb(0x55FF77), 1.6f);
        int[] t = {0};
        TASKS.add(() -> {
            int tick = t[0];
            for (int h = 0; h < 16; h += 2) {
                double r = 0.6 + h * 0.28;
                for (int arm = 0; arm < 2; arm++) {
                    double a = tick * 0.45 + h * 0.7 + arm * Math.PI;
                    level.sendParticles(green, c.x + Math.cos(a) * r, c.y + h * 0.9, c.z + Math.sin(a) * r, 2, 0.08, 0.08, 0.08, 0);
                }
            }
            if (tick % 2 == 0) level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 4, c.z, 6, 1.5, 5, 1.5, 0.02);
            if (tick % 12 == 0) sound(level, c, SoundEvents.ENDER_DRAGON_FLAP, 2f, 0.5f);
            // hostile mobs only: a big shove on impact, then a wind wall that keeps sliding them away
            AABB box = new AABB(c.x - 12, c.y - 2, c.z - 12, c.x + 12, c.y + 14, c.z + 12);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x != owner && x instanceof Enemy)) {
                double dx = e.getX() - c.x, dz = e.getZ() - c.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (tick > 0 && dist > 8) continue;
                Vec3 out = dist < 0.01 ? new Vec3(1, 0, 0) : new Vec3(dx / dist, 0, dz / dist);
                Vec3 swirl = new Vec3(-out.z, 0, out.x).scale(0.3);
                double power = tick == 0 ? 2.0 : 0.9;
                Vec3 mv = e.getDeltaMovement();
                double y = e.onGround() ? 0.25 : Math.max(mv.y, 0.1);
                e.setDeltaMovement(out.x * power + swirl.x, y, out.z * power + swirl.z);
                e.hurtMarked = true;
                e.fallDistance = 0;
                if (tick % 10 == 0) e.hurt(level.damageSources().magic(), 2f);
            }
            return ++t[0] < 120;
        });
    }

    // ---------- Flying OP flight ----------

    public static void enableFlight(ServerPlayer p) {
        Abilities ab = p.getAbilities();
        boolean changed = false;
        if (!ab.mayfly) { ab.mayfly = true; changed = true; }
        if (!ab.flying) { ab.flying = true; changed = true; }
        if (ab.getFlyingSpeed() < 0.1f) { ab.setFlyingSpeed(0.12f); changed = true; }
        p.getPersistentData().putBoolean(FLY_TAG, true);
        if (changed) p.onUpdateAbilities();
        p.fallDistance = 0;
    }

    public static void disableFlight(ServerPlayer p) {
        if (!p.getPersistentData().getBoolean(FLY_TAG)) return;
        p.getPersistentData().remove(FLY_TAG);
        Abilities ab = p.getAbilities();
        ab.setFlyingSpeed(0.05f);
        if (!p.isCreative() && !p.isSpectator()) {
            ab.mayfly = false;
            ab.flying = false;
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
        }
        p.onUpdateAbilities();
        p.fallDistance = 0;
    }
}
