package com.kartik.kodanda;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

import java.util.List;

public class KodandaBowItem extends Item {
    private static final int MAX_FLY_TICKS = 1000; // 1000 ticks x 10 arrows = 10,000 arrows

    public KodandaBowItem(Properties props) {
        super(props);
    }

    // ----- selected astra (stored on the stack) -----

    public static ArrowKind getKind(ItemStack stack) {
        return ArrowKind.byIndex(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("astra"));
    }

    public static void setKind(ItemStack stack, ArrowKind kind) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("astra", kind.ordinal()));
    }

    /** Called on the server when the player presses R. */
    public static void cycle(ServerPlayer p) {
        InteractionHand hand = p.getMainHandItem().getItem() instanceof KodandaBowItem
                ? InteractionHand.MAIN_HAND
                : (p.getOffhandItem().getItem() instanceof KodandaBowItem ? InteractionHand.OFF_HAND : null);
        if (hand == null) return;
        ItemStack stack = p.getItemInHand(hand);
        ArrowKind next = getKind(stack).next();
        setKind(stack, next);
        p.displayClientMessage(Component.literal("\u2726 ").withStyle(ChatFormatting.GOLD).append(next.displayName()), true);
        Astras.sound(p.level(), p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.8f + next.ordinal() * 0.08f);
        Astras.sound(p.level(), p.position(), SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.6f);
    }

    // ----- item behaviour -----

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** Only used for Flying OP: stream of golden arrows while holding right-click. */
    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (level.isClientSide || !(living instanceof ServerPlayer p)) return;
        if (getKind(stack) != ArrowKind.FLYING) return;
        int used = getUseDuration(stack, living) - remaining;
        if (used > MAX_FLY_TICKS) {
            p.stopUsingItem();
            return;
        }
        Astras.enableFlight(p);
        ServerLevel sl = (ServerLevel) level;
        RandomSource r = p.getRandom();
        for (int i = 0; i < 10; i++) {
            KodandaArrowEntity a = new KodandaArrowEntity(sl, p, ArrowKind.FLYING, false);
            a.shootFromRotation(p, p.getXRot() + (r.nextFloat() - 0.5f) * 14f, p.getYRot() + (r.nextFloat() - 0.5f) * 14f, 0f, 3.2f, 0f);
            sl.addFreshEntity(a);
        }
        if (used % 4 == 0) Astras.sound(sl, p.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f, 1.4f);
        if (used % 20 == 0) Astras.sound(sl, p.position(), SoundEvents.TRIDENT_RIPTIDE_1, 1f, 1.2f);
    }

    /** Release the bow: fires the selected astra (or ends Flying OP). */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
        if (level.isClientSide || !(living instanceof ServerPlayer p)) return;
        ArrowKind kind = getKind(stack);
        if (kind == ArrowKind.FLYING) {
            Astras.disableFlight(p);
            return;
        }
        int charge = getUseDuration(stack, living) - timeLeft;
        if (charge < 6) return;
        float power = Math.min(charge / 15f, 1f);
        KodandaArrowEntity arrow = new KodandaArrowEntity(level, p, kind, false);
        arrow.shootFromRotation(p, p.getXRot(), p.getYRot(), 0f, 1.5f + power * 2.5f, 0.2f);
        level.addFreshEntity(arrow);
        Astras.launchSound(level, p.position(), kind);
        p.getCooldowns().addCooldown(this, kind.cooldown);
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(s -> s.withColor(0xFFD700).withBold(true));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("The divine bow of Shree Ram").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tip.add(Component.literal("Astra: ").withStyle(ChatFormatting.GRAY).append(getKind(stack).displayName()));
        tip.add(Component.literal("Hold Right-Click: shoot  |  R: switch astra").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Infinite arrows").withStyle(ChatFormatting.YELLOW));
    }
}
