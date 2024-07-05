package io.papermc.generator.utils.experimental;

import com.google.common.base.Suppliers;
import io.papermc.generator.Main;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

public final class ExperimentalHelper {

    private static final Map<String, Supplier<Registry<? extends FeatureElement>>> FILTERED_REGISTRIES = FeatureElement.FILTERED_REGISTRIES.stream()
        .collect(Collectors.toMap((key) -> {
            if (key == Registries.ENTITY_TYPE) {
                return "entity";
            }
            return key.location().getPath();
        }, key -> Suppliers.memoize(() -> Main.REGISTRY_ACCESS.registryOrThrow(key))));

    private static final Set<String> EXPERIMENTAL_SOUND_EXCEPTIONS = Set.of(
        "event.mob_effect.trial_omen",
        "event.mob_effect.raid_omen"
        // add missing keys here for the next "big" update
    );

    // sounds is not dependent on feature flag
    // but some depends on flag locked element!
    public static SingleFlagHolder findSoundRelatedFeatureFlag(ResourceKey<SoundEvent> key) {
        String path = key.location().getPath();
        // the below way is not perfect, but it tries its best
        if (EXPERIMENTAL_SOUND_EXCEPTIONS.contains(path)) {
            return FlagHolders.NEXT_UPDATE;
        }

        String[] fragments = path.split("\\.");
        if (fragments.length < 2) {
            return null;
        }

        Supplier<Registry<? extends FeatureElement>> filteredRegistry = FILTERED_REGISTRIES.get(fragments[0]);
        if (filteredRegistry == null) {
            return null;
        }

        Optional<SingleFlagHolder> requiredFeature = getRequiredFeature(filteredRegistry.get(), fragments[1]);
        if (fragments[fragments.length - 2].equals("imitate")) { // parrot and note block
            requiredFeature = requiredFeature.or(() -> getRequiredFeature(BuiltInRegistries.ENTITY_TYPE, fragments[fragments.length - 1]));
        }

        return requiredFeature.orElse(null);
    }

    private static Optional<SingleFlagHolder> getRequiredFeature(Registry<? extends FeatureElement> registry, String key) {
        Optional<? extends FeatureElement> optionalElement = registry.getOptional(ResourceLocation.withDefaultNamespace(key));
        return optionalElement.map(element -> {
            if (element instanceof BundleItem) {
                return FlagHolders.BUNDLE; // special case since the item is not locked itself just in the creative menu
            }
            if (FeatureFlags.isExperimental(element.requiredFeatures())) {
                return SingleFlagHolder.fromSet(element.requiredFeatures());
            }
            return null;
        });
    }


    private static final Set<VillagerTrades.TreasureMapForEmeralds> EXPERIMENTAL_TRADES;

    static {
        Set<VillagerTrades.TreasureMapForEmeralds> experimentalTrades = new HashSet<>();

        collectTreasureMapTrades(VillagerTrades.EXPERIMENTAL_TRADES, experimentalTrades);
        collectTreasureMapTrades(VillagerTrades.EXPERIMENTAL_WANDERING_TRADER_TRADES.stream()
            .flatMap(tradeList -> Stream.of(tradeList.getLeft()))
            .toArray(VillagerTrades.ItemListing[]::new), experimentalTrades);

        EXPERIMENTAL_TRADES = Collections.unmodifiableSet(experimentalTrades);
    }

    // map decoration type is not dependent on feature flag
    // but some depends on flag locked element!
    private static void collectTreasureMapTrades(Map<VillagerProfession, Int2ObjectMap<VillagerTrades.ItemListing[]>> tradeMap, Set<VillagerTrades.TreasureMapForEmeralds> trades) {
        for (Int2ObjectMap<VillagerTrades.ItemListing[]> tradePerIndex : tradeMap.values()) {
            collectTreasureMapTrades(tradePerIndex, trades);
        }
    }

    private static void collectTreasureMapTrades(Int2ObjectMap<VillagerTrades.ItemListing[]> tradeMap, Set<VillagerTrades.TreasureMapForEmeralds> trades) {
        for (VillagerTrades.ItemListing[] tradeList : tradeMap.values()) {
            collectTreasureMapTrades(tradeList, trades);
        }
    }

    private static void collectTreasureMapTrades(VillagerTrades.ItemListing[] tradeList, Set<VillagerTrades.TreasureMapForEmeralds> trades) {
        for (VillagerTrades.ItemListing trade : tradeList) {
            if (trade instanceof VillagerTrades.TreasureMapForEmeralds treasureMapTrade) {
                trades.add(treasureMapTrade);
            } else if (trade instanceof VillagerTrades.TypeSpecificTrade typeSpecificTrade) {
                collectTreasureMapTradesPerVillageType(typeSpecificTrade, trades);
            }
        }
    }

    private static void collectTreasureMapTradesPerVillageType(VillagerTrades.TypeSpecificTrade specificTrade, Set<VillagerTrades.TreasureMapForEmeralds> trades) {
        for (VillagerTrades.ItemListing trade : specificTrade.trades().values()) {
            if (trade instanceof VillagerTrades.TreasureMapForEmeralds treasureMapTrade) {
                trades.add(treasureMapTrade);
            }
        }
    }

    public static SingleFlagHolder findMapDecorationTypeRelatedFeatureFlag(ResourceKey<MapDecorationType> key) {
        for (VillagerTrades.TreasureMapForEmeralds trade : EXPERIMENTAL_TRADES) {
            if (trade.destinationType.is(key)) {
                String featureFlagName = Main.EXPERIMENTAL_TAGS.get(trade.destination);
                if (featureFlagName != null) {
                    return SingleFlagHolder.fromVanillaName(featureFlagName);
                }
                break;
            }
        }
        return null;
    }
}
