package com.ronm19.wolfism.datagen;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.block.ModBlocks;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider( PackOutput output, CompletableFuture<HolderLookup.Provider> registries ) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes( @NotNull RecipeOutput pRecipeOutput ) {

        // ------------------------ BLOCKS --------------------------------------



        // ------------------------ TOOLS/ITEM & ARMORS --------------------------

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PRIMAL_BONE.get())
                .pattern(" B ")
                .pattern("BGB")
                .pattern(" B ")
                .define('B', Items.BONE)
                .define('G', Items.GOLD_NUGGET)
                .unlockedBy("has_bone", has(Items.BONE))
                .save(pRecipeOutput);

        // Ember Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.EMBER_BONE.get())
                .pattern(" B ")
                .pattern("BPB")
                .pattern(" B ")
                .define('P', ModItems.PRIMAL_BONE.get())
                .define('B', Items.BLAZE_POWDER)
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .unlockedBy("has_blaze_powder", has(Items.BLAZE_POWDER))
                .save(pRecipeOutput);

        // Frost Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FROST_BONE.get())
                .pattern(" I ")
                .pattern("IPI")
                .pattern(" I ")
                .define('P', ModItems.PRIMAL_BONE.get())
                .define('I', Blocks.PACKED_ICE)
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .unlockedBy("has_packed_ice", has(Blocks.PACKED_ICE))
                .save(pRecipeOutput);

        // Charged Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHARGED_BONE.get())
                .pattern("ILI")
                .pattern("PRP")
                .pattern("IPI")
                .define('I', Items.IRON_INGOT)
                .define('L', Items.LIGHTNING_ROD)
                .define('P', ModItems.PRIMAL_BONE.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .save(pRecipeOutput);


        // Shadow Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SHADOW_BONE.get())
                .pattern(" M ")
                .pattern("MPM")
                .pattern(" M ")
                .define('P', ModItems.PRIMAL_BONE.get())
                .define('M', Items.PHANTOM_MEMBRANE)
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .unlockedBy("has_phantom_membrane", has(Items.PHANTOM_MEMBRANE))
                .save(pRecipeOutput);

        // Spirit Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SPIRIT_BONE.get())
                .pattern(" G ")
                .pattern("GPG")
                .pattern(" G ")
                .define('P', ModItems.PRIMAL_BONE.get())
                .define('G', Items.GHAST_TEAR)
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .unlockedBy("has_ghast_tear", has(Items.GHAST_TEAR))
                .save(pRecipeOutput);

        // Golden Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GOLDEN_BONE.get())
                .pattern(" G ")
                .pattern("GPG")
                .pattern(" G ")
                .define('G', Items.GOLD_INGOT)
                .define('P', ModItems.PRIMAL_BONE.get())
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .save(pRecipeOutput);

        // Arcane Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ARCANE_BONE.get())
                .pattern(" A ")
                .pattern("ABA")
                .pattern(" A ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', ModItems.PRIMAL_BONE.get())
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .save(pRecipeOutput);

        // Angel Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ANGEL_BONE.get())
                .pattern(" F ")
                .pattern("GBG")
                .pattern(" F ")
                .define('F', ItemTags.FLOWERS)
                .define('G', Items.GOLD_INGOT)
                .define('B', ModItems.PRIMAL_BONE.get())
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .save(pRecipeOutput);

        // Lunar Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LUNAR_BONE.get())
                .pattern(" G ")
                .pattern("APA")
                .pattern(" G ")
                .define('G', Items.GLOWSTONE_DUST)
                .define('A', Items.AMETHYST_SHARD)
                .define('P', ModItems.PRIMAL_BONE.get())
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .save(pRecipeOutput);

        // Blood Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLOOD_BONE.get())
                .pattern(" G ")
                .pattern("GPG")
                .pattern(" G ")
                .define('G', ModItems.BLOOD_GEM.get())
                .define('P', ModItems.PRIMAL_BONE.get())
                .unlockedBy("has_blood_gem", has(ModItems.BLOOD_GEM.get()))
                .save(pRecipeOutput);

        // Royal Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_BONE.get())
                .pattern(" G ")
                .pattern("GPG")
                .pattern(" A ")
                .define('G', Items.GOLD_INGOT)
                .define('P', ModItems.PRIMAL_BONE.get())
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .save(pRecipeOutput);

        // Void Bone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.VOID_BONE.get())
                .pattern("ESE")
                .pattern("OPO")
                .pattern("EAE")
                .define('E', Items.ENDER_PEARL)
                .define('S', Items.ECHO_SHARD)
                .define('O', Items.OBSIDIAN)
                .define('P', ModItems.PRIMAL_BONE.get())
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy("has_primal_bone", has(ModItems.PRIMAL_BONE.get()))
                .save(pRecipeOutput);


        // Blood gem
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLOOD_GEM.get())
                .pattern(" R ")
                .pattern("RBR")
                .pattern(" R ")
                .define('R', Items.REDSTONE)
                .define('B', Items.BONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(pRecipeOutput);

        // ---------------------- STORAGE / REVERSIBLE ------------------- //

        nineBlockStorageRecipes(pRecipeOutput, RecipeCategory.MISC, ModItems.PRIMAL_BONE.get(), RecipeCategory.BUILDING_BLOCKS, ModBlocks.PRIMAL_BONE_BLOCK.get());


        // -------------- Shapeless Recipes ---------------------- //



        // ------------------------ BLOCKS --------------------------------------


        // ------------------------ ITEMS --------------------------------------


    }

    protected static void oreSmelting( RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                       float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe ::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting( RecipeOutput pRecipeOutput, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult,
                                       float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pRecipeOutput, RecipeSerializer.BLASTING_RECIPE, BlastingRecipe ::new, pIngredients, pCategory, pResult,
                pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static <T extends AbstractCookingRecipe> void oreCooking( RecipeOutput pRecipeOutput, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> factory,
                                                                        List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer, factory).group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(pRecipeOutput, WolfismMod.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }

    protected static void planksFromLog( RecipeOutput output, ItemLike planks, ItemLike log ) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, planks, 4)
                .requires(log)
                .unlockedBy(getHasName(log), has(log))
                .save(output);
    }

    protected static void planksFromLogs( RecipeOutput output, ItemLike planks, Block logs ) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, planks, 4)
                .requires(logs)
                .unlockedBy("has_logs", has(logs))
                .save(output);
    }

    protected static void woodFromLogs( RecipeOutput output, ItemLike wood, ItemLike log ) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, wood, 3)
                .pattern("##")
                .pattern("##")
                .define('#', log)
                .unlockedBy(getHasName(log), has(log))
                .save(output);
    }
}

