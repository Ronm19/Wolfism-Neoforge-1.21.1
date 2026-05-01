package com.ronm19.wolfism.datagen;

import com.ronm19.wolfism.WolfismMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider  extends BlockTagsProvider {
    public ModBlockTagProvider( PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper ) {
        super(output, lookupProvider, WolfismMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags( HolderLookup.@NotNull Provider provider ) {
        this.tag(BlockTags.LOGS_THAT_BURN);

        this.tag(BlockTags.DIRT);

    }
}
