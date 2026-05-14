package com.ronm19.wolfism.entity.custom.elite;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.client.renderer.BloodWolfRenderer;
import com.ronm19.wolfism.entity.command.WolfismCommand;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import com.ronm19.wolfism.entity.custom.elemental.FireWolfEntity;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class BloodWolfEntity extends WolfismWolfEntity {
    private static final EntityDataAccessor<Boolean> BLOOD_FRENZY =
            SynchedEntityData.defineId(BloodWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(BloodWolfEntity.class, EntityDataSerializers.INT);




    private int bloodFrenzyTicks;
    private int ownerProtectionCooldown;

    public BloodWolfEntity(EntityType<? extends WolfismWolfEntity > entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLOOD_FRENZY, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.RED.getId());

    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BloodWolfHuntCommandGoal(this, 1.35D, 20.0D));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BloodWolfOwnerProtectionGoal(this));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.35D, true));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.15D, 10.0F, 2.0F));
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Player.class, true, player -> !this.isTame()));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.bloodFrenzyTicks > 0) {
                this.bloodFrenzyTicks--;
                this.setBloodFrenzy(true);

                if (this.tickCount % 20 == 0) {
                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 0, false, false));
                    this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 0, false, false));
                }
            } else {
                this.setBloodFrenzy(false);
            }

            if (this.ownerProtectionCooldown > 0) {
                this.ownerProtectionCooldown--;
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive() && this.isTargetWounded(target)) {
                Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.38D);
                Objects.requireNonNull(this.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(7.0D);
            } else if (this.isBloodFrenzy()) {
                Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.39D);
                Objects.requireNonNull(this.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(7.5D);
            } else {
                Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.34D);
                Objects.requireNonNull(this.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(6.0D);
            }
        }
    }

    @Override
    public boolean supportsCommand(WolfismCommand command) {
        return command == WolfismCommand.HUNT || super.supportsCommand(command);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);

        if (hurt && target instanceof LivingEntity living) {
            if (this.isTargetWounded(living)) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }

            if (!living.isAlive()) {
                this.activateBloodFrenzy();
            }
        }

        return hurt;
    }

    public boolean isTargetWounded(LivingEntity target) {
        return target.getHealth() <= target.getMaxHealth() * 0.5F;
    }

    public void activateBloodFrenzy() {
        this.bloodFrenzyTicks = 160;
        this.setBloodFrenzy(true);
        this.playSound(SoundEvents.WOLF_GROWL, 1.0F, 0.75F);
    }

    public boolean isBloodFrenzy() {
        return this.entityData.get(BLOOD_FRENZY);
    }

    public void setBloodFrenzy(boolean value) {
        this.entityData.set(BLOOD_FRENZY, value);
    }

    public boolean shouldGlowEyes() {
        return this.isBloodFrenzy()
                || this.getTarget() != null
                || !this.level().isDay();
    }

    public boolean canProtectOwner() {
        return this.ownerProtectionCooldown <= 0;
    }

    public void startOwnerProtectionCooldown() {
        this.ownerProtectionCooldown = 20 * 25;
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.level().isClientSide && !this.isTame() && stack.is(ModItems.BLOOD_BONE.get())) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (this.random.nextFloat() < 0.40F) {
                this.tame(player);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
                this.setTarget(player);
                this.activateBloodFrenzy();
            }

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @javax.annotation.Nullable
    public BloodWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        BloodWolfEntity wolf = (BloodWolfEntity) ModEntities.BLOOD_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof BloodWolfEntity wolf1) {
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
    public void addAdditionalSaveData( @NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BloodFrenzyTicks", this.bloodFrenzyTicks);
        tag.putInt("OwnerProtectionCooldown", this.ownerProtectionCooldown);
        tag.putByte("CollarColor", (byte) this.getCollarColor().getId());
    }

    @Override
    public void readAdditionalSaveData( @NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.bloodFrenzyTicks = tag.getInt("BloodFrenzyTicks");
        this.ownerProtectionCooldown = tag.getInt("OwnerProtectionCooldown");
        if (tag.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));
        }
    }

    public static boolean canSpawn(
            EntityType<BloodWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
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

    private static class BloodWolfOwnerProtectionGoal extends Goal {
        private final BloodWolfEntity wolf;

        public BloodWolfOwnerProtectionGoal(BloodWolfEntity wolf) {
            this.wolf = wolf;
        }

        @Override
        public boolean canUse() {
            if (!wolf.isTame() || wolf.isOrderedToSit() || !wolf.canProtectOwner()) {
                return false;
            }

            LivingEntity owner = wolf.getOwner();
            if (owner == null || !owner.isAlive()) {
                return false;
            }

            return owner.getHealth() <= owner.getMaxHealth() * 0.5F && owner.getLastHurtByMob() != null;
        }

        @Override
        public void start() {
            LivingEntity owner = wolf.getOwner();

            if (owner != null && owner.getLastHurtByMob() != null) {
                wolf.setTarget(owner.getLastHurtByMob());
                wolf.activateBloodFrenzy();
                wolf.startOwnerProtectionCooldown();
            }
        }
    }

    private static class BloodWolfHuntCommandGoal extends Goal {
        private final BloodWolfEntity wolf;
        private final double speedModifier;
        private final double huntRange;

        private int targetSearchCooldown;
        private int attackCooldown;
        private int repathCooldown;
        private int noTargetMoveCooldown;
        private int stuckTicks;
        private Vec3 lastPosition = Vec3.ZERO;

        public BloodWolfHuntCommandGoal(BloodWolfEntity wolf, double speedModifier, double huntRange) {
            this.wolf = wolf;
            this.speedModifier = speedModifier;
            this.huntRange = huntRange;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.wolf.isAlive()
                    && this.wolf.isTame()
                    && !this.wolf.isHoldingCommand()
                    && this.wolf.isInCommand(WolfismCommand.HUNT);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.targetSearchCooldown = 0;
            this.attackCooldown = 0;
            this.repathCooldown = 0;
            this.noTargetMoveCooldown = 0;
            this.stuckTicks = 0;
            this.lastPosition = this.wolf.position();
        }

        @Override
        public void stop() {
            this.wolf.getNavigation().stop();
            this.stuckTicks = 0;
        }

        @Override
        public void tick() {
            this.tickCooldowns();

            LivingEntity target = this.wolf.getTarget();

            if (!this.isValidActiveTarget(target)) {
                target = this.tryFindNewTarget();
            }

            if (target == null) {
                this.wolf.setTarget(null);
                this.doNoTargetMovement();
                return;
            }

            this.wolf.setTarget(target);
            this.wolf.getLookControl().setLookAt(target, 30.0F, 30.0F);

            this.moveTowardTarget(target);
            this.checkIfStuck(target);
            this.tryAttackTarget(target);
        }

        private void tickCooldowns() {
            if (this.targetSearchCooldown > 0) {
                this.targetSearchCooldown--;
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            if (this.repathCooldown > 0) {
                this.repathCooldown--;
            }

            if (this.noTargetMoveCooldown > 0) {
                this.noTargetMoveCooldown--;
            }
        }

        private boolean isValidActiveTarget(@Nullable LivingEntity target) {
            if (target == null || !target.isAlive()) {
                return false;
            }

            if (!this.canBloodWolfHuntTarget(target)) {
                return false;
            }

            /*
             * If the target is way outside the hunt range, drop it.
             * This prevents the wolf from obsessing over an unreachable target forever.
             */
            double maxChaseRange = this.huntRange + 10.0D;
            return this.wolf.distanceToSqr(target) <= maxChaseRange * maxChaseRange;
        }

        @Nullable
        private LivingEntity tryFindNewTarget() {
            if (this.targetSearchCooldown > 0) {
                return null;
            }

            this.targetSearchCooldown = 12;

            LivingEntity target = this.findBestBloodWolfTarget();

            if (target != null) {
                this.stuckTicks = 0;
                this.repathCooldown = 0;
            }

            return target;
        }

        private void moveTowardTarget(LivingEntity target) {
            if (this.repathCooldown > 0 && !this.wolf.getNavigation().isDone()) {
                return;
            }

            this.repathCooldown = 8;

            double finalSpeed = this.getHuntSpeed(target);
            boolean pathStarted = this.wolf.getNavigation().moveTo(target, finalSpeed);

            /*
             * If pathing fails immediately, don't let the wolf freeze.
             * Drop the target soon and force a new search/movement attempt.
             */
            if (!pathStarted && this.wolf.distanceToSqr(target) > this.getAttackReachSqr(target)) {
                this.stuckTicks += 10;
            }
        }

        private void checkIfStuck(LivingEntity target) {
            if (this.wolf.distanceToSqr(target) <= this.getAttackReachSqr(target)) {
                this.stuckTicks = 0;
                this.lastPosition = this.wolf.position();
                return;
            }

            double movedDistance = this.wolf.position().distanceToSqr(this.lastPosition);

            if (movedDistance < 0.003D && !this.wolf.getNavigation().isDone()) {
                this.stuckTicks++;
            } else {
                this.stuckTicks = 0;
            }

            this.lastPosition = this.wolf.position();

            /*
             * If stuck for about 2 seconds, drop the target and search again.
             * This fixes the "standing still and thinking" behavior.
             */
            if (this.stuckTicks >= 40) {
                this.wolf.setTarget(null);
                this.wolf.getNavigation().stop();

                this.stuckTicks = 0;
                this.targetSearchCooldown = 4;
                this.repathCooldown = 0;
                this.noTargetMoveCooldown = 0;
            }
        }

        private void tryAttackTarget(LivingEntity target) {
            if (this.wolf.distanceToSqr(target) <= this.getAttackReachSqr(target)
                    && this.attackCooldown <= 0) {
                this.attackCooldown = this.getAttackCooldown(target);
                this.wolf.doHurtTarget(target);
            }
        }

        /*
         * When no target exists, the Blood Wolf should not freeze.
         * It either stays near the owner or prowls around briefly.
         */
        private void doNoTargetMovement() {
            LivingEntity owner = this.wolf.getOwner();

            if (owner != null && owner.isAlive() && this.wolf.distanceToSqr(owner) > 144.0D) {
                this.wolf.getNavigation().moveTo(owner, 1.15D);
                return;
            }

            if (this.noTargetMoveCooldown > 0) {
                return;
            }

            this.noTargetMoveCooldown = 35 + this.wolf.getRandom().nextInt(25);

            Vec3 wanderPos = DefaultRandomPos.getPos(this.wolf, 10, 5);

            if (wanderPos != null) {
                this.wolf.getNavigation().moveTo(
                        wanderPos.x,
                        wanderPos.y,
                        wanderPos.z,
                        this.speedModifier * 0.75D
                );
            } else {
                this.wolf.getNavigation().stop();
            }
        }

        @Nullable
        private LivingEntity findBestBloodWolfTarget() {
            AABB area = this.wolf.getBoundingBox().inflate(this.huntRange);

            List<LivingEntity> targets = this.wolf.level().getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    this::canBloodWolfHuntTarget
            );

            return targets.stream()
                    .min(Comparator
                            .comparingDouble(this::getBloodPriorityScore)
                            .thenComparingDouble(this.wolf::distanceToSqr))
                    .orElse(null);
        }

        private boolean canBloodWolfHuntTarget(LivingEntity target) {
            if (target == null || !target.isAlive()) {
                return false;
            }

            if (target == this.wolf) {
                return false;
            }

            /*
             * Extra safety:
             * Blood Wolves should never hunt Wolfism wolves,
             * including untamed Blood Wolves.
             */
            if (target instanceof WolfismWolfEntity) {
                return false;
            }

            LivingEntity owner = this.wolf.getOwner();
            if (owner != null && target == owner) {
                return false;
            }

            if (target instanceof Player) {
                return false;
            }

            /*
             * allowAnimals = true because Blood Wolf hunt mode is allowed
             * to chase normal animals as part of its aggressive hunter identity.
             */
            return this.wolf.canWolfismTarget(target, true);
        }

        /*
         * Lower score = higher priority.
         *
         * Blood Wolf prefers:
         * 1. wounded enemies
         * 2. monsters
         * 3. nearby animals
         */
        private double getBloodPriorityScore(LivingEntity target) {
            double missingHealth = target.getMaxHealth() - target.getHealth();
            double healthPercent = target.getHealth() / target.getMaxHealth();

            double score = this.wolf.distanceToSqr(target) * 0.05D;

            if (target instanceof Monster) {
                score -= 30.0D;
            }

            if (target instanceof Animal) {
                score -= 5.0D;
            }

            score -= missingHealth * 4.0D;

            if (healthPercent <= 0.35F) {
                score -= 45.0D;
            }

            return score;
        }

        private double getHuntSpeed(LivingEntity target) {
            float healthPercent = target.getHealth() / target.getMaxHealth();

            if (healthPercent <= 0.35F) {
                return this.speedModifier + 0.25D;
            }

            return this.speedModifier;
        }

        private int getAttackCooldown(LivingEntity target) {
            float healthPercent = target.getHealth() / target.getMaxHealth();

            if (healthPercent <= 0.35F) {
                return 14;
            }

            return 20;
        }

        private double getAttackReachSqr(LivingEntity target) {
            double attackReach = this.wolf.getBbWidth() * 2.4D + target.getBbWidth();
            return attackReach * attackReach;
        }
    }
}