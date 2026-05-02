package com.ronm19.wolfism.world;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.ModEntities;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

public class ModEntitySpawns {

    public static final ResourceKey<BiomeModifier> ADD_END_WOLF_SPAWNS = registerKey("add_end_wolf_spawns");
    public static final ResourceKey<BiomeModifier> ADD_VOID_WOLF_SPAWNS = registerKey("add_void_wolf_spawns");

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(ADD_END_WOLF_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(biomes.getOrThrow(BiomeTags.IS_END), List.of(new MobSpawnSettings.SpawnerData(
                                        ModEntities.END_WOLF.get(), 90, 1, 3))));
        context.register(ADD_VOID_WOLF_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(biomes.getOrThrow(BiomeTags.IS_END), List.of(new MobSpawnSettings.SpawnerData(
                ModEntities.VOID_WOLF.get(), 90, 1, 3))));
    }

    private static ResourceKey<BiomeModifier> registerKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, name));
    }
}