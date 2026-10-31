package com.bloodsteal.gem;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.Locale;

public enum Gem {
    BLOOD("Blood Gem", NamedTextColor.DARK_RED, Material.RED_DYE, 1.5, 9101, "Blood Burst"),
    CRIMSON("Crimson Gem", NamedTextColor.RED, Material.REDSTONE, 2.5, 9102, "Crimson Rage"),
    SHADOW("Shadow Gem", NamedTextColor.DARK_PURPLE, Material.ECHO_SHARD, 4.0, 9103, "Shadow Blink"),
    INFERNO("Inferno Gem", NamedTextColor.GOLD, Material.BLAZE_ROD, 6.0, 9104, "Infernal Execution"),
    AVERON("Averon Gem", NamedTextColor.LIGHT_PURPLE, Material.NETHER_STAR, 9.0, 9105,
            "Blood Dominion / Averon Rift / Supreme Blood");

    private final String displayName;
    private final TextColor color;
    private final Material material;
    private final double multiplier;
    private final int modelData;
    private final String abilityLabel;

    Gem(String displayName, TextColor color, Material material, double multiplier, int modelData, String abilityLabel) {
        this.displayName = displayName;
        this.color = color;
        this.materi
