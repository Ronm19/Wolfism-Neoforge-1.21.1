package com.ronm19.wolfism.entity.custom.elite;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.command.WolfismCommand;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import com.ronm19.wolfism.entity.command.WolfismCommand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class LunarWolfEntity extends WolfismWolfEntity  {
    private int ownerBuffCooldown = 0;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(LunarWolfEntity.class, EntityDataSerializers.INT);

    public LunarWolfEntity( EntityType<? extends WolfismWolfEntity> entityType, Level level ) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Special Lunar Wolf support ability.
        this.goalSelector.addGoal(1, new LunarWolfNightWatchCommandGoal(this, 1.25D));
        this.goalSelector.addGoal(3, new LunarHowlGoal(this));
    }

    // ===== DATA =====
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.WHITE.getId());
    }

    @Override
    public boolean supportsCommand( WolfismCommand command) {
        return command == WolfismCommand.NIGHT_WATCH || super.supportsCommand(command);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            applyLunarPower();
            applyOwnerMoonlitBond();
        }

        spawnLunarParticles();
    }

    private void applyLunarPower() {
        if (!isLunarActive()) {
            return;
        }

        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, true, false, true));

        if (this.getTarget() != null) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0, true, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false, true));
        }
    }

    private void applyOwnerMoonlitBond() {
        if (!this.isTame() || this.getOwner() == null || !isLunarActive()) {
            return;
        }

        if (ownerBuffCooldown > 0) {
            ownerBuffCooldown--;
            return;
        }

        LivingEntity owner = this.getOwner();

        if (owner.distanceTo(this) <= 18.0F) {
            owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
            owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, true, false, true));
            ownerBuffCooldown = 100;
        }
    }

    private void spawnLunarParticles() {
        if (!this.level().isClientSide) {
            return;
        }

        if (!isLunarActive()) {
            return;
        }

        if (this.random.nextInt(14) == 0) {
            double x = this.getX() + (this.random.nextDouble() - 0.5D) * 0.7D;
            double y = this.getY() + 0.6D + this.random.nextDouble() * 0.5D;
            double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.7D;

            this.level().addParticle(
                    ParticleTypes.END_ROD,
                    x,
                    y,
                    z,
                    0.0D,
                    0.01D,
                    0.0D
            );
        }
    }

    public boolean isLunarActive() {
        if (this.level() == null) {
            return false;
        }

        boolean night = this.level().isNight();
        boolean thunder = this.level().isThundering();
        boolean darkArea = this.level().getBrightness(LightLayer.SKY, this.blockPosition()) <= 4;

        return night || thunder || darkArea;
    }

    public boolean shouldGlowEyes() {
        return isLunarActive() || this.getTarget() != null;
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand ) {
        ItemStack stack = player.getItemInHand(hand);

        // Special higher-chance taming item.
        if (!this.isTame() && stack.is(ModItems.LUNAR_BONE.get())) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // Higher chance than normal bone.
                if (this.random.nextInt(3) != 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public @Nullable LunarWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent ) {
        LunarWolfEntity wolf = ModEntities.LUNAR_WOLF.get().create(level);

        if (wolf != null && otherParent instanceof LunarWolfEntity other) {

            wolf.setVariant(this.random.nextBoolean() ? this.getVariant() : other.getVariant());

            if (this.isTame()) {
                wolf.setOwnerUUID(this.getOwnerUUID());
                wolf.setTame(true, true);
                wolf.setCollarColor(this.random.nextBoolean() ? this.getCollarColor() : other.getCollarColor());
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
            EntityType<LunarWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    private static class LunarHowlGoal extends Goal {
        private final LunarWolfEntity wolf;
        private int cooldown = 0;

        public LunarHowlGoal(LunarWolfEntity wolf) {
            this.wolf = wolf;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }

            return wolf.isAlive()
                    && wolf.isTame()
                    && wolf.isLunarActive()
                    && wolf.getTarget() != null
                    && wolf.random.nextInt(80) == 0;
        }

        @Override
        public void start() {
            cooldown = 400;

            wolf.level().playSound(
                    null,
                    wolf.blockPosition(),
                    SoundEvents.WOLF_HOWL,
                    SoundSource.NEUTRAL,
                    1.2F,
                    0.75F
            );

            UUID ownerId = wolf.getOwnerUUID();

            List<Wolf> nearbyWolves = wolf.level().getEntitiesOfClass(
                    Wolf.class,
                    wolf.getBoundingBox().inflate(10.0D),
                    otherWolf -> otherWolf.isAlive()
                            && otherWolf.isTame()
                            && otherWolf.getOwnerUUID() != null
                            && otherWolf.getOwnerUUID().equals(ownerId)
            );

            for (Wolf alliedWolf : nearbyWolves) {
                alliedWolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0, true, false, true));
                alliedWolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, true, false, true));
            }
        }
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

    private static class LunarWolfNightWatchCommandGoal extends Goal {
        private final LunarWolfEntity wolf;
        private final double speedModifier;

        private int targetSearchCooldown;
        private int attackCooldown;
        private int repathCooldown;

        public LunarWolfNightWatchCommandGoal(LunarWolfEntity wolf, double speedModifier) {
            this.wolf = wolf;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.wolf.isAlive()
                    && this.wolf.isTame()
                    && !this.wolf.isHoldingCommand()
                    && this.wolf.isInCommand(WolfismCommand.NIGHT_WATCH)
                    && this.wolf.getOwner() != null;
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
        }

        @Override
        public void stop() {
            this.wolf.getNavigation().stop();
            this.wolf.setTarget(null);
        }

        @Override
        public void tick() {
            LivingEntity owner = this.wolf.getOwner();

            if (owner == null || !owner.isAlive()) {
                this.wolf.getNavigation().stop();
                this.wolf.setTarget(null);
                return;
            }

            this.tickCooldowns();

            LivingEntity target = this.wolf.getTarget();

            if (!this.isValidNightWatchTarget(target, owner)) {
                this.wolf.setTarget(null);
                target = null;

                if (this.targetSearchCooldown <= 0) {
                    this.targetSearchCooldown = this.isNightWatchStrong() ? 10 : 18;
                    target = this.findBestNightWatchTarget(owner);
                    this.wolf.setTarget(target);
                }
            }

            if (target != null && target.isAlive()) {
                this.moveAndAttackTarget(target);
                return;
            }

            this.stayNearOwner(owner);
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
        }

        private boolean isNightWatchStrong() {
            return this.wolf.level().isNight()
                    || this.wolf.level().getRawBrightness(this.wolf.blockPosition(), 0) <= 7;
        }

        private double getDetectRange() {
            return this.isNightWatchStrong() ? 24.0D : 13.0D;
        }

        private double getFinalSpeed() {
            return this.isNightWatchStrong() ? this.speedModifier + 0.15D : this.speedModifier;
        }

        private int getFinalAttackCooldown() {
            return this.isNightWatchStrong() ? 17 : 21;
        }

        private LivingEntity findBestNightWatchTarget(LivingEntity owner) {
            double detectRange = this.getDetectRange();
            AABB area = owner.getBoundingBox().inflate(detectRange);

            List<LivingEntity> targets = this.wolf.level().getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    target -> this.isValidNightWatchTarget(target, owner)
            );

            return targets.stream()
                    .min(Comparator
                            .comparingDouble((LivingEntity target) -> this.getNightWatchPriorityScore(target, owner))
                            .thenComparingDouble(this.wolf::distanceToSqr))
                    .orElse(null);
        }

        private boolean isValidNightWatchTarget(LivingEntity target, LivingEntity owner) {
            if (target == null || !target.isAlive()) {
                return false;
            }

            if (target == this.wolf) {
                return false;
            }

            if (target == owner) {
                return false;
            }

            if (target instanceof Player) {
                return false;
            }

            if (!(target instanceof Monster)) {
                return false;
            }

            if (!this.wolf.canWolfismTarget(target, false)) {
                return false;
            }

            double detectRange = this.getDetectRange();

            return target.distanceToSqr(owner) <= detectRange * detectRange;
        }

        /*
         * Lower score = higher priority.
         *
         * Lunar Wolf prefers:
         * 1. monsters targeting owner
         * 2. monsters close to owner
         * 3. monsters targeting the Lunar Wolf
         * 4. darkness/night threats
         */
        private double getNightWatchPriorityScore(LivingEntity target, LivingEntity owner) {
            double score = target.distanceToSqr(owner);

            if (target instanceof Monster monster) {
                if (monster.getTarget() == owner) {
                    score -= 250.0D;
                }

                if (monster.getTarget() == this.wolf) {
                    score -= 100.0D;
                }
            }

            if (target.distanceToSqr(owner) <= 25.0D) {
                score -= 80.0D;
            }

            if (this.isNightWatchStrong()) {
                score -= 35.0D;
            }

            return score;
        }

        private void moveAndAttackTarget(LivingEntity target) {
            this.wolf.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (this.repathCooldown <= 0 || this.wolf.getNavigation().isDone()) {
                this.repathCooldown = 8;
                this.wolf.getNavigation().moveTo(target, this.getFinalSpeed());
            }

            if (this.wolf.distanceToSqr(target) <= this.getAttackReachSqr(target)
                    && this.attackCooldown <= 0) {
                this.attackCooldown = this.getFinalAttackCooldown();
                this.wolf.doHurtTarget(target);
            }
        }

        private void stayNearOwner(LivingEntity owner) {
            double distanceToOwner = this.wolf.distanceToSqr(owner);

            /*
             * Lunar Wolf should feel like a focused guard, not a wandering hunter.
             */
            if (distanceToOwner > 49.0D) {
                if (this.repathCooldown <= 0 || this.wolf.getNavigation().isDone()) {
                    this.repathCooldown = 10;
                    this.wolf.getNavigation().moveTo(owner, this.speedModifier);
                }
            } else {
                this.wolf.getNavigation().stop();
                this.wolf.getLookControl().setLookAt(owner, 20.0F, 20.0F);
            }
        }

        private double getAttackReachSqr(LivingEntity target) {
            double attackReach = this.wolf.getBbWidth() * 2.25D + target.getBbWidth();
            return attackReach * attackReach;
        }
    }
}