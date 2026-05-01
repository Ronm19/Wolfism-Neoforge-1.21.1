package com.ronm19.wolfism.entity.custom.neutral;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class TimberWolfEntity extends Wolf {

    private static final ResourceLocation SPEED_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "timber_woodland_speed");

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(TimberWolfEntity.class, EntityDataSerializers.INT);


    private static final AttributeModifier SPEED_BONUS = new AttributeModifier(SPEED_BONUS_ID, 0.03D, AttributeModifier.Operation.ADD_VALUE);

    public TimberWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.ORANGE.getId());

    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.targetSelector.addGoal(7, new NonTameRandomTargetGoal<>(this, Chicken.class, false, livingEntity -> true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            this.updateSpeedBonus();
        }
    }

    private void updateSpeedBonus() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        boolean shouldHave = this.isWoodlandBiome() && (this.getTarget() != null || this.isCatchingUpToOwner());
        boolean hasBonus = speed.getModifier(SPEED_BONUS_ID) != null;

        if (shouldHave && !hasBonus) {
            speed.addTransientModifier(SPEED_BONUS);
        } else if (!shouldHave && hasBonus) {
            speed.removeModifier(SPEED_BONUS_ID);
        }
    }

    private boolean isCatchingUpToOwner() {
        if (!this.isTame() || this.isOrderedToSit()) return false;

        LivingEntity owner = this.getOwner();
        return owner != null && this.distanceToSqr(owner) > 36.0D;
    }

    private boolean isWoodlandBiome() {
        Holder<Biome> biome = this.level().getBiome(this.blockPosition());

        return biome.is(Biomes.TAIGA)
                || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA)
                || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
                || biome.is(Biomes.SNOWY_TAIGA)
                || biome.is(Biomes.FOREST)
                || biome.is(Biomes.BIRCH_FOREST)
                || biome.is(Biomes.GROVE);
    }

    @javax.annotation.Nullable
    public TimberWolfEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        TimberWolfEntity wolf = (TimberWolfEntity) ModEntities.TIMBER_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof TimberWolfEntity wolf1) {
            if (this.random.nextBoolean()) {
                wolf.setVariant(this.getVariant());
            } else {
                wolf.setVariant(wolf1.getVariant());
            }

            if (this.isTame()) {
                wolf.setOwnerUUID(this.getOwnerUUID());
                wolf.setTame(true, true);
                if (this.random.nextBoolean()) {
                    wolf.setCollarColor(this.getCollarColor());
                } else {
                    wolf.setCollarColor(wolf1.getCollarColor());
                }
            }
        }

        return wolf;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("CollarColor", (byte) this.getCollarColor().getId());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(compound.getInt("CollarColor")));
        }

        this.readPersistentAngerSaveData(this.level(), compound);
    }

    public @NotNull DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public boolean hasArmor() {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR);
    }

    private void setCollarColor(DyeColor collarColor) {
        this.entityData.set(DATA_COLLAR_COLOR, collarColor.getId());
    }

    public static boolean canSpawn(
            EntityType<TimberWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }
}