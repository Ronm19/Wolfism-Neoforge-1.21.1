package com.ronm19.wolfism.entity.custom.elemental;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class FireWolfEntity extends Wolf {

    private static final EntityDataAccessor<Boolean> HEATED =
            SynchedEntityData.defineId(FireWolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    private static final int EMBER_BONE_TAME_CHANCE = 2;

    private int packCallCooldown = 0;

    public FireWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEATED, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.ORANGE.getId());
    }

    public boolean isHeated() {
        return this.entityData.get(HEATED);
    }

    public void setHeated(boolean value) {
        this.entityData.set(HEATED, value);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new FireWolfPackAttackGoal(this, 1.3D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0D, 6.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtByTargetGoal(this));
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
            } else if (itemstack.is(ModItems.EMBER_BONE) && !this.isAngry()) {
                itemstack.consume(1, player);
                this.tryToTame(player, EMBER_BONE_TAME_CHANCE);
                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(player, hand);
            }
        } else {
            boolean flag = this.isOwnedBy(player)
                    || this.isTame()
                    || itemstack.is(ModItems.EMBER_BONE) && !this.isTame() && !this.isAngry();
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

    @javax.annotation.Nullable
    public FireWolfEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        FireWolfEntity wolf = (FireWolfEntity) ModEntities.FIRE_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof FireWolfEntity wolf1) {
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

    private List<FireWolfEntity> getNearbyPackmates(LivingEntity target, double range) {
        List<FireWolfEntity> pack = new ArrayList<>(this.level().getEntitiesOfClass(
                FireWolfEntity.class,
                this.getBoundingBox().inflate(range),
                wolf -> wolf.isAlive() && wolf != this && (wolf.getTarget() == target || wolf.getTarget() == null)
        ));

        pack.add(this);
        pack.sort(Comparator.comparingInt(Entity::getId));
        return pack;
    }

    private int getPackRole(LivingEntity target) {
        List<FireWolfEntity> pack = getNearbyPackmates(target, 12.0D);
        int index = pack.indexOf(this);

        if (index < 0) {
            return 0;
        }

        return index % 3;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.packCallCooldown > 0) {
            this.packCallCooldown--;
        }

        if (!this.level().isClientSide) {
            boolean hotBiome = isHotBiome();

            if (hotBiome || this.getTarget() != null) {
                setHeated(true);
            } else {
                setHeated(false);
            }
        }

        if (this.level().isClientSide && this.isHeated()) {
            if (this.random.nextFloat() < 0.3F) {
                double x = this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();
                double y = this.getY() + this.random.nextDouble() * this.getBbHeight();
                double z = this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth();

                this.level().addParticle(
                        net.minecraft.core.particles.ParticleTypes.FLAME,
                        x, y, z,
                        0.0D, 0.02D, 0.0D
                );
            }
        }

        if (!this.level().isClientSide && this.getTarget() != null && this.tickCount % 40 == 0) {
            this.playSound(SoundEvents.WOLF_GROWL, 1.0F, 1.0F);
        }
    }

    private boolean isHotBiome() {
        Biome biome = this.level().getBiome(this.blockPosition()).value();
        return biome.getBaseTemperature() >= 1.0F;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean success = super.doHurtTarget(target);

        if (success && target instanceof LivingEntity living) {
            living.setRemainingFireTicks(3);

            if (isHotBiome()) {
                target.hurt(this.damageSources().mobAttack(this), 2.0F);
            }

            triggerNearbyAggression();
        }

        return success;
    }

    private void triggerNearbyAggression() {
        if (this.level() instanceof ServerLevel server) {
            for (FireWolfEntity wolf : server.getEntitiesOfClass(
                    FireWolfEntity.class,
                    this.getBoundingBox().inflate(10))) {

                if (wolf != this && wolf.getTarget() == null) {
                    wolf.setTarget(this.getTarget());
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isInWaterRainOrBubble()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05, 0));
            this.setSpeed(0.28F);
        }
    }

    public static boolean canSpawn(
            EntityType<FireWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("CollarColor", (byte) this.getCollarColor().getId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));
        }
    }

    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(FireWolfEntity.class, EntityDataSerializers.INT);
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

    private static class FireWolfPackAttackGoal extends Goal {
        private final FireWolfEntity wolf;
        private final double speedModifier;
        private final boolean followingTargetEvenIfNotSeen;

        private int attackCooldown;
        private int repathCooldown;

        public FireWolfPackAttackGoal(FireWolfEntity wolf, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            this.wolf = wolf;
            this.speedModifier = speedModifier;
            this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.wolf.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.wolf.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }

            if (!this.followingTargetEvenIfNotSeen && this.wolf.getSensing().hasLineOfSight(target)) {
                return true;
            }

            return !this.wolf.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.attackCooldown = 0;
            this.repathCooldown = 0;
        }

        @Override
        public void stop() {
            this.wolf.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.wolf.getTarget();
            if (target == null) {
                return;
            }

            this.wolf.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            if (this.repathCooldown > 0) {
                this.repathCooldown--;
            }

            double distanceSqr = this.wolf.distanceToSqr(target);
            double attackReachSqr = this.getAttackReachSqr(target);

            Vec3 movePos = this.getPackMovePos(target);

            if (this.repathCooldown <= 0) {
                this.repathCooldown = 10;
                this.wolf.getNavigation().moveTo(movePos.x, movePos.y, movePos.z, this.speedModifier);
            }

            if (distanceSqr <= attackReachSqr && this.attackCooldown <= 0) {
                this.attackCooldown = 20;
                this.wolf.doHurtTarget(target);
            }
        }

        private Vec3 getPackMovePos(LivingEntity target) {
            int role = this.wolf.getPackRole(target);

            Vec3 targetPos = target.position();
            Vec3 forward = target.getLookAngle();

            if (forward.lengthSqr() < 1.0E-4D) {
                forward = new Vec3(1.0D, 0.0D, 0.0D);
            }

            forward = forward.normalize();
            Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);

            return switch (role) {
                case 1 -> targetPos.add(side.scale(1.8D));
                case 2 -> targetPos.add(side.scale(-1.8D));
                default -> targetPos;
            };
        }

        private double getAttackReachSqr(LivingEntity target) {
            return (this.wolf.getBbWidth() * 2.0F) * (this.wolf.getBbWidth() * 2.0F) + target.getBbWidth();
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_AMBIENT;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return SoundEvents.WOLF_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }
}