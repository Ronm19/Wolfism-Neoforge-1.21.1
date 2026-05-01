package com.ronm19.wolfism.datagen;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.block.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider( PackOutput output, ExistingFileHelper exFileHelper ) {
        super(output, WolfismMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.PRIMAL_BONE_BLOCK);
    }


    private void blockWithItem( DeferredBlock<Block> deferredBlock ) {
        simpleBlockWithItem(deferredBlock.get(), cubeAll(deferredBlock.get()));
    }

    private void simpleGrassLikeBlock(Block block, ResourceLocation top, ResourceLocation side, ResourceLocation bottom) {
        // Generate the cube model using top, side, and bottom textures
        var model = models().cubeBottomTop(blockName(block), side, bottom, top);

        // Register the blockstate to use that model
        simpleBlock(block, model);

        // Also create an item model that points to the same block model
        simpleBlockItem(block, model);
    }


    private void leavesBlock( DeferredBlock<Block> deferredBlock ) {
        simpleBlockWithItem(deferredBlock.get(),
                models().singleTexture(BuiltInRegistries.BLOCK.getKey(deferredBlock.get()).getPath(), ResourceLocation.parse("minecraft:block/leaves"),
                        "all", blockTexture(deferredBlock.get())).renderType("cutout"));
    }

    private void saplingBlock( DeferredBlock<Block> deferredBlock ) {
        simpleBlock(deferredBlock.get(), models().cross(BuiltInRegistries.BLOCK.getKey(deferredBlock.get()).getPath(), blockTexture(deferredBlock.get())).renderType("cutout"));
    }

    private void blockItem( DeferredBlock<Block> deferredBlock ) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("wolfism:block/" + deferredBlock.getId().getPath()));
    }

    private void blockItem( DeferredBlock<Block> deferredBlock, String appendix ) {
        simpleBlockItem(deferredBlock.get(), new ModelFile.UncheckedModelFile("wolfism:block/" + deferredBlock.getId().getPath() + appendix));
    }

    private void customLamp( DeferredBlock<? extends Block> lampBlock ) {
        // Get the block's registry name (like "solar_lamp")
        String blockName = Objects.requireNonNull(lampBlock.getId()).getPath();

        // Build blockstate models depending on CLICKED
        getVariantBuilder(lampBlock.get()).forAllStates(state -> {
            boolean isOn = state.getValue(RedstoneLampBlock.LIT);
            String textureName = blockName + (isOn ? "_turned_on" : "_turned_off");

            return new ConfiguredModel[]{
                    new ConfiguredModel(models().cubeAll(textureName,
                            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "block/" + textureName)))
            };
        });

        // Item model defaults to the "on" texture
        String itemTexture = blockName + "_turned_on";
        simpleBlockItem(lampBlock.get(), models().cubeAll(itemTexture,
                ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "block/" + itemTexture)));
    }

    public void makeCrop( CropBlock block, IntegerProperty ageProperty, String modelName, String textureName ) {
        Function<BlockState, ConfiguredModel[]> function = state -> {
            int age = state.getValue(ageProperty);
            return new ConfiguredModel[]{
                    new ConfiguredModel(models().crop(modelName + age,
                                    ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "block/" + textureName + age))
                            .renderType("cutout"))
            };
        };

        getVariantBuilder(block).forAllStates(function);
    }

    private String blockName(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    public @NotNull ResourceLocation blockTexture( Block block ) {
        return modLoc("block/" + blockName(block));
    }
}