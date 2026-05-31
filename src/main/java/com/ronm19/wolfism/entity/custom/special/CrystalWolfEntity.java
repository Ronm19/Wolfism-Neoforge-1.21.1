package com.ronm19.wolfism.entity.custom.special;

// keep your real package line here

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

public class CrystalWolfEntity extends WolfismWolfEntity {

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    public CrystalWolfEntity(EntityType<? extends CrystalWolfEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WolfismWolfEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.12D);
    }

    protected void defineSynchedData( SynchedEntityData.Builder builder ) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.MAGENTA.getId());
    }


    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame() && isTamingItem(stack)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // 33% tame chance
                if (this.getRandom().nextInt(3) == 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7); // hearts
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6); // smoke
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private boolean isTamingItem(ItemStack stack) {
        return stack.is(Items.AMETHYST_SHARD);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isTamingItem(stack);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean didHit = super.doHurtTarget(target);

        if (didHit && target instanceof LivingEntity livingTarget) {
            applyCrystalBite(livingTarget);
        }

        return didHit;
    }

    private void applyCrystalBite(LivingEntity target) {
        RandomSource random = this.getRandom();

        // 15% chance: Crystallizing slow
        if (random.nextFloat() < 0.15F) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Crystal Guard: tamed Crystal Wolf protects nearby owner.
        if (!this.level().isClientSide
                && this.isTame()
                && !this.isOrderedToSit()
                && this.tickCount % 400 == 0) {

            LivingEntity owner = this.getOwner();

            if (owner != null && this.distanceTo(owner) <= 8.0F) {
                owner.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        100,
                        0,
                        true,
                        false,
                        true
                ), this);
            }
        }
    }

    public boolean shouldEyesGlow() {
        if (this.isInvisible()) {
            return false;
        }

        long time = this.level().getDayTime() % 24000L;
        boolean isNight = time >= 13000L && time <= 23000L;
        boolean darkEnough = this.level().getRawBrightness(this.blockPosition(), 0) <= 7;

        return isNight || darkEnough;
    }

    @javax.annotation.Nullable
    public CrystalWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        CrystalWolfEntity wolf = (CrystalWolfEntity) ModEntities.CRYSTAL_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof CrystalWolfEntity wolf1) {
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
            EntityType<CrystalWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }

        return level.getLevel().dimension() == Level.OVERWORLD;
    }

    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(CrystalWolfEntity.class, EntityDataSerializers.INT);
    }
}