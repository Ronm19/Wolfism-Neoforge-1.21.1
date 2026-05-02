package com.ronm19.wolfism.entity;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.elemental.*;
import com.ronm19.wolfism.entity.custom.elite.BloodWolfEntity;
import com.ronm19.wolfism.entity.custom.elite.LunarWolfEntity;
import com.ronm19.wolfism.entity.custom.elite.VoidWolfEntity;
import com.ronm19.wolfism.entity.custom.neutral.ArcticWolfEntity;
import com.ronm19.wolfism.entity.custom.neutral.BlackWolfEntity;
import com.ronm19.wolfism.entity.custom.neutral.TimberWolfEntity;
import com.ronm19.wolfism.entity.custom.special.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, WolfismMod.MOD_ID);


    // ------------------------------------ MONSTERs --------------------------------- //


    // ------------------------------------ ELEMENTAL --------------------------------- //

    public static final Supplier<EntityType<FrostWolfEntity>> FROST_WOLF =
            ENTITY_TYPES.register("frost_wolf", () -> EntityType.Builder.of(FrostWolfEntity ::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.2F).clientTrackingRange(10).updateInterval(3).build("frost_wolf"));

    public static final Supplier<EntityType<FireWolfEntity>> FIRE_WOLF =
            ENTITY_TYPES.register("fire_wolf", () -> EntityType.Builder.of(FireWolfEntity ::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.2F).clientTrackingRange(10).updateInterval(3).build("fire_wolf"));

    public static final Supplier<EntityType<StormWolfEntity>> STORM_WOLF =
            ENTITY_TYPES.register("storm_wolf", () -> EntityType.Builder.of(StormWolfEntity ::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.2F).clientTrackingRange(10).updateInterval(3).build("storm_wolf"));

    public static final Supplier<EntityType<EarthWolfEntity>> EARTH_WOLF =
            ENTITY_TYPES.register("earth_wolf", () -> EntityType.Builder.of(EarthWolfEntity ::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.2F).clientTrackingRange(10).updateInterval(3).build("earth_wolf"));

    public static final Supplier<EntityType<WaterWolfEntity>> WATER_WOLF =
            ENTITY_TYPES.register("water_wolf", () -> EntityType.Builder.of(WaterWolfEntity ::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.2F).clientTrackingRange(10).updateInterval(3).build("water_wolf"));

    // ------------------------------------ NEUTRAL WOLFS --------------------------------- //

    public static final Supplier<EntityType<TimberWolfEntity>> TIMBER_WOLF =
            ENTITY_TYPES.register("timber_wolf", () -> EntityType.Builder.of(TimberWolfEntity ::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).updateInterval(3).build("timber_wolf"));

    public static final Supplier<EntityType<ArcticWolfEntity>> ARCTIC_WOLF =
            ENTITY_TYPES.register("arctic_wolf", () -> EntityType.Builder.of(ArcticWolfEntity ::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).updateInterval(3).build("arctic_wolf"));

    public static final Supplier<EntityType<BlackWolfEntity>> BLACK_WOLF =
            ENTITY_TYPES.register("black_wolf", () -> EntityType.Builder.of(BlackWolfEntity ::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).updateInterval(3).build("black_wolf"));

    // ------------------------------------ SPECIAL WOLFS --------------------------------- //

    public static final Supplier<EntityType<SpiritWolfEntity>> SPIRIT_WOLF =
            ENTITY_TYPES.register("spirit_wolf", () -> EntityType.Builder.of(SpiritWolfEntity ::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).updateInterval(3).build("spirit_wolf"));

    public static final Supplier<EntityType<ShadowWolfEntity>> SHADOW_WOLF =
            ENTITY_TYPES.register("shadow_wolf", () -> EntityType.Builder.of(ShadowWolfEntity ::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).updateInterval(3).build("shadow_wolf"));

    public static final Supplier<EntityType<GoldenWolfEntity>> GOLDEN_WOLF =
            ENTITY_TYPES.register("golden_wolf", () -> EntityType.Builder.of(GoldenWolfEntity ::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).updateInterval(3).build("golden_wolf"));

    public static final Supplier<EntityType<VioletWolfEntity>> VIOLET_WOLF =
            ENTITY_TYPES.register("violet_wolf", () -> EntityType.Builder.of(VioletWolfEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).build("violet_wolf"));

    public static final Supplier<EntityType<CherryWolfEntity>> CHERRY_WOLF =
            ENTITY_TYPES.register("cherry_wolf", () -> EntityType.Builder.of(CherryWolfEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).build("cherry_wolf"));

    public static final Supplier<EntityType<AngelWolfEntity>> ANGEL_WOLF =
            ENTITY_TYPES.register("angel_wolf", () -> EntityType.Builder.of(AngelWolfEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).build("angel_wolf"));

    public static final Supplier<EntityType<EndWolfEntity>> END_WOLF =
            ENTITY_TYPES.register("end_wolf", () -> EntityType.Builder.of(EndWolfEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.85F).build("end_wolf"));

    public static final Supplier<EntityType<WolfKingEntity>> WOLF_KING =
            ENTITY_TYPES.register("wolf_king", () -> EntityType.Builder.of(WolfKingEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).build("wolf_king"));

    // ------------------------------------ ELITE WOLFS --------------------------------- //

    public static final Supplier<EntityType<BloodWolfEntity>> BLOOD_WOLF =
            ENTITY_TYPES.register("blood_wolf", () -> EntityType.Builder.of(BloodWolfEntity ::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).updateInterval(3).build("blood_wolf"));

    public static final Supplier<EntityType<LunarWolfEntity>> LUNAR_WOLF =
            ENTITY_TYPES.register("lunar_wolf", () -> EntityType.Builder.of(LunarWolfEntity ::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).clientTrackingRange(10).updateInterval(3).build("lunar_wolf"));

    public static final Supplier<EntityType<SalvaWolfEntity>> SALVA_WOLF =
            ENTITY_TYPES.register("salva_wolf", () -> EntityType.Builder.of(SalvaWolfEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F).build("salva_wolf"));

    public static final Supplier<EntityType<VoidWolfEntity>> VOID_WOLF =
            ENTITY_TYPES.register("void_wolf", () -> EntityType.Builder.of(VoidWolfEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.85F).build("void_wolf"));

    // ------------------------------------ BOSSEs --------------------------------- //


    // ------------------------------------ MISC --------------------------------- //


    public static void register( IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
