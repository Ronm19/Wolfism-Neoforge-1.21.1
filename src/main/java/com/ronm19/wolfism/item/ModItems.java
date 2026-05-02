package com.ronm19.wolfism.item;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.ModEntities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static DeferredRegister.Items ITEMS = DeferredRegister.createItems(WolfismMod.MOD_ID);

    // ------------------------------------------ ITEMS ---------------------------------------------------- //

    public static final DeferredItem<Item> PRIMAL_BONE = ITEMS.registerItem("primal_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> EMBER_BONE = ITEMS.registerItem("ember_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> FROST_BONE = ITEMS.registerItem("frost_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> CHARGED_BONE = ITEMS.registerItem("charged_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> SHADOW_BONE = ITEMS.registerItem("shadow_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> SPIRIT_BONE = ITEMS.registerItem("spirit_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> GOLDEN_BONE = ITEMS.registerItem("golden_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> ARCANE_BONE = ITEMS.registerItem("arcane_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> BLOOD_BONE = ITEMS.registerItem("blood_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> ANGEL_BONE = ITEMS.registerItem("angel_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> LUNAR_BONE = ITEMS.registerItem("lunar_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> ROYAL_BONE = ITEMS.registerItem("royal_bone", Item::new, new Item.Properties());
    public static final DeferredItem<Item> VOID_BONE = ITEMS.registerItem("void_bone", Item::new, new Item.Properties());


    public static final DeferredItem<Item> BLOOD_GEM = ITEMS.registerItem("blood_gem", Item::new, new Item.Properties());


    // ------------------------------------------ TOOLS ---------------------------------------------------- //



    // ------------------------------------------ ARMORS ---------------------------------------------------- //



    // ------------------------------------------ SPAWN EGGs ---------------------------------------------------- //


    // ------------------------------------ ELEMENTAL --------------------------------- //

    public static final DeferredItem<Item> FROST_WOLF_SPAWN_EGG = ITEMS.register("frost_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_WOLF, 0xBFE9FF, 0x6EC7FF, new Item.Properties()));

    public static final DeferredItem<Item> FIRE_WOLF_SPAWN_EGG = ITEMS.register("fire_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FIRE_WOLF, 0x2B1B17, 0xE25822, new Item.Properties()));

    public static final DeferredItem<Item> STORM_WOLF_SPAWN_EGG = ITEMS.register("storm_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.STORM_WOLF, 0x2E333C, 0x6FD5FF, new Item.Properties()));

    public static final DeferredItem<Item> EARTH_WOLF_SPAWN_EGG = ITEMS.register("earth_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.EARTH_WOLF, 0x7A674F, 0x556B3D, new Item.Properties()));

    public static final DeferredItem<Item> WATER_WOLF_SPAWN_EGG = ITEMS.register("water_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.WATER_WOLF, 0x3F8F95, 0xA6E3E6, new Item.Properties()));

    // ------------------------------------ NEUTRAL --------------------------------- //

    public static final DeferredItem<Item> TIMBER_WOLF_SPAWN_EGG = ITEMS.register("timber_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TIMBER_WOLF, 0x4A3B2E, 0xB9AE9B, new Item.Properties()));

    public static final DeferredItem<Item> ARCTIC_WOLF_SPAWN_EGG = ITEMS.register("arctic_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ARCTIC_WOLF, 0xD9D9D4, 0x4A4A4A, new Item.Properties()));

    public static final DeferredItem<Item> BLACK_WOLF_SPAWN_EGG = ITEMS.register("black_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLACK_WOLF, 0x1A1A1A, 0x7C7C7C, new Item.Properties()));

    // ---------------------------- SPECIAL --------------------------------- //

    public static final DeferredItem<Item> SPIRIT_WOLF_SPAWN_EGG = ITEMS.register("spirit_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SPIRIT_WOLF, 0xE8F7FF, 0x7FCBEF, new Item.Properties()));

    public static final DeferredItem<Item> SHADOW_WOLF_SPAWN_EGG = ITEMS.register("shadow_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SHADOW_WOLF, 0x0A0A0A, 0x6A00FF, new Item.Properties()));

    public static final DeferredItem<Item> GOLDEN_WOLF_SPAWN_EGG = ITEMS.register("golden_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GOLDEN_WOLF, 0xD9A441, 0xFFF2A6, new Item.Properties()));

    public static final DeferredItem<Item> VIOLET_WOLF_SPAWN_EGG = ITEMS.register("violet_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VIOLET_WOLF,  0x2D173F, 0xB56CFF, new Item.Properties()));

    public static final DeferredItem<Item> CHERRY_WOLF_SPAWN_EGG = ITEMS.register("cherry_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CHERRY_WOLF,  0xF4A7B9, 0xFFF0F5, new Item.Properties()));

    public static final DeferredItem<Item> ANGEL_WOLF_SPAWN_EGG = ITEMS.register("angel_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ANGEL_WOLF, 0xFFF8E1, 0xF5C542, new Item.Properties()));

    public static final DeferredItem<Item> END_WOLF_SPAWN_EGG = ITEMS.register("end_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.END_WOLF, 0x0A0612, 0x7A3CFF, new Item.Properties()));

    // ---------------------------- ELITE --------------------------------- //

    public static final DeferredItem<DeferredSpawnEggItem> BLOOD_WOLF_SPAWN_EGG = ITEMS.register("blood_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLOOD_WOLF, 0x2A0508, 0xB11226, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> LUNAR_WOLF_SPAWN_EGG = ITEMS.register("lunar_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.LUNAR_WOLF, 0x0B1020, 0x9FE8FF, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> SALVA_WOLF_SPAWN_EGG = ITEMS.register("salva_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SALVA_WOLF, 0x3A2414, 0xC58A16, new Item.Properties()));

    public static final DeferredItem<Item> VOID_WOLF_SPAWN_EGG = ITEMS.register("void_wolf_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.VOID_WOLF, 0x07040D, 0x7A4BCB, new Item.Properties()));




    public static void register( IEventBus eventBus ) {
        ITEMS.register(eventBus);
    }
}
