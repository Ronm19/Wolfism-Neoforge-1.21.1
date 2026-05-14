package com.ronm19.wolfism.event;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.elemental.*;
import com.ronm19.wolfism.entity.custom.elite.BloodWolfEntity;
import com.ronm19.wolfism.entity.custom.elite.LunarWolfEntity;
import com.ronm19.wolfism.entity.custom.elite.VoidWolfEntity;
import com.ronm19.wolfism.entity.custom.elite.WolfKingEntity;
import com.ronm19.wolfism.entity.custom.neutral.ArcticWolfEntity;
import com.ronm19.wolfism.entity.custom.neutral.BlackWolfEntity;
import com.ronm19.wolfism.entity.custom.neutral.TimberWolfEntity;
import com.ronm19.wolfism.entity.custom.special.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = WolfismMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {

        // * --------------- NEUTRAL -------------------------- * //


        // * --------------- ELEMENTAL -------------------------- * //


    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {

        // * --------------- NATURAL -------------------------- * //

        event.put(ModEntities.TIMBER_WOLF.get(), TimberWolfEntity.createAttributes().build());
        event.put(ModEntities.ARCTIC_WOLF.get(), ArcticWolfEntity.createAttributes().build());
        event.put(ModEntities.BLACK_WOLF.get(), BlackWolfEntity.createAttributes().build());


        // * --------------- ELEMENTAL -------------------------- * //

        event.put(ModEntities.FROST_WOLF.get(), FrostWolfEntity.createAttributes().build());
        event.put(ModEntities.FIRE_WOLF.get(), FireWolfEntity.createAttributes().build());
        event.put(ModEntities.STORM_WOLF.get(), StormWolfEntity.createAttributes().build());
        event.put(ModEntities.EARTH_WOLF.get(), EarthWolfEntity.createAttributes().build());
        event.put(ModEntities.WATER_WOLF.get(), WaterWolfEntity.createAttributes().build());

        // * --------------- SPECIAL -------------------------- * //

        event.put(ModEntities.SPIRIT_WOLF.get(), SpiritWolfEntity.createAttributes().build());
        event.put(ModEntities.SHADOW_WOLF.get(), ShadowWolfEntity.createAttributes().build());
        event.put(ModEntities.GOLDEN_WOLF.get(), GoldenWolfEntity.createAttributes().build());
        event.put(ModEntities.VIOLET_WOLF.get(), VioletWolfEntity.createAttributes().build());
        event.put(ModEntities.CHERRY_WOLF.get(), CherryWolfEntity.createAttributes().build());
        event.put(ModEntities.ANGEL_WOLF.get(), AngelWolfEntity.createAttributes().build());
        event.put(ModEntities.END_WOLF.get(), EndWolfEntity.createAttributes().build());
        event.put(ModEntities.WOLF_KING.get(), WolfKingEntity.createAttributes().build());

        // * --------------- ELITE -------------------------- * //

        event.put(ModEntities.BLOOD_WOLF.get(), BloodWolfEntity.createAttributes().build());
        event.put(ModEntities.LUNAR_WOLF.get(), LunarWolfEntity.createAttributes().build());
        event.put(ModEntities.SALVA_WOLF.get(), SalvaWolfEntity.createAttributes().build());
        event.put(ModEntities.VOID_WOLF.get(), VoidWolfEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {

        // ------------------------------------ ELITE --------------------------------- //

        event.register(
                ModEntities.BLOOD_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BloodWolfEntity ::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.LUNAR_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                LunarWolfEntity ::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.SALVA_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SalvaWolfEntity ::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.VOID_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                VoidWolfEntity ::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        // ------------------------------------ SPECIAL --------------------------------- //

        event.register(
                ModEntities.SPIRIT_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SpiritWolfEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.SHADOW_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ShadowWolfEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.GOLDEN_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                GoldenWolfEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.VIOLET_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                VioletWolfEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.CHERRY_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CherryWolfEntity ::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.ANGEL_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AngelWolfEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.END_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EndWolfEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.WOLF_KING.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WolfKingEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );


        // ------------------------------------ NATURAL --------------------------------- //

        event.register(
                ModEntities.TIMBER_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                TimberWolfEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.ARCTIC_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ArcticWolfEntity ::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        event.register(
                ModEntities.BLACK_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlackWolfEntity ::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );

        // ------------------------------------ ELEMENTAL --------------------------------- //

        event.register(ModEntities.FROST_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                FrostWolfEntity::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.FIRE_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                FireWolfEntity::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.STORM_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                StormWolfEntity ::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(ModEntities.EARTH_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                EarthWolfEntity ::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);

        event.register(
                ModEntities.WATER_WOLF.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (entityType, level, reason, pos, random) -> {

                    boolean validGround = level.getBlockState(pos.below())
                            .isValidSpawn(level, pos.below(), entityType);

                    boolean notInWater = level.getFluidState(pos).isEmpty();

                    boolean nearWater = isNearWater(level, pos);

                    return validGround && notInWater && nearWater;
                },
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    private static boolean isNearWater( LevelAccessor level, BlockPos pos) {
        for (BlockPos check : BlockPos.betweenClosed(
                pos.offset(-5, -2, -5),
                pos.offset(5, 2, 5))) {

            if (!level.getFluidState(check).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}