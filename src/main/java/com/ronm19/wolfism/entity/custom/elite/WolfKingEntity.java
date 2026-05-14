package com.ronm19.wolfism.entity.custom.elite;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.command.WolfismCommand;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.ronm19.wolfism.entity.command.WolfismCommand;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import java.util.List;
import java.util.UUID;

public class WolfKingEntity extends WolfismWolfEntity {
    private static final EntityDataAccessor<Boolean> ROYAL_RAGE =
            SynchedEntityData.defineId(WolfKingEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(WolfKingEntity.class, EntityDataSerializers.INT);


    private static final int ROYAL_RAGE_DURATION = 20 * 14;
    private static final int ROYAL_RAGE_COOLDOWN = 20 * 55;
    private static final float ROYAL_BONE_TAME_CHANCE = 0.75F;

    private int royalAuraTick;
    private int royalRageTime;
    private int royalRageCooldown;

    public WolfKingEntity(EntityType<? extends WolfismWolfEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 52.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.goalSelector.addGoal(1, new WolfKingRoyalCommandGoal(this, 1.2D, 20.0D));

    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROYAL_RAGE, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.YELLOW.getId());
    }

    @Override
    public boolean supportsCommand( WolfismCommand command) {
        return command == WolfismCommand.ROYAL_COMMAND || super.supportsCommand(command);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            this.handleRoyalRage();

            this.royalAuraTick++;
            if (this.royalAuraTick >= 40) {
                this.royalAuraTick = 0;
                this.applyRoyalPresence();
            }
        }
    }

    private void handleRoyalRage() {
        if (this.royalRageCooldown > 0) {
            this.royalRageCooldown--;
        }

        if (this.royalRageTime > 0) {
            this.royalRageTime--;
            this.setRoyalRageActive(true);

            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 45, 1, true, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 1, true, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 0, true, false, true));

            return;
        }

        this.setRoyalRageActive(false);

        if (!this.isTame() || this.royalRageCooldown > 0) {
            return;
        }

        boolean ownerInDanger = this.isOwnerInDanger();
        boolean surroundedByEnemies = this.countNearbyEnemies(10.0D) >= 3;
        boolean inCombat = this.getTarget() != null && this.getTarget().isAlive();

        if (ownerInDanger || surroundedByEnemies || inCombat) {
            this.startRoyalRage();
        }
    }

    private boolean isOwnerInDanger() {
        if (this.getOwner() == null) {
            return false;
        }

        return this.getOwner().getHealth() <= this.getOwner().getMaxHealth() * 0.35F;
    }

    private int countNearbyEnemies(double range) {
        List<Monster> enemies = this.level().getEntitiesOfClass(
                Monster.class,
                this.getBoundingBox().inflate(range),
                enemy -> enemy.isAlive() && !enemy.isAlliedTo(this)
        );

        return enemies.size();
    }

    private void startRoyalRage() {
        this.royalRageTime = ROYAL_RAGE_DURATION;
        this.royalRageCooldown = ROYAL_RAGE_COOLDOWN;
        this.setRoyalRageActive(true);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    this.getX(),
                    this.getY() + 0.8D,
                    this.getZ(),
                    35,
                    0.55D,
                    0.45D,
                    0.55D,
                    0.05D
            );
        }
    }

    private void applyRoyalPresence() {
        if (!this.isTame()) {
            return;
        }

        UUID ownerId = this.getOwnerUUID();
        if (ownerId == null) {
            return;
        }

        List<Wolf> alliedWolves = this.level().getEntitiesOfClass(
                Wolf.class,
                this.getBoundingBox().inflate(10.0D),
                wolf -> wolf.isAlive()
                        && wolf.isTame()
                        && wolf.getOwnerUUID() != null
                        && wolf.getOwnerUUID().equals(ownerId)
        );

        int nearbyAlliedWolves = 0;

        for (Wolf wolf : alliedWolves) {
            if (wolf == this) {
                continue;
            }

            nearbyAlliedWolves++;

            wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, this.isRoyalRageActive() ? 1 : 0, true, false, true));
            wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));

            if (this.isRoyalRageActive()) {
                wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
            }
        }

        if (nearbyAlliedWolves >= 2) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
        }

        if (nearbyAlliedWolves >= 4) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame() && stack.is(ModItems.ROYAL_BONE.get())) {
            if (this.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (this.random.nextFloat() < ROYAL_BONE_TAME_CHANCE) {
                this.tame(player);
                this.getNavigation().stop();
                this.setTarget(null);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            this.getX(),
                            this.getY() + 0.8D,
                            this.getZ(),
                            18,
                            0.4D,
                            0.35D,
                            0.4D,
                            0.05D
                    );
                }
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }

            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);

        if (hurt && this.isRoyalRageActive()) {
            target.hurt(this.damageSources().mobAttack(this), 3.0F);
        }

        return hurt;
    }

    public boolean isRoyalRageActive() {
        return this.entityData.get(ROYAL_RAGE);
    }

    private void setRoyalRageActive(boolean value) {
        this.entityData.set(ROYAL_RAGE, value);
    }

    public boolean shouldGlowEyes() {
        return this.isRoyalRageActive() || this.isAggressive();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("CollarColor", (byte) this.getCollarColor().getId());
        tag.putInt("RoyalRageTime", this.royalRageTime);
        tag.putInt("RoyalRageCooldown", this.royalRageCooldown);
        tag.putBoolean("RoyalRageActive", this.isRoyalRageActive());
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

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.royalRageTime = tag.getInt("RoyalRageTime");
        this.royalRageCooldown = tag.getInt("RoyalRageCooldown");
        this.setRoyalRageActive(tag.getBoolean("RoyalRageActive"));

        if (tag.contains("CollarColor", 99))
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));

        }

    @Override
    public @Nullable WolfKingEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        WolfKingEntity baby = ModEntities.WOLF_KING.get().create(level);

        if (baby != null && otherParent instanceof WolfKingEntity other) {
            if (this.isTame()) {
                baby.setOwnerUUID(this.getOwnerUUID());
                baby.setTame(true, true);
                baby.setCollarColor(this.random.nextBoolean() ? this.getCollarColor() : other.getCollarColor());
            }
        }

        return baby;
    }

    public static boolean canSpawn(
            EntityType<WolfKingEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    private static class WolfKingRoyalCommandGoal extends Goal {
        private final WolfKingEntity wolf;
        private final double speedModifier;
        private final double commandRange;

        private int targetSearchCooldown;
        private int attackCooldown;
        private int auraCooldown;
        private int rallyCooldown;
        private int repathCooldown;
        private int intimidationCooldown;

        public WolfKingRoyalCommandGoal(WolfKingEntity wolf, double speedModifier, double commandRange) {
            this.wolf = wolf;
            this.speedModifier = speedModifier;
            this.commandRange = commandRange;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.wolf.isAlive()
                    && this.wolf.isTame()
                    && !this.wolf.isHoldingCommand()
                    && this.wolf.isInCommand(WolfismCommand.ROYAL_COMMAND)
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
            this.auraCooldown = 0;
            this.rallyCooldown = 0;
            this.repathCooldown = 0;
            this.intimidationCooldown = 0;
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
            this.applyRoyalAura(owner);
            this.applyRoyalIntimidation(owner);

            LivingEntity target = this.wolf.getTarget();

            if (!this.isValidRoyalTarget(target, owner)) {
                this.wolf.setTarget(null);
                target = null;

                if (this.targetSearchCooldown <= 0) {
                    this.targetSearchCooldown = 10;
                    target = this.findBestRoyalTarget(owner);
                    this.wolf.setTarget(target);
                }
            }

            if (target != null && target.isAlive()) {
                this.rallyPackAgainstTarget(target, owner);
                this.moveAndAttackTarget(target);
                return;
            }

            this.holdRoyalPosition(owner);
        }

        private void tickCooldowns() {
            if (this.targetSearchCooldown > 0) {
                this.targetSearchCooldown--;
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            if (this.auraCooldown > 0) {
                this.auraCooldown--;
            }

            if (this.rallyCooldown > 0) {
                this.rallyCooldown--;
            }

            if (this.repathCooldown > 0) {
                this.repathCooldown--;
            }

            if (this.intimidationCooldown > 0) {
                this.intimidationCooldown--;
            }
        }

        private void applyRoyalAura(LivingEntity owner) {
            if (this.auraCooldown > 0) {
                return;
            }

            this.auraCooldown = 100;

            AABB area = owner.getBoundingBox().inflate(10.0D);

            List<WolfismWolfEntity> alliedWolves = this.wolf.level().getEntitiesOfClass(
                    WolfismWolfEntity.class,
                    area,
                    ally -> ally.isAlive()
                            && ally.isTame()
                            && ally.getOwnerUUID() != null
                            && ally.getOwnerUUID().equals(this.wolf.getOwnerUUID())
            );

            /*
             * Owner receives lighter protection.
             * The pack receives stronger combat leadership.
             */
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, true));

            for (WolfismWolfEntity ally : alliedWolves) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 90, 0, false, true));
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 90, 0, false, true));

                if (this.wolf.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.ENCHANT,
                            ally.getX(),
                            ally.getY() + 0.7D,
                            ally.getZ(),
                            6,
                            0.35D,
                            0.35D,
                            0.35D,
                            0.02D
                    );
                }
            }
        }

        private void applyRoyalIntimidation(LivingEntity owner) {
            if (this.intimidationCooldown > 0) {
                return;
            }

            this.intimidationCooldown = 60;

            AABB area = owner.getBoundingBox().inflate(this.commandRange);

            List<LivingEntity> enemies = this.wolf.level().getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    target -> this.isValidRoyalTarget(target, owner)
            );

            for (LivingEntity enemy : enemies) {
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));

                /*
                 * Close enemies are pressured harder by the King's presence.
                 */
                if (enemy.distanceToSqr(owner) <= 36.0D) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, true));
                }
            }
        }

        private LivingEntity findBestRoyalTarget(LivingEntity owner) {
            AABB area = owner.getBoundingBox().inflate(this.commandRange);

            List<LivingEntity> targets = this.wolf.level().getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    target -> this.isValidRoyalTarget(target, owner)
            );

            return targets.stream()
                    .min(Comparator
                            .comparingDouble((LivingEntity target) -> this.getRoyalPriorityScore(target, owner))
                            .thenComparingDouble(this.wolf::distanceToSqr))
                    .orElse(null);
        }

        private boolean isValidRoyalTarget(LivingEntity target, LivingEntity owner) {
            if (target == null || !target.isAlive()) {
                return false;
            }

            if (target == this.wolf || target == owner) {
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

            return target.distanceToSqr(owner) <= this.commandRange * this.commandRange;
        }

        /*
         * Lower score = higher priority.
         *
         * Wolf King prefers:
         * 1. monsters targeting owner
         * 2. monsters targeting owned wolves
         * 3. monsters close to owner
         * 4. monsters close to Wolf King
         */
        private double getRoyalPriorityScore(LivingEntity target, LivingEntity owner) {
            double score = target.distanceToSqr(owner);

            if (target instanceof Monster monster) {
                LivingEntity monsterTarget = monster.getTarget();

                if (monsterTarget == owner) {
                    score -= 350.0D;
                }

                if (monsterTarget instanceof TamableAnimal tamable
                        && tamable.isTame()
                        && tamable.getOwnerUUID() != null
                        && Objects.equals(tamable.getOwnerUUID(), this.wolf.getOwnerUUID())) {
                    score -= 240.0D;
                }

                if (monsterTarget == this.wolf) {
                    score -= 170.0D;
                }
            }

            if (target.distanceToSqr(owner) <= 25.0D) {
                score -= 120.0D;
            }

            return score;
        }

        private void rallyPackAgainstTarget(LivingEntity target, LivingEntity owner) {
            if (this.rallyCooldown > 0) {
                return;
            }

            this.rallyCooldown = 30;

            AABB area = owner.getBoundingBox().inflate(14.0D);

            List<WolfismWolfEntity> alliedWolves = this.wolf.level().getEntitiesOfClass(
                    WolfismWolfEntity.class,
                    area,
                    ally -> ally.isAlive()
                            && ally.isTame()
                            && ally.getOwnerUUID() != null
                            && ally.getOwnerUUID().equals(this.wolf.getOwnerUUID())
                            && ally != this.wolf
                            && ally.canUseCommandCombat()
            );

            for (WolfismWolfEntity ally : alliedWolves) {
                /*
                 * Do not break HOLD command.
                 * canUseCommandCombat() already blocks HOLD/sitting wolves.
                 */
                ally.setTarget(target);
            }

            if (this.wolf.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.ENCHANT,
                        this.wolf.getX(),
                        this.wolf.getY() + 0.9D,
                        this.wolf.getZ(),
                        12,
                        0.5D,
                        0.45D,
                        0.5D,
                        0.03D
                );
            }
        }

        private void moveAndAttackTarget(LivingEntity target) {
            this.wolf.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (this.repathCooldown <= 0 || this.wolf.getNavigation().isDone()) {
                this.repathCooldown = 8;
                this.wolf.getNavigation().moveTo(target, this.speedModifier);
            }

            if (this.wolf.distanceToSqr(target) <= this.getAttackReachSqr(target)
                    && this.attackCooldown <= 0) {
                this.attackCooldown = 20;
                this.wolf.doHurtTarget(target);
            }
        }

        private void holdRoyalPosition(LivingEntity owner) {
            double distanceToOwner = this.wolf.distanceToSqr(owner);

            /*
             * Wolf King should stay close enough to lead the pack,
             * but not cling like a passive support wolf.
             */
            if (distanceToOwner > 64.0D) {
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
            double attackReach = this.wolf.getBbWidth() * 2.5D + target.getBbWidth();
            return attackReach * attackReach;
        }
    }
}