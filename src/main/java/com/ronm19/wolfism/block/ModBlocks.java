package com.ronm19.wolfism.block;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(WolfismMod.MOD_ID);


    public static final DeferredBlock<Block> PRIMAL_BONE_BLOCK = registerBlock("primal_bone_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BONE_BLOCK).sound(SoundType.BONE_BLOCK)));

    private static <T extends Block> DeferredBlock<T> registerBlock( String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;

    }

    private static <T extends Block> void registerBlockItem( String name, DeferredBlock<T> block ) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register( IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
