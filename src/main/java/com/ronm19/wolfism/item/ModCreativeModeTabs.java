package com.ronm19.wolfism.item;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WolfismMod.MOD_ID);

    public static final Supplier<CreativeModeTab> WOLFISM_ITEMS =
            CREATIVE_MODE_TABS.register("wolfism_items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wolfism.wolfism_items"))
                    .icon(() -> new ItemStack(ModItems.PRIMAL_BONE.get()))
                    .displayItems((pParameters, pOutput) -> {

                        pOutput.accept(ModItems.PRIMAL_BONE);
                        pOutput.accept(ModItems.EMBER_BONE);
                        pOutput.accept(ModItems.FROST_BONE);
                        pOutput.accept(ModItems.CHARGED_BONE);
                        pOutput.accept(ModItems.SHADOW_BONE);
                        pOutput.accept(ModItems.SPIRIT_BONE);
                        pOutput.accept(ModItems.GOLDEN_BONE);
                        pOutput.accept(ModItems.ARCANE_BONE);
                        pOutput.accept(ModItems.ANGEL_BONE);
                        pOutput.accept(ModItems.BLOOD_BONE);
                        pOutput.accept(ModItems.LUNAR_BONE);
                        pOutput.accept(ModItems.ROYAL_BONE);
                        pOutput.accept(ModItems.VOID_BONE);
                        pOutput.accept(ModItems.BLOOD_GEM);

                    }).build());

    public static final Supplier<CreativeModeTab> WOLFISM_BLOCKS =
            CREATIVE_MODE_TABS.register("wolfism_blocks", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wolfism.wolfism_blocks"))
                    .icon(() -> new ItemStack(ModItems.PRIMAL_BONE.get()))
                    .icon(() -> new ItemStack(ModBlocks.PRIMAL_BONE_BLOCK.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "wolfism_items"))
                    .displayItems((pParameters, pOutput) -> {

                        pOutput.accept(ModBlocks.PRIMAL_BONE_BLOCK);

                    }).build());

    public static final Supplier<CreativeModeTab> WOLFISM_ENTITIES =
            CREATIVE_MODE_TABS.register("wolfism_entities", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wolfism.wolfism_entities"))
                    .icon(() -> new ItemStack(ModItems.FROST_WOLF_SPAWN_EGG.get()))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "wolfism_blocks"))
                    .displayItems((pParameters, pOutput) -> {

                        // ------------------------------------ ELEMENTAL --------------------------------- //

                        pOutput.accept(ModItems.FROST_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.FIRE_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.STORM_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.EARTH_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.WATER_WOLF_SPAWN_EGG);

                        // ------------------------------------ NEUTRAL --------------------------------- //

                        pOutput.accept(ModItems.TIMBER_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.ARCTIC_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.BLACK_WOLF_SPAWN_EGG);

                        // ------------------------------------ SPECIAL --------------------------------- //

                        pOutput.accept(ModItems.SPIRIT_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.SHADOW_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.GOLDEN_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.VIOLET_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.CHERRY_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.ANGEL_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.END_WOLF_SPAWN_EGG);


                        // ------------------------------------ ELITE --------------------------------- //

                        pOutput.accept(ModItems.BLOOD_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.LUNAR_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.SALVA_WOLF_SPAWN_EGG);
                        pOutput.accept(ModItems.VOID_WOLF_SPAWN_EGG);


                    }).build());




    public static void register( IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}