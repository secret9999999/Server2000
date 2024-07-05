package io.papermc.generator;

import io.papermc.generator.types.SourceGenerator;
import io.papermc.generator.types.craftblockdata.CraftBlockDataGenerators;
import io.papermc.generator.types.goal.MobGoalGenerator;
import io.papermc.generator.types.registry.GeneratedKeyType;
import io.papermc.generator.types.registry.GeneratedTagKeyType;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.bukkit.GameEvent;
import org.bukkit.MusicInstrument;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockType;
import org.bukkit.damage.DamageType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Wolf;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public interface Generators {

    List<SourceGenerator> API = List.of(
        simpleKey("GameEventKeys", GameEvent.class, Registries.GAME_EVENT, RegistryKey.GAME_EVENT, false),
        simpleKey("BiomeKeys", Biome.class, Registries.BIOME, RegistryKey.BIOME, true),
        simpleKey("TrimMaterialKeys", TrimMaterial.class, Registries.TRIM_MATERIAL, RegistryKey.TRIM_MATERIAL, true),
        simpleKey("TrimPatternKeys", TrimPattern.class, Registries.TRIM_PATTERN, RegistryKey.TRIM_PATTERN, true),
        simpleKey("StructureKeys", Structure.class, Registries.STRUCTURE, RegistryKey.STRUCTURE, true),
        simpleKey("StructureTypeKeys", StructureType.class, Registries.STRUCTURE_TYPE, RegistryKey.STRUCTURE_TYPE, false),
        simpleKey("InstrumentKeys", MusicInstrument.class, Registries.INSTRUMENT, RegistryKey.INSTRUMENT, false),
        simpleKey("EnchantmentKeys", Enchantment.class, Registries.ENCHANTMENT, RegistryKey.ENCHANTMENT, false),
        simpleKey("MobEffectKeys", PotionEffectType.class, Registries.MOB_EFFECT, RegistryKey.MOB_EFFECT, false),
        simpleKey("DamageTypeKeys", DamageType.class, Registries.DAMAGE_TYPE, RegistryKey.DAMAGE_TYPE, true),
        simpleKey("WolfVariantKeys", Wolf.Variant.class, Registries.WOLF_VARIANT, RegistryKey.WOLF_VARIANT, true),
        simpleKey("BlockTypeKeys", BlockType.class, Registries.BLOCK, RegistryKey.BLOCK, false),
        simpleKey("ItemTypeKeys", ItemType.class, Registries.ITEM, RegistryKey.ITEM, false),

        simpleTagKey("BlockTypeTagKeys", BlockType.class, Registries.BLOCK, RegistryKey.BLOCK),
        simpleTagKey("ItemTypeTagKeys", ItemType.class, Registries.ITEM, RegistryKey.ITEM),
        simpleTagKey("EnchantmentTagKeys", Enchantment.class, Registries.ENCHANTMENT, RegistryKey.ENCHANTMENT),
        new MobGoalGenerator("VanillaGoal", "com.destroystokyo.paper.entity.ai")
        // todo extract fields for registry based api
    );

    private static <T, A> SourceGenerator simpleKey(final String className, final Class<A> apiType, final ResourceKey<? extends Registry<T>> registryKey, final RegistryKey<A> apiRegistryKey, final boolean publicCreateKeyMethod) {
        return new GeneratedKeyType<>(className, apiType, "io.papermc.paper.registry.keys", registryKey, apiRegistryKey, publicCreateKeyMethod);
    }

    private static <T, A> SourceGenerator simpleTagKey(final String className, final Class<A> apiType, final ResourceKey<? extends Registry<T>> registryKey, final RegistryKey<A> apiRegistryKey) {
        return new GeneratedTagKeyType<>(className, apiType, "io.papermc.paper.registry.keys.tags", registryKey, apiRegistryKey, true);
    }

    List<SourceGenerator> SERVER = Collections.unmodifiableList(Util.make(new ArrayList<>(), CraftBlockDataGenerators::bootstrap));
}
