package com.ronm19.wolfism;

import com.mojang.logging.LogUtils;
import com.ronm19.wolfism.block.ModBlocks;
import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.client.renderer.*;
import com.ronm19.wolfism.item.ModCreativeModeTabs;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(WolfismMod.MOD_ID)
public class WolfismMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "wolfism";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public WolfismMod( IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModEntities.register(modEventBus);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = WolfismMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    static class ClientModEvents {
        @SubscribeEvent
        static void onClientSetup(FMLClientSetupEvent event) {

            // ---------------------- ELEMENTAL ----------------------- //

            EntityRenderers.register(ModEntities.FROST_WOLF.get(), FrostWolfRenderer::new);
            EntityRenderers.register(ModEntities.FIRE_WOLF.get(), FireWolfRenderer ::new);
            EntityRenderers.register(ModEntities.STORM_WOLF.get(), StormWolfRenderer ::new);
            EntityRenderers.register(ModEntities.EARTH_WOLF.get(), EarthWolfRenderer ::new);
            EntityRenderers.register(ModEntities.WATER_WOLF.get(), WaterWolfRenderer ::new);

            // ---------------------- NEUTRAL ----------------------- //

            EntityRenderers.register(ModEntities.TIMBER_WOLF.get(), TimberWolfRenderer::new);
            EntityRenderers.register(ModEntities.ARCTIC_WOLF.get(), ArcticWolfRenderer::new);
            EntityRenderers.register(ModEntities.BLACK_WOLF.get(), BlackWolfRenderer::new);

            // ---------------------- SPECIAL ----------------------- //

            EntityRenderers.register(ModEntities.SPIRIT_WOLF.get(), SpiritWolfRenderer::new);
            EntityRenderers.register(ModEntities.SHADOW_WOLF.get(), ShadowWolfRenderer::new);
            EntityRenderers.register(ModEntities.GOLDEN_WOLF.get(), GoldenWolfRenderer::new);
            EntityRenderers.register(ModEntities.VIOLET_WOLF.get(), VioletWolfRenderer::new);
            EntityRenderers.register(ModEntities.CHERRY_WOLF.get(), CherryWolfRenderer::new);
            EntityRenderers.register(ModEntities.ANGEL_WOLF.get(), AngelWolfRenderer::new);
            EntityRenderers.register(ModEntities.ZOMBIE_WOLF.get(), ZombieWolfRenderer::new);
            EntityRenderers.register(ModEntities.HUSK_WOLF.get(), HuskWolfRenderer::new);
            EntityRenderers.register(ModEntities.SKELETON_WOLF.get(), SkeletonWolfRenderer::new);
            EntityRenderers.register(ModEntities.DROWNED_WOLF.get(), DrownedWolfRenderer::new);
            EntityRenderers.register(ModEntities.CRYSTAL_WOLF.get(), CrystalWolfRenderer::new);
            EntityRenderers.register(ModEntities.END_WOLF.get(), EndWolfRenderer::new);


            // ---------------------- ELITE ----------------------- //

            EntityRenderers.register(ModEntities.BLOOD_WOLF.get(), BloodWolfRenderer::new);
            EntityRenderers.register(ModEntities.LUNAR_WOLF.get(), LunarWolfRenderer::new);
            EntityRenderers.register(ModEntities.GRIM_WOLF.get(), GrimWolfRenderer::new);
            EntityRenderers.register(ModEntities.WITHERED_WOLF.get(), WitheredWolfRenderer::new);
            EntityRenderers.register(ModEntities.SALVA_WOLF.get(), SalvaWolfRenderer::new);
            EntityRenderers.register(ModEntities.VOID_WOLF.get(), VoidWolfRenderer::new);
            EntityRenderers.register(ModEntities.WOLF_KING.get(), WolfKingRenderer::new);
        }
    }
}
