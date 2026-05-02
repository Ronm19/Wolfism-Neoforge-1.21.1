package com.ronm19.wolfism.datagen;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;

public class ModItemModelProvider extends ItemModelProvider {
    private static LinkedHashMap<ResourceKey<TrimMaterial>, Float> trimMaterials = new LinkedHashMap<>();
    static {
        trimMaterials.put(TrimMaterials.QUARTZ, 0.1F);
        trimMaterials.put(TrimMaterials.IRON, 0.2F);
        trimMaterials.put(TrimMaterials.NETHERITE, 0.3F);
        trimMaterials.put(TrimMaterials.REDSTONE, 0.4F);
        trimMaterials.put(TrimMaterials.COPPER, 0.5F);
        trimMaterials.put(TrimMaterials.GOLD, 0.6F);
        trimMaterials.put(TrimMaterials.EMERALD, 0.7F);
        trimMaterials.put(TrimMaterials.DIAMOND, 0.8F);
        trimMaterials.put(TrimMaterials.LAPIS, 0.9F);
        trimMaterials.put(TrimMaterials.AMETHYST, 1.0F);
    }


    public ModItemModelProvider( PackOutput output, ExistingFileHelper existingFileHelper ) {
        super(output, WolfismMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        basicItem(ModItems.PRIMAL_BONE.get());
        basicItem(ModItems.SHADOW_BONE.get());
        basicItem(ModItems.EMBER_BONE.get());
        basicItem(ModItems.SPIRIT_BONE.get());
        basicItem(ModItems.FROST_BONE.get());
        basicItem(ModItems.CHARGED_BONE.get());
        basicItem(ModItems.GOLDEN_BONE.get());
        basicItem(ModItems.ARCANE_BONE.get());
        basicItem(ModItems.ANGEL_BONE.get());
        basicItem(ModItems.LUNAR_BONE.get());
        basicItem(ModItems.BLOOD_BONE.get());
        basicItem(ModItems.ROYAL_BONE.get());
        basicItem(ModItems.VOID_BONE.get());

        basicItem(ModItems.BLOOD_GEM.get());


        // -------------------------- ELITE ----------------------------- //

        withExistingParent(ModItems.BLOOD_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.LUNAR_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.SALVA_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.VOID_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));

        // -------------------------- SPECIAL ----------------------------- //

        withExistingParent(ModItems.SPIRIT_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.SHADOW_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.GOLDEN_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.VIOLET_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.CHERRY_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.ANGEL_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.END_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));


        // -------------------------- ELEMENTAL ----------------------------- //

        withExistingParent(ModItems.FROST_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.FIRE_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.STORM_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.EARTH_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.WATER_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));

        // -------------------------- NEUTRAL -------------------------------- //

        withExistingParent(ModItems.TIMBER_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.ARCTIC_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
        withExistingParent(ModItems.BLACK_WOLF_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));


    }

    public void flowerItem( DeferredBlock<Block> block) {
        this.withExistingParent(block.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0",  ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID,
                        "block/" + block.getId().getPath()));
    }

    private void saplingItem( DeferredBlock<Block> item) {
        withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "block/" + item.getId().getPath()));
    }

    public void buttonItem( DeferredBlock<Block> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/button_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void fenceItem(DeferredBlock<Block> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/fence_inventory"))
                .texture("texture",  ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    public void wallItem(DeferredBlock<Block> block, DeferredBlock<Block> baseBlock) {
        this.withExistingParent(block.getId().getPath(), mcLoc("block/wall_inventory"))
                .texture("wall",  ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID,
                        "block/" + baseBlock.getId().getPath()));
    }

    private ItemModelBuilder handheldItem( DeferredItem<Item> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/handheld")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID,"item/" + item.getId().getPath()));
    }
}
