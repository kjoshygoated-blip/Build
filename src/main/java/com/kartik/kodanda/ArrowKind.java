package com.kartik.kodanda;

import net.minecraft.network.chat.Component;

public enum ArrowKind {
    BRAHMASTRA("Brahmastra", 0xFFD700, 20),
    BRAHMASHIRA("Brahmashira", 0xFFB000, 60),
    NARAYANASTRA("Narayanastra", 0x3A8DFF, 40),
    INDRASTRA("Indrastra", 0xFFFF40, 25),
    VARUNASTRA("Varunastra", 0x33CCFF, 20),
    AGNEYASTRA("Agneyastra", 0xFF3311, 25),
    VAYAVYASTRA("Vayavyastra", 0x55FF77, 30),
    FLYING("Flying OP", 0xFFE066, 0);

    public final String label;
    public final int color;
    public final int cooldown;

    ArrowKind(String label, int color, int cooldown) {
        this.label = label;
        this.color = color;
        this.cooldown = cooldown;
    }

    public ArrowKind next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static ArrowKind byIndex(int i) {
        ArrowKind[] v = values();
        return v[Math.floorMod(i, v.length)];
    }

    public Component displayName() {
        return Component.literal(label).withStyle(s -> s.withColor(color).withBold(true));
    }
}
