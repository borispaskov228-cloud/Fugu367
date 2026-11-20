package com.herbcraft;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.registry.DynamicRegistrySetupCallback;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.PlacedFeatures;
import net.minecraft.world.gen.feature.RandomPatchFeatureConfig;
import net.minecraft.world.gen.feature.SimpleBlockFeatureConfig;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;
import net.minecraft.world.gen.placementmodifier.RarityFilterPlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.List;

/**
 * Генерация диких трав через обычный worldgen-фичу.
 * ВАЖНО: не трогаем мир во время загрузки чанков — это вызывало зависание
 * создания мира. Всё регистрируется в динамических реестрах 1.20.1.
 */
public class HerbWorldGen {
    public static void register() {
        if (!HerbCraftConfig.generateWildHerbs()) {
            HerbCraft.LOGGER.info("[HerbCraft] Генерация диких трав отключена в конфиге");
            return;
        }

        DynamicRegistrySetupCallback.EVENT.register(view -> {
            Registry<ConfiguredFeature<?, ?>> configured =
                    view.getOptional(RegistryKeys.CONFIGURED_FEATURE).orElse(null);
            Registry<PlacedFeature> placed =
                    view.getOptional(RegistryKeys.PLACED_FEATURE).orElse(null);
            if (configured == null || placed == null) return;

            for (HerbType type : HerbType.values()) {
                String path = type.id + "_wild";

                ConfiguredFeature<?, ?> single = Registry.register(
                        configured,
                        HerbCraft.id("single_" + path),
                        new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                                new SimpleBlockFeatureConfig(BlockStateProvider.of(
                                        HerbCraftBlocks.WILD.get(type).getDefaultState()))));

                PlacedFeature singlePlaced = Registry.register(
                        placed,
                        HerbCraft.id("single_" + path + "_placed"),
                        new PlacedFeature(RegistryEntry.of(single), List.of()));

                ConfiguredFeature<?, ?> patch = Registry.register(
                        configured,
                        HerbCraft.id("patch_" + path),
                        new ConfiguredFeature<>(Feature.FLOWER,
                                new RandomPatchFeatureConfig(12, 5, 2,
                                        RegistryEntry.of(singlePlaced))));

                Identifier placedId = HerbCraft.id("patch_" + path + "_placed");
                Registry.register(
                        placed,
                        placedId,
                        new PlacedFeature(RegistryEntry.of(patch), List.of(
                                RarityFilterPlacementModifier.of(16),
                                SquarePlacementModifier.of(),
                                PlacedFeatures.MOTION_BLOCKING_HEIGHTMAP,
                                BiomePlacementModifier.of())));
            }
        });

        // Привязываем фичи к биомам один раз, на этапе инициализации мода.
        for (HerbType type : HerbType.values()) {
            BiomeModifications.addFeature(
                    BiomeSelectors.foundInOverworld(),
                    GenerationStep.Feature.VEGETAL_DECORATION,
                    RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                            HerbCraft.id("patch_" + type.id + "_wild_placed")));
        }
    }
}
