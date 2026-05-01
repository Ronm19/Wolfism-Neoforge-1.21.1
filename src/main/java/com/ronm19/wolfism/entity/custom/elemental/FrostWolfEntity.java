package com.ronm19.wolfism.entity.custom.elemental;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrostWolfEntity extends Wolf {
    private final Map<Integer, Integer> frostStacks = new HashMap<>();
    private int retreatCooldown = 0;
    private int packJoinDelay = 0;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;
    private static final int FROST_BONE_TAME_CHANCE = 2;

    public FrostWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 4.5D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.ARMOR, 5.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.CYAN.getId());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new FrostWolfAttackGoal(this, 1.1D));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 6.0F, 2.0F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new FrostWolfProtectOwnerGoal(this, 12.0D));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(5, new FrostWolfPackAssistGoal(this, 16.0D));
    }

    public @NotNull DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public boolean isGlowingEyes() {
        return !this.level().isDay() || (this.level().isRaining() && this.level().canSeeSky(this.blockPosition()));
    }

    public boolean hasArmor() {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR);
    }

    private void setCollarColor(DyeColor collarColor) {
        this.entityData.set(DATA_COLLAR_COLOR, collarColor.getId());
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (!this.level().isClientSide || this.isBaby() && this.isFood(itemstack)) {
            if (this.isTame()) {
                if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                    FoodProperties foodproperties = itemstack.getFoodProperties(this);
                    float f = foodproperties != null ? (float) foodproperties.nutrition() : 1.0F;
                    this.heal(2.0F * f);
                    itemstack.consume(1, player);
                    this.gameEvent(GameEvent.EAT);
                    return InteractionResult.sidedSuccess(this.level().isClientSide());
                } else {
                    if (item instanceof DyeItem dyeitem) {
                        if (this.isOwnedBy(player)) {
                            DyeColor dyecolor = dyeitem.getDyeColor();
                            if (dyecolor != this.getCollarColor()) {
                                this.setCollarColor(dyecolor);
                                itemstack.consume(1, player);
                                return InteractionResult.SUCCESS;
                            }

                            return super.mobInteract(player, hand);
                        }
                    }

                    if (itemstack.is(Items.WOLF_ARMOR) && this.isOwnedBy(player) && this.getBodyArmorItem().isEmpty() && !this.isBaby()) {
                        this.setBodyArmorItem(itemstack.copyWithCount(1));
                        itemstack.consume(1, player);
                        return InteractionResult.SUCCESS;
                    } else if (!itemstack.canPerformAction(ItemAbilities.SHEARS_REMOVE_ARMOR)
                            || !this.isOwnedBy(player)
                            || !this.hasArmor()
                            || EnchantmentHelper.has(this.getBodyArmorItem(), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) && !player.isCreative()) {
                        if (((Ingredient) ((ArmorMaterial) ArmorMaterials.ARMADILLO.value()).repairIngredient().get()).test(itemstack)
                                && this.isInSittingPose()
                                && this.hasArmor()
                                && this.isOwnedBy(player)
                                && this.getBodyArmorItem().isDamaged()) {
                            itemstack.shrink(1);
                            this.playSound(SoundEvents.WOLF_ARMOR_REPAIR);
                            ItemStack itemstack2 = this.getBodyArmorItem();
                            int i = (int) ((float) itemstack2.getMaxDamage() * 0.125F);
                            itemstack2.setDamageValue(Math.max(0, itemstack2.getDamageValue() - i));
                            return InteractionResult.SUCCESS;
                        } else {
                            InteractionResult interactionresult = super.mobInteract(player, hand);
                            if (!interactionresult.consumesAction() && this.isOwnedBy(player)) {
                                this.setOrderedToSit(!this.isOrderedToSit());
                                this.jumping = false;
                                this.navigation.stop();
                                this.setTarget((LivingEntity) null);
                                return InteractionResult.SUCCESS_NO_ITEM_USED;
                            } else {
                                return interactionresult;
                            }
                        }
                    } else {
                        itemstack.hurtAndBreak(1, player, getSlotForHand(hand));
                        this.playSound(SoundEvents.ARMOR_UNEQUIP_WOLF);
                        ItemStack itemstack1 = this.getBodyArmorItem();
                        this.setBodyArmorItem(ItemStack.EMPTY);
                        this.spawnAtLocation(itemstack1);
                        return InteractionResult.SUCCESS;
                    }
                }
            } else if (itemstack.is(ModItems.FROST_BONE) && !this.isAngry()) {
                itemstack.consume(1, player);
                this.tryToTame(player, FROST_BONE_TAME_CHANCE);
                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(player, hand);
            }
        } else {
            boolean flag = this.isOwnedBy(player)
                    || this.isTame()
                    || itemstack.is(ModItems.FROST_BONE) && !this.isTame() && !this.isAngry();
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
    }

    private void tryToTame(Player player, int chanceBound) {
        if (this.random.nextInt(chanceBound) == 0 && !EventHooks.onAnimalTame(this, player)) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget((LivingEntity) null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte) 7);
        } else {
            this.level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity entity) {
        boolean success = super.doHurtTarget(entity);

        if (success && entity instanceof LivingEntity target) {
            int id = target.getId();
            int stacks = frostStacks.getOrDefault(id, 0) + 1;
            frostStacks.put(id, stacks);

            int amplifier = Math.min(stacks / 2, 1);

            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    60,
                    amplifier
            ));

            if (this.random.nextFloat() < 0.15F) {
                for (LivingEntity nearby : this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(3.0D),
                        e -> e != this && e != this.getOwner()
                )) {
                    nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
            }
        }

        return success;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            this.spawnAmbientFrostParticles();
        } else {
            this.handleFrostPathing();
        }
    }

    private void spawnAmbientFrostParticles() {
        if (this.isOrderedToSit()) {
            return;
        }

        double motionX = this.getDeltaMovement().x;
        double motionZ = this.getDeltaMovement().z;
        double horizontalSpeedSq = motionX * motionX + motionZ * motionZ;

        if (horizontalSpeedSq < 0.0025D) {
            return;
        }

        if (this.tickCount % 3 != 0) {
            return;
        }

        int particleCount = this.isGlowingEyes() ? 3 : 2;

        for (int i = 0; i < particleCount; i++) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
            double y = this.getY() + 0.15D + this.random.nextDouble() * 0.6D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();

            double vx = -motionX * 0.25D + (this.random.nextDouble() - 0.5D) * 0.02D;
            double vy = 0.01D + this.random.nextDouble() * 0.02D;
            double vz = -motionZ * 0.25D + (this.random.nextDouble() - 0.5D) * 0.02D;

            this.level().addParticle(
                    ParticleTypes.SNOWFLAKE,
                    x, y, z,
                    vx, vy, vz
            );
        }

        if (this.onGround() && this.random.nextFloat() < 0.35F) {
            for (int i = 0; i < 2; i++) {
                double x = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                double y = this.getY() + 0.05D;
                double z = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();

                this.level().addParticle(
                        ParticleTypes.SNOWFLAKE,
                        x, y, z,
                        (this.random.nextDouble() - 0.5D) * 0.01D,
                        0.02D,
                        (this.random.nextDouble() - 0.5D) * 0.01D
                );
            }
        }
    }

    private void handleFrostPathing() {
        if (!this.isTame()) return;
        if (!this.onGround()) return;
        if (this.isOrderedToSit()) return;
        if (this.getDeltaMovement().horizontalDistanceSqr() < 0.0025D) return;
        if (this.tickCount % 4 != 0) return;

        BlockPos center = this.blockPosition();
        int radius = 1;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -1, -radius),
                center.offset(radius, -1, radius)
        )) {
            if (!pos.closerToCenterThan(this.position(), radius + 0.75D)) {
                continue;
            }

            BlockPos abovePos = pos.above();
            BlockState state = this.level().getBlockState(pos);
            BlockState aboveState = this.level().getBlockState(abovePos);

            if (state.is(Blocks.WATER) && aboveState.isAir()) {
                BlockState frosted = Blocks.FROSTED_ICE.defaultBlockState();

                if (frosted.canSurvive(this.level(), pos)) {
                    this.level().setBlockAndUpdate(pos, frosted);
                    this.level().scheduleTick(
                            pos,
                            Blocks.FROSTED_ICE,
                            Mth.nextInt(this.random, 60, 120)
                    );
                }
                continue;
            }

            if (aboveState.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(this.level(), abovePos)) {
                BlockState floorState = this.level().getBlockState(pos);

                if (floorState.isFaceSturdy(this.level(), pos, Direction.UP)
                        && !floorState.is(Blocks.ICE)
                        && !floorState.is(Blocks.FROSTED_ICE)
                        && !floorState.is(Blocks.LAVA)
                        && !floorState.is(Blocks.MAGMA_BLOCK)) {
                    this.level().setBlockAndUpdate(abovePos, Blocks.SNOW.defaultBlockState());
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (retreatCooldown > 0) {
                retreatCooldown--;
            }

            if (packJoinDelay > 0) {
                packJoinDelay--;
            }

            if (this.tickCount % 40 == 0) {
                frostStacks.entrySet().removeIf(entry -> {
                    Entity entity = this.level().getEntity(entry.getKey());
                    return !(entity instanceof LivingEntity living) || !living.isAlive();
                });
            }

            if (this.getHealth() < this.getMaxHealth() * 0.35F && retreatCooldown == 0) {
                LivingEntity target = this.getTarget();
                this.setTarget(null);
                this.getNavigation().stop();

                if (target != null) {
                    double dx = this.getX() - target.getX();
                    double dz = this.getZ() - target.getZ();
                    double len = Math.sqrt(dx * dx + dz * dz);

                    if (len > 0.0D) {
                        dx /= len;
                        dz /= len;

                        double retreatX = this.getX() + dx * 6.0D;
                        double retreatZ = this.getZ() + dz * 6.0D;

                        this.getNavigation().moveTo(retreatX, this.getY(), retreatZ, 1.2D);
                    }
                }

                retreatCooldown = 100;
            }
        }
    }

    @javax.annotation.Nullable
    public FrostWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        FrostWolfEntity wolf = (FrostWolfEntity) ModEntities.FROST_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof FrostWolfEntity wolf1) {
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
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_FIRE)) {
            amount *= 1.4F;
        }
        return super.hurt(source, amount);
    }

    public boolean isPackMate(FrostWolfEntity other) {
        if (other == null || other == this || !other.isAlive()) {
            return false;
        }

        if (!this.isTame() || !other.isTame()) {
            return false;
        }

        if (this.getOwnerUUID() == null || other.getOwnerUUID() == null) {
            return false;
        }

        return this.getOwnerUUID().equals(other.getOwnerUUID());
    }

    public boolean canJoinPackTarget(LivingEntity target) {
        LivingEntity owner = this.getOwner();

        if (target == null || owner == null) {
            return false;
        }

        if (!target.isAlive()) {
            return false;
        }

        if (target == this || target == owner) {
            return false;
        }

        if (this.canAttackWithOwnerTarget(target, owner)) {
            return false;
        }

        if (owner.distanceToSqr(target) > 16.0D * 16.0D && this.distanceToSqr(target) > 20.0D * 20.0D) {
            return false;
        }

        return true;
    }

    public boolean canAttackWithOwnerTarget(LivingEntity target, LivingEntity owner) {
        if (target == null || target == owner || target == this) {
            return false;
        }

        if (!target.isAlive()) {
            return false;
        }

        if (this.isAlliedTo(target) || owner.isAlliedTo(target)) {
            return false;
        }

        if (target instanceof TamableAnimal tameable && tameable.isTame() && tameable.getOwner() == owner) {
            return false;
        }

        return true;
    }

    @Override
    protected @NotNull SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_AMBIENT;
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected @NotNull SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        this.playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RetreatCooldown", this.retreatCooldown);
        tag.putInt("PackJoinDelay", this.packJoinDelay);
        tag.putByte("CollarColor", (byte) this.getCollarColor().getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.retreatCooldown = tag.getInt("RetreatCooldown");
        this.packJoinDelay = tag.getInt("PackJoinDelay");
        if (tag.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));
        }
    }

    public static class FrostWolfProtectOwnerGoal extends TargetGoal {
        private final FrostWolfEntity wolf;
        private final double range;
        private LivingEntity target;

        public FrostWolfProtectOwnerGoal(FrostWolfEntity wolf, double range) {
            super(wolf, false, true);
            this.wolf = wolf;
            this.range = range;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!this.wolf.isTame() || this.wolf.isOrderedToSit()) {
                return false;
            }

            LivingEntity owner = this.wolf.getOwner();
            if (owner == null || !owner.isAlive()) {
                return false;
            }

            LivingEntity found = this.findClosestThreat(owner);

            if (found == null) return false;

            if (this.mob.getTarget() != null && this.mob.getTarget().isAlive()) {
                return false;
            }

            this.target = found;
            return true;
        }

        @Override
        public void start() {
            this.mob.setTarget(this.target);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity owner = this.wolf.getOwner();
            LivingEntity currentTarget = this.mob.getTarget();

            if (owner == null || currentTarget == null || !currentTarget.isAlive()) {
                return false;
            }

            if (owner.distanceToSqr(currentTarget) > (this.range * this.range)) {
                return false;
            }

            return this.isRealThreat(owner, currentTarget);
        }

        private LivingEntity findClosestThreat(LivingEntity owner) {
            return owner.level()
                    .getEntitiesOfClass(
                            LivingEntity.class,
                            owner.getBoundingBox().inflate(this.range, 6.0D, this.range),
                            entity -> this.isRealThreat(owner, entity)
                    )
                    .stream()
                    .min(Comparator.comparingDouble(owner::distanceToSqr))
                    .orElse(null);
        }

        private boolean isRealThreat(LivingEntity owner, LivingEntity entity) {
            if (entity == null || entity == this.wolf || entity == owner) {
                return false;
            }

            if (!entity.isAlive()) {
                return false;
            }

            if (this.wolf.isAlliedTo(entity) || owner.isAlliedTo(entity)) {
                return false;
            }

            if (entity instanceof TamableAnimal tameable && tameable.isTame() && tameable.getOwner() == owner) {
                return false;
            }

            if (this.wolf.canAttackWithOwnerTarget(entity, owner)) {
                return false;
            }

            boolean directlyHurtingOwner = owner.getLastHurtByMob() == entity;
            boolean targetingOwner = entity instanceof Mob mob && mob.getTarget() == owner;
            boolean closeHostile = entity instanceof Monster && owner.distanceToSqr(entity) <= 64.0D;

            return directlyHurtingOwner || targetingOwner || closeHostile;
        }
    }

    public class FrostWolfPackAssistGoal extends TargetGoal {
        private final FrostWolfEntity wolf;
        private final double range;
        private LivingEntity target;

        public FrostWolfPackAssistGoal(FrostWolfEntity wolf, double range) {
            super(wolf, false);
            this.wolf = wolf;
            this.range = range;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!this.wolf.isTame() || this.wolf.isOrderedToSit()) {
                return false;
            }

            if (this.wolf.getOwner() == null) {
                return false;
            }

            if (FrostWolfEntity.this.packJoinDelay > 0) {
                return false;
            }

            LivingEntity currentTarget = this.wolf.getTarget();
            if (currentTarget != null && currentTarget.isAlive()) {
                return false;
            }

            LivingEntity found = this.findPackTarget();
            if (found == null) {
                return false;
            }

            FrostWolfEntity.this.packJoinDelay = 10 + this.wolf.getRandom().nextInt(10);
            this.target = found;
            return true;
        }

        @Override
        public void start() {
            this.mob.setTarget(this.target);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity currentTarget = this.mob.getTarget();

            if (currentTarget == null || !currentTarget.isAlive()) {
                return false;
            }

            if (!this.wolf.canJoinPackTarget(currentTarget)) {
                return false;
            }

            LivingEntity owner = this.wolf.getOwner();
            return owner == null || owner.distanceToSqr(currentTarget) <= (this.range * this.range);
        }

        private LivingEntity findPackTarget() {
            LivingEntity best = null;
            double bestScore = Double.MAX_VALUE;

            for (FrostWolfEntity ally : this.wolf.level().getEntitiesOfClass(
                    FrostWolfEntity.class,
                    this.wolf.getBoundingBox().inflate(this.range, 4.0D, this.range),
                    other -> this.wolf.isPackMate(other) && !other.isOrderedToSit()
            )) {
                LivingEntity allyTarget = ally.getTarget();

                if (allyTarget == null || !allyTarget.isAlive()) continue;
                if (!this.wolf.canJoinPackTarget(allyTarget)) continue;

                double distanceScore = this.wolf.distanceToSqr(allyTarget);

                int nearbyAllies = this.wolf.level().getEntitiesOfClass(
                        FrostWolfEntity.class,
                        allyTarget.getBoundingBox().inflate(2.0D),
                        w -> this.wolf.isPackMate(w)
                ).size();

                double crowdPenalty = nearbyAllies * 2.0D;
                double finalScore = distanceScore + crowdPenalty;

                if (finalScore < bestScore) {
                    bestScore = finalScore;
                    best = allyTarget;
                }
            }

            return best;
        }
    }

    public static class FrostWolfAttackGoal extends MeleeAttackGoal {
        private final FrostWolfEntity wolf;

        public FrostWolfAttackGoal(FrostWolfEntity wolf, double speed) {
            super(wolf, speed, true);
            this.wolf = wolf;
        }

        @Override
        public boolean canUse() {
            if (this.wolf.isOrderedToSit()) {
                return false;
            }

            if (this.wolf.getHealth() < this.wolf.getMaxHealth() * 0.35F) {
                return false;
            }

            return super.canUse();
        }

        @Override
        public void tick() {
            super.tick();

            LivingEntity target = this.mob.getTarget();
            if (target == null) return;

            if (this.wolf.getHealth() < this.wolf.getMaxHealth() * 0.35F) {
                this.wolf.setTarget(null);
                this.wolf.getNavigation().stop();
                return;
            }

            double distanceSq = this.mob.distanceToSqr(target);

            List<FrostWolfEntity> allies = this.wolf.level().getEntitiesOfClass(
                    FrostWolfEntity.class,
                    this.wolf.getBoundingBox().inflate(4.0D),
                    ally -> ally != this.wolf
                            && this.wolf.isPackMate(ally)
                            && ally.getTarget() == target
            );

            if (!allies.isEmpty()) {
                float offset = (this.wolf.getId() % 3 - 1) * 0.35F;
                this.wolf.getMoveControl().strafe(0.0F, offset);
            }

            if (distanceSq < 6.25D) {
                float sideways = (this.wolf.getId() & 1) == 0 ? 0.25F : -0.25F;
                this.wolf.getMoveControl().strafe(-0.15F, sideways);
            }

            if (this.wolf.tickCount % 40 == 0 && distanceSq < 4.0D) {
                this.wolf.getNavigation().stop();
            }
        }
    }

    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(FrostWolfEntity.class, EntityDataSerializers.INT);
    }
}