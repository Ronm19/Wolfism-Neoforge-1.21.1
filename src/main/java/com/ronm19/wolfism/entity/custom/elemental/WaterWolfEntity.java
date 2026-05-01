package com.ronm19.wolfism.entity.custom.elemental;

import com.ronm19.wolfism.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class WaterWolfEntity extends Wolf {

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR =
            SynchedEntityData.defineId(WaterWolfEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> NEAR_WATER =
            SynchedEntityData.defineId(WaterWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private static final Ingredient TEMPT_ITEMS = Ingredient.of(
            Items.COD,
            Items.SALMON,
            Items.TROPICAL_FISH,
            Items.PUFFERFISH,
            Items.BONE
    );

    private int waterCheckCooldown = 0;
    private int glowCooldown = 0;
    private int ownerUnderwaterTicks = 0;

    public WaterWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NEAR_WATER, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.BLUE.getId());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15D, TEMPT_ITEMS, false));
        this.goalSelector.addGoal(8, new SeekWaterGoal(this, 1.0D));
    }

    public boolean isNearWater() {
        return this.entityData.get(NEAR_WATER);
    }

    private void setNearWater(boolean value) {
        this.entityData.set(NEAR_WATER, value);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("CollarColor", (byte) this.getWaterWolfCollarColor().getId());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CollarColor", 99)) {
            this.setWaterWolfCollarColor(DyeColor.byId(compound.getInt("CollarColor")));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            updateNearWaterState();

            if (this.isInWaterOrBubble()) {
                this.setAirSupply(this.getMaxAirSupply());
                refreshSelfAquaticEffects();
            }

            handleOwnerWaterSupport();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isInWaterOrBubble()) {
            Vec3 movement = this.getDeltaMovement();
            double speedBoost = this.getTarget() != null ? 1.12D : 1.06D;

            if (movement.horizontalDistanceSqr() > 0.0004D || this.getTarget() != null) {
                this.setDeltaMovement(movement.x * speedBoost, movement.y + 0.01D, movement.z * speedBoost);
            }

            applyOwnerWaterFollowBoost();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);

        if (hurt && target instanceof LivingEntity living) {
            if (this.isInWaterOrBubble() || target.isInWaterOrBubble() || this.isNearWater()) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
            }
        }

        return hurt;
    }

    private void updateNearWaterState() {
        if (--this.waterCheckCooldown <= 0) {
            this.waterCheckCooldown = 10;

            boolean inWater = this.isInWaterOrBubble();
            boolean nearWater = inWater || this.checkNearbyWater() || this.isBeingRainedOn();

            if (nearWater) {
                this.setNearWater(true);
                this.glowCooldown = 40;
            } else {
                if (this.glowCooldown > 0) {
                    this.glowCooldown--;
                } else {
                    this.setNearWater(false);
                }
            }
        }
    }

    private void refreshSelfAquaticEffects() {
        refreshEffect(this, MobEffects.WATER_BREATHING, 10, 0);
        refreshEffect(this, MobEffects.NIGHT_VISION, 220, 0);
    }

    private void handleOwnerWaterSupport() {
        if (!this.isTame() || this.isOrderedToSit()) {
            this.ownerUnderwaterTicks = 0;
            return;
        }

        if (!(this.getOwner() instanceof LivingEntity owner)) {
            this.ownerUnderwaterTicks = 0;
            return;
        }

        if (!owner.isAlive()) {
            this.ownerUnderwaterTicks = 0;
            return;
        }

        if (this.distanceToSqr(owner) > 64.0D) {
            this.ownerUnderwaterTicks = 0;
            return;
        }

        if (!(this.isNearWater() || this.isInWaterOrBubble() || owner.isInWaterOrBubble())) {
            this.ownerUnderwaterTicks = 0;
            return;
        }

        boolean ownerInWater = owner.isInWaterOrBubble();
        boolean ownerUnderwater = owner.isEyeInFluid(FluidTags.WATER);

        if (ownerInWater) {
            refreshEffect(owner, MobEffects.DOLPHINS_GRACE, 10, 0);
        }

        if (ownerUnderwater) {
            this.ownerUnderwaterTicks++;
            if (this.ownerUnderwaterTicks >= 40) {
                refreshEffect(owner, MobEffects.WATER_BREATHING, 10, 0);
            }
        } else {
            this.ownerUnderwaterTicks = 0;
        }
    }

    private void refreshEffect(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance current = entity.getEffect(effect);
        if (current == null || current.getDuration() <= 4 || current.getAmplifier() < amplifier) {
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false));
        }
    }

    private boolean checkNearbyWater() {
        BlockPos pos = this.blockPosition();

        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            BlockState state = this.level().getBlockState(checkPos);
            if (state.getFluidState().is(FluidTags.WATER)) {
                return true;
            }
        }

        return false;
    }

    private boolean isBeingRainedOn() {
        BlockPos pos = this.blockPosition();
        return this.level().isRainingAt(pos) || this.level().isRainingAt(pos.above());
    }

    private void applyOwnerWaterFollowBoost() {
        if (!this.isTame()) return;
        if (this.isOrderedToSit()) return;
        if (!(this.getOwner() instanceof LivingEntity owner)) return;
        if (!owner.isInWaterOrBubble()) return;

        double distanceSqr = this.distanceToSqr(owner);
        if (distanceSqr < 16.0D || distanceSqr > 256.0D) return;

        Vec3 towardOwner = new Vec3(
                owner.getX() - this.getX(),
                owner.getEyeY() - this.getEyeY(),
                owner.getZ() - this.getZ()
        );

        if (towardOwner.lengthSqr() < 1.0E-4D) return;

        towardOwner = towardOwner.normalize();
        this.setDeltaMovement(this.getDeltaMovement().add(
                towardOwner.x * 0.045D,
                towardOwner.y * 0.02D,
                towardOwner.z * 0.045D
        ));
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);

        if (this.isTame()) {
            return super.mobInteract(player, hand);
        }

        if (isTamingItem(item) && this.isNearWater()) {
            if (!player.getAbilities().instabuild) {
                item.shrink(1);
            }

            if (!this.level().isClientSide) {
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.setTarget(null);
                    this.getNavigation().stop();
                    this.setHealth(this.getMaxHealth());
                    this.setWaterWolfCollarColor(DyeColor.BLUE);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private boolean isTamingItem(ItemStack stack) {
        return stack.is(Items.COD)
                || stack.is(Items.SALMON)
                || stack.is(Items.TROPICAL_FISH)
                || stack.is(Items.PUFFERFISH)
                || stack.is(Items.BONE);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON);
    }

    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob partner) {
        WaterWolfEntity baby = (WaterWolfEntity) this.getType().create(level);

        if (baby != null) {
            if (this.isTame()) {
                baby.setOwnerUUID(this.getOwnerUUID());
                baby.setTame(true, true);
                baby.setWaterWolfCollarColor(this.getWaterWolfCollarColor());
            }

            if (partner instanceof WaterWolfEntity other && other.isTame() && !this.isTame()) {
                baby.setOwnerUUID(other.getOwnerUUID());
                baby.setTame(true, true);
                baby.setWaterWolfCollarColor(other.getWaterWolfCollarColor());
            }

            if (partner instanceof WaterWolfEntity other && this.isTame() && other.isTame()) {
                baby.setWaterWolfCollarColor(
                        this.random.nextBoolean()
                                ? this.getWaterWolfCollarColor()
                                : other.getWaterWolfCollarColor()
                );
            }
        }

        return baby;
    }

    @Override
    public boolean canMate(Animal otherAnimal) {
        if (otherAnimal == this) {
            return false;
        }

        if (!(otherAnimal instanceof WaterWolfEntity other)) {
            return false;
        }

        if (!this.isTame() || !other.isTame()) {
            return false;
        }

        if (this.isInSittingPose() || other.isInSittingPose()) {
            return false;
        }

        return this.isInLove() && other.isInLove();
    }

    public @NotNull DyeColor getWaterWolfCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public void setWaterWolfCollarColor(DyeColor collarColor) {
        this.entityData.set(DATA_COLLAR_COLOR, collarColor.getId());
    }

    public boolean hasArmor() {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    private static class SeekWaterGoal extends Goal {
        private final WaterWolfEntity wolf;
        private final double speedModifier;
        private BlockPos targetPos;

        public SeekWaterGoal(WaterWolfEntity wolf, double speedModifier) {
            this.wolf = wolf;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.wolf.isTame()) return false;
            if (this.wolf.isOrderedToSit()) return false;
            if (this.wolf.getTarget() != null) return false;
            if (this.wolf.isNearWater()) return false;
            if (this.wolf.getRandom().nextInt(80) != 0) return false;

            BlockPos origin = this.wolf.blockPosition();

            for (BlockPos check : BlockPos.betweenClosed(origin.offset(-12, -2, -12), origin.offset(12, 2, 12))) {
                if (!this.wolf.level().getFluidState(check).isEmpty()) {
                    continue;
                }

                if (!this.wolf.level().getBlockState(check.below()).isSolid()) {
                    continue;
                }

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos side = check.relative(direction);
                    if (this.wolf.level().getFluidState(side).is(FluidTags.WATER)) {
                        this.targetPos = check.immutable();
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.targetPos != null
                    && !this.wolf.getNavigation().isDone()
                    && !this.wolf.isTame()
                    && !this.wolf.isOrderedToSit()
                    && this.wolf.getTarget() == null
                    && !this.wolf.isNearWater();
        }

        @Override
        public void start() {
            if (this.targetPos != null) {
                this.wolf.getNavigation().moveTo(
                        this.targetPos.getX() + 0.5D,
                        this.targetPos.getY(),
                        this.targetPos.getZ() + 0.5D,
                        this.speedModifier
                );
            }
        }

        @Override
        public void stop() {
            this.targetPos = null;
        }
    }
}