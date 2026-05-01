package com.ronm19.wolfism.entity.custom.neutral;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.elite.BloodWolfEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ArcticWolfEntity extends Wolf {

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    public ArcticWolfEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
    }

    // =========================
    // ATTRIBUTES
    // =========================
    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D) // slightly tankier
                .add(Attributes.ATTACK_DAMAGE, 4.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D);
    }

    protected void defineSynchedData( SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.CYAN.getId());
    }

    // =========================
    // FREEZE IMMUNITY
    // =========================
    @Override
    public boolean canFreeze() {
        return false;
    }

    // Optional extra safety
    @Override
    public boolean isFreezing() {
        return false;
    }

    // =========================
    // TICK (COLD BIOME BUFF)
    // =========================
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (isInColdBiome()) {
                applyColdBuffs();
            }
        }
    }

    private boolean isInColdBiome() {
        return this.level().getBiome(this.blockPosition()).is(net.neoforged.neoforge.common.Tags.Biomes.IS_COLD_OVERWORLD);
    }

    private void applyColdBuffs() {
        // subtle, not OP
        this.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED, 40, 0, false, false
        ));

        this.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false
        ));
    }

    // =========================
    // TAMING / FOOD
    // =========================
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SALMON) || stack.is(Items.COD);
    }


    public @NotNull DyeColor getCollarColor() {
        return DyeColor.byId((Integer)this.entityData.get(DATA_COLLAR_COLOR));
    }

    public boolean hasArmor() {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR);
    }

    private void setCollarColor(DyeColor collarColor) {
        this.entityData.set(DATA_COLLAR_COLOR, collarColor.getId());
    }

    // =========================
    // BREEDING
    // =========================


    @javax.annotation.Nullable
    public ArcticWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        ArcticWolfEntity wolf = (ArcticWolfEntity) ModEntities.ARCTIC_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof ArcticWolfEntity wolf1) {
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

    // =========================
    // OPTIONAL: POWDER SNOW SAFETY
    // =========================
    @Override
    public boolean isSensitiveToWater() {
        return false; // avoids weird slowdowns in snow/wet
    }

    public void addAdditionalSaveData( CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("CollarColor", (byte)this.getCollarColor().getId());
        this.addPersistentAngerSaveData(compound);
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(compound.getInt("CollarColor")));
        }

        this.readPersistentAngerSaveData(this.level(), compound);
    }
    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(ArcticWolfEntity.class, EntityDataSerializers.INT);
    }
}