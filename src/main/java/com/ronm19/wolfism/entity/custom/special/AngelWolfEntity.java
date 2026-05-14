package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.command.WolfismCommand;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import com.ronm19.wolfism.entity.custom.neutral.ArcticWolfEntity;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class AngelWolfEntity extends WolfismWolfEntity {
    private static final int OWNER_CLEANSE_COOLDOWN_MAX = 600; // 30 seconds
    private static final int OWNER_HEAL_COOLDOWN_MAX = 140;    // 7 seconds
    private static final int SELF_HEAL_COOLDOWN_MAX = 180;     // 9 seconds

    private int ownerCleanseCooldown;
    private int ownerHealCooldown;
    private int selfHealCooldown;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    public AngelWolfEntity( EntityType<? extends WolfismWolfEntity > entityType, Level level ) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    protected void defineSynchedData( SynchedEntityData.Builder builder ) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.YELLOW.getId());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AngelWolfProtectCommandGoal(this, 1.25D, 18.0D));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));

        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25D, true));

        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.25D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new BreedGoal(this, 1.0D));

        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new BegGoal(this, 8.0F));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));

        // Angel Wolf does not hunt passive animals.
        // She focuses on undead enemies and owner protection.
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(
                this,
                Monster.class,
                10,
                true,
                false,
                target -> target != null && this.isUndeadTarget(target)
        ));
    }

    private boolean isUndeadTarget(LivingEntity target) {
        return target instanceof Zombie
                || target instanceof AbstractSkeleton
                || target instanceof Phantom
                || target instanceof WitherBoss
                || target instanceof SkeletonHorse
                || target instanceof ZombieHorse
                || target instanceof Zoglin;
    }

    @Override
    public boolean supportsCommand(WolfismCommand command) {
        return command == WolfismCommand.PROTECT || super.supportsCommand(command);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            spawnClientParticles();
            return;
        }

        tickCooldowns();

        if (this.tickCount % 20 == 0) {
            handleOwnerSupport();
            handleSelfHealing();
        }
    }

    private void tickCooldowns() {
        if (this.ownerCleanseCooldown > 0) {
            this.ownerCleanseCooldown--;
        }

        if (this.ownerHealCooldown > 0) {
            this.ownerHealCooldown--;
        }

        if (this.selfHealCooldown > 0) {
            this.selfHealCooldown--;
        }
    }

    private void handleOwnerSupport() {
        if (!this.isTame()) {
            return;
        }

        LivingEntity owner = this.getOwner();

        if (owner == null || !owner.isAlive()) {
            return;
        }

        if (this.distanceToSqr(owner) > 256.0D) {
            return;
        }

        cleanseOwner(owner);
        healOwner(owner);
        grantDarkVision(owner);
    }

    private void cleanseOwner( LivingEntity owner ) {
        if (this.ownerCleanseCooldown > 0) {
            return;
        }

        boolean removedHarmfulEffect = false;

        for (MobEffectInstance effectInstance : new ArrayList<>(owner.getActiveEffects())) {
            Holder<MobEffect> effect = effectInstance.getEffect();

            if (effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                owner.removeEffect(effect);
                removedHarmfulEffect = true;
            }
        }

        if (!removedHarmfulEffect) {
            return;
        }

        owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true, true));
        this.ownerCleanseCooldown = OWNER_CLEANSE_COOLDOWN_MAX;

        sendParticles(owner, ParticleTypes.END_ROD, 18);
        sendParticles(this, ParticleTypes.HAPPY_VILLAGER, 8);
    }

    private void healOwner( LivingEntity owner ) {
        if (this.ownerHealCooldown > 0) {
            return;
        }

        if (owner.getHealth() >= owner.getMaxHealth() - 2.0F) {
            return;
        }

        owner.heal(2.0F);
        owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true, true));

        this.ownerHealCooldown = OWNER_HEAL_COOLDOWN_MAX;
        sendParticles(owner, ParticleTypes.HAPPY_VILLAGER, 10);
    }

    private void grantDarkVision( LivingEntity owner ) {
        boolean shouldHelpVision = this.level().isNight()
                || this.level().getMaxLocalRawBrightness(owner.blockPosition()) <= 7;

        if (!shouldHelpVision) {
            return;
        }

        MobEffectInstance current = owner.getEffect(MobEffects.NIGHT_VISION);

        if (current == null || current.getDuration() < 220) {
            owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, true));
        }
    }

    private void handleSelfHealing() {
        if (this.selfHealCooldown > 0) {
            return;
        }

        if (this.getHealth() >= this.getMaxHealth()) {
            return;
        }

        this.heal(1.5F);
        this.selfHealCooldown = SELF_HEAL_COOLDOWN_MAX;

        sendParticles(this, ParticleTypes.HAPPY_VILLAGER, 6);
    }

    private void spawnClientParticles() {
        if (this.random.nextFloat() > 0.16F) {
            return;
        }

        boolean strongerGlow = this.level().isNight()
                || this.level().getMaxLocalRawBrightness(this.blockPosition()) <= 7;

        if (!strongerGlow && this.random.nextFloat() > 0.35F) {
            return;
        }

        this.level().addParticle(
                ParticleTypes.END_ROD,
                this.getX() + (this.random.nextDouble() - 0.5D) * 0.45D,
                this.getY() + 0.65D + this.random.nextDouble() * 0.35D,
                this.getZ() + (this.random.nextDouble() - 0.5D) * 0.45D,
                0.0D,
                0.015D,
                0.0D
        );
    }

    private void sendParticles( LivingEntity target, ParticleOptions particle, int count ) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                particle,
                target.getX(),
                target.getY() + 0.8D,
                target.getZ(),
                count,
                0.35D,
                0.35D,
                0.35D,
                0.03D
        );
    }

    @Override
    public boolean doHurtTarget( Entity target ) {
        boolean hurt = super.doHurtTarget(target);

        if (hurt && target instanceof LivingEntity livingTarget) {
            if (this.isUndeadTarget(livingTarget)) {
                livingTarget.hurt(this.damageSources().mobAttack(this), 3.0F);
                sendParticles(livingTarget, ParticleTypes.END_ROD, 8);
            }
        }

        return hurt;
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand ) {
        ItemStack stack = player.getItemInHand(hand);

        // Angel Bone = better/special taming item.
        // Regular Bones still work because super.mobInteract handles vanilla wolf taming.
        if (stack.is(ModItems.ANGEL_BONE.get()) && !this.isTame()) {
            if (this.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            // High chance, but not always guaranteed.
            if (this.random.nextInt(3) != 0) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
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

    public boolean shouldGlowEyes() {
        if (this.isInvisible()) {
            return false;
        }

        if (this.getTarget() != null || this.hurtTime > 0) {
            return true;
        }

        long time = this.level().getDayTime() % 24000L;

        boolean isNightTime = time >= 13000L && time <= 23000L;

        int skyLight = this.level().getBrightness(LightLayer.SKY, this.blockPosition());
        int blockLight = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition());

        boolean isActuallyDarkArea = skyLight <= 4 && blockLight <= 8;

        boolean noSkyDimensionDark = !this.level().dimensionType().hasSkyLight()
                && blockLight <= 8;

        return isNightTime || isActuallyDarkArea || noSkyDimensionDark;
    }

    @javax.annotation.Nullable
    public AngelWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        AngelWolfEntity wolf = (AngelWolfEntity) ModEntities.ANGEL_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof AngelWolfEntity wolf1) {
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


    public static boolean canSpawn(
            EntityType<AngelWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public void addAdditionalSaveData( CompoundTag tag ) {
        super.addAdditionalSaveData(tag);
        tag.putByte("CollarColor", (byte) this.getCollarColor().getId());
        tag.putInt("OwnerCleanseCooldown", this.ownerCleanseCooldown);
        tag.putInt("OwnerHealCooldown", this.ownerHealCooldown);
        tag.putInt("SelfHealCooldown", this.selfHealCooldown);

    }

    @Override
    public void readAdditionalSaveData( CompoundTag tag ) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));
            this.ownerCleanseCooldown = tag.getInt("OwnerCleanseCooldown");
            this.ownerHealCooldown = tag.getInt("OwnerHealCooldown");
            this.selfHealCooldown = tag.getInt("SelfHealCooldown");
        }
    }
    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(AngelWolfEntity.class, EntityDataSerializers.INT);
    }

    private static class AngelWolfProtectCommandGoal extends Goal {
        private final AngelWolfEntity wolf;
        private final double speedModifier;
        private final double protectRange;

        private int healCooldown;
        private int targetSearchCooldown;
        private int attackCooldown;

        public AngelWolfProtectCommandGoal(AngelWolfEntity wolf, double speedModifier, double protectRange) {
            this.wolf = wolf;
            this.speedModifier = speedModifier;
            this.protectRange = protectRange;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.wolf.isAlive()
                    && this.wolf.isTame()
                    && !this.wolf.isHoldingCommand()
                    && this.wolf.isInCommand(WolfismCommand.PROTECT)
                    && this.wolf.getOwner() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            LivingEntity owner = this.wolf.getOwner();

            if (owner == null || !owner.isAlive()) {
                this.wolf.getNavigation().stop();
                return;
            }

            if (this.healCooldown > 0) {
                this.healCooldown--;
            }

            if (this.targetSearchCooldown > 0) {
                this.targetSearchCooldown--;
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            this.supportOwnerAndAllies(owner);

            LivingEntity target = this.wolf.getTarget();

            if (target == null || !target.isAlive() || !this.canAngelProtectTarget(target, owner)) {
                if (this.targetSearchCooldown <= 0) {
                    this.targetSearchCooldown = 15;
                    target = this.findBestProtectTarget(owner);
                    this.wolf.setTarget(target);
                }
            }

            if (target != null && target.isAlive() && this.canAngelProtectTarget(target, owner)) {
                this.moveAndAttackTarget(target);
                return;
            }

            this.stayNearOwner(owner);
        }

        private void supportOwnerAndAllies(LivingEntity owner) {
            if (this.healCooldown > 0) {
                return;
            }

            this.healCooldown = 80;

            if (owner.getHealth() < owner.getMaxHealth()) {
                owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));
            }

            AABB area = this.wolf.getBoundingBox().inflate(8.0D);

            List<WolfismWolfEntity> nearbyOwnedWolves = this.wolf.level().getEntitiesOfClass(
                    WolfismWolfEntity.class,
                    area,
                    ally -> ally.isAlive()
                            && ally.isTame()
                            && ally.getOwnerUUID() != null
                            && ally.getOwnerUUID().equals(this.wolf.getOwnerUUID())
                            && ally.getHealth() < ally.getMaxHealth()
            );

            for (WolfismWolfEntity ally : nearbyOwnedWolves) {
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));
            }
        }

        private LivingEntity findBestProtectTarget(LivingEntity owner) {
            AABB area = owner.getBoundingBox().inflate(this.protectRange);

            List<LivingEntity> targets = this.wolf.level().getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    target -> this.canAngelProtectTarget(target, owner)
            );

            return targets.stream()
                    .min(Comparator
                            .comparingDouble((LivingEntity target) -> this.getProtectPriorityScore(target, owner))
                            .thenComparingDouble(this.wolf::distanceToSqr))
                    .orElse(null);
        }

        private boolean canAngelProtectTarget(LivingEntity target, LivingEntity owner) {
            if (target == null || !target.isAlive()) {
                return false;
            }

            if (!(target instanceof Monster)) {
                return false;
            }

            if (!this.wolf.canWolfismTarget(target, false)) {
                return false;
            }

            double distanceToOwner = target.distanceToSqr(owner);

            return distanceToOwner <= this.protectRange * this.protectRange;
        }

        /*
         * Lower score = higher priority.
         *
         * Angel Wolf prefers:
         * 1. monsters targeting owner
         * 2. monsters very close to owner
         * 3. monsters close to herself
         */
        private double getProtectPriorityScore(LivingEntity target, LivingEntity owner) {
            double score = target.distanceToSqr(owner);

            if (target instanceof Monster monster && monster.getTarget() == owner) {
                score -= 200.0D;
            }

            if (target.distanceToSqr(owner) <= 16.0D) {
                score -= 80.0D;
            }

            return score;
        }

        private void moveAndAttackTarget(LivingEntity target) {
            this.wolf.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.wolf.getNavigation().moveTo(target, this.speedModifier);

            if (this.wolf.distanceToSqr(target) <= this.getAttackReachSqr(target)
                    && this.attackCooldown <= 0) {
                this.attackCooldown = 20;
                this.wolf.doHurtTarget(target);
            }
        }

        private void stayNearOwner(LivingEntity owner) {
            double distanceToOwner = this.wolf.distanceToSqr(owner);

            if (distanceToOwner > 36.0D) {
                this.wolf.getNavigation().moveTo(owner, this.speedModifier);
            } else {
                this.wolf.getNavigation().stop();
                this.wolf.getLookControl().setLookAt(owner, 20.0F, 20.0F);
            }
        }

        private double getAttackReachSqr(LivingEntity target) {
            double attackReach = this.wolf.getBbWidth() * 2.2D + target.getBbWidth();
            return attackReach * attackReach;
        }
    }
}