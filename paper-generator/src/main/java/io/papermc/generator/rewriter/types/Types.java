package io.papermc.generator.rewriter.types;

import io.papermc.typewriter.ClassNamed;

public final class Types {

    public static final String BASE_PACKAGE = "org.bukkit.craftbukkit";

    public static final ClassNamed CRAFT_BLOCK_DATA = ClassNamed.of(BASE_PACKAGE + ".block.data", "CraftBlockData");

    public static final ClassNamed CRAFT_BLOCK_STATES = ClassNamed.of(BASE_PACKAGE + ".block", "CraftBlockStates");

    public static final ClassNamed CRAFT_STATISTIC = ClassNamed.of(BASE_PACKAGE, "CraftStatistic");

    public static final ClassNamed CRAFT_POTION_UTIL = ClassNamed.of(BASE_PACKAGE + ".potion", "CraftPotionUtil");
}
