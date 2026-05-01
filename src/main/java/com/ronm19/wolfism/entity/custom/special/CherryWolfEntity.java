package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.neutral.ArcticWolfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class CherryWolfEntity extends Wolf {

    private static final int BLOSSOM_SURGE_COOLDOWN = 30 * 20;
    private static final int BLOSSOM_SURGE_ACTIVE_TIME = 7 * 20;

    private static final int CHERRY_VIOLET_COMBO_COOLDOWN = 45 * 20;
    private static final int CHERRY_VIOLET_COMBO_ACTIVE_TIME = 8 * 20;

    private static final double CHERRY_VIOLET_COMBO_RADIUS = 10.0D;
    private static final double CHERRY_VIOLET_COMBO_EFFECT_RADIUS = 8.0D;

    private int cherryVioletComboCooldown = 0;
    private int cherryVioletComboActiveTicks = 0;

    private static final double BLOSSOM_SURGE_RADIUS = 7.0D;
    private static final double OWNER_TRIGGER_RADIUS = 14.0D;

    private int blossomSurgeCooldown = 0;
    private int blossomSurgeActiveTicks = 0;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    public CherryWolfEntity( EntityType<? extends CherryWolfEntity> entityType, Level level ) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    protected void defineSynchedData( SynchedEntityData.Builder builder ) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.PINK.getId());
    }

    public @NotNull DyeColor getCollarColor() {
        return DyeColor.byId((Integer) this.entityData.get(DATA_COLLAR_COLOR));
    }

    public boolean hasArmor() {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR);
    }

    private void setCollarColor( DyeColor collarColor ) {
        this.entityData.set(DATA_COLLAR_COLOR, collarColor.getId());
    }

    @javax.annotation.Nullable
        public CherryWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        CherryWolfEntity wolf = (CherryWolfEntity) ModEntities.CHERRY_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof CherryWolfEntity wolf1) {
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
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 🌸 FLOWER BOOST TAME
        if (!this.isTame() && isFlower(itemstack)) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            if (!this.level().isClientSide) {
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // 🦴 NORMAL BONE TAME
        if (!this.isTame() && itemstack.is(Items.BONE)) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            if (!this.level().isClientSide) {
                if (this.random.nextInt(5) == 0) {
                    this.tame(player);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // ❤️ HEAL
        if (this.isTame() && this.isOwnedBy(player)
                && itemstack.getFoodProperties(this) != null
                && this.getHealth() < this.getMaxHealth()) {

            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            float healAmount = this.isBlossomSurgeActive() ? 6.0F : 4.0F;
            this.heal(healAmount);

            if (this.level() instanceof ServerLevel serverLevel) {
                spawnBlossomParticles(serverLevel, 1.5D, 10);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private boolean isFlower(ItemStack stack) {
        return stack.is(Items.PINK_PETALS)
                || stack.is(ItemTags.FLOWERS)
                || stack.is(Items.CHERRY_LEAVES)
                || stack.is(Items.PEONY)
                || stack.is(Items.PINK_TULIP)
                || stack.is(Items.ALLIUM);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            tickCherryWolfSupport();
        }
    }

    private void tickCherryWolfSupport() {
        if (this.blossomSurgeCooldown > 0) {
            this.blossomSurgeCooldown--;
        }

        if (this.blossomSurgeActiveTicks > 0) {
            this.blossomSurgeActiveTicks--;
        }

        if (this.cherryVioletComboCooldown > 0) {
            this.cherryVioletComboCooldown--;
        }

        if (this.cherryVioletComboActiveTicks > 0) {
            this.cherryVioletComboActiveTicks--;
        }

        if (this.tickCount % 10 == 0 && shouldTriggerBlossomSurge()) {
            triggerBlossomSurge();
        }

        tickCalmPresence();
    }

    private boolean shouldTriggerBlossomSurge() {
        if (!this.isTame()) return false;
        if (this.blossomSurgeCooldown > 0) return false;
        if (this.blossomSurgeActiveTicks > 0) return false;

        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) return false;

        boolean cherryIsInCombat = this.getTarget() != null && this.getTarget().isAlive();

        boolean ownerWasRecentlyHurt =
                owner.hurtTime > 0 &&
                        this.distanceToSqr(owner) <= OWNER_TRIGGER_RADIUS * OWNER_TRIGGER_RADIUS;

        boolean cherryWasRecentlyHurt = this.hurtTime > 0;

        return cherryIsInCombat || ownerWasRecentlyHurt || cherryWasRecentlyHurt;
    }

    private void triggerBlossomSurge() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        this.blossomSurgeCooldown = BLOSSOM_SURGE_COOLDOWN;
        this.blossomSurgeActiveTicks = BLOSSOM_SURGE_ACTIVE_TIME;

        LivingEntity owner = this.getOwner();
        LivingEntity ownerAttacker = owner != null ? owner.getLastHurtByMob() : null;

        this.level().playSound(
                null,
                this.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL,
                0.9F,
                1.75F
        );

        spawnBlossomParticles(serverLevel, BLOSSOM_SURGE_RADIUS, 70);

        tryActivateCherryVioletComboFromCherry(serverLevel, owner);

        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(BLOSSOM_SURGE_RADIUS),
                entity -> entity.isAlive() && entity != this
        );

        applyBlossomBuffs(this);

        for (LivingEntity entity : nearbyEntities) {
            if (owner != null && isFriendlyAlly(entity, owner)) {
                applyBlossomBuffs(entity);
            } else if (shouldAffectAsEnemy(entity, ownerAttacker)) {
                applyBlossomDebuffs(entity);

                if (entity == this.getTarget() || entity == ownerAttacker) {
                    entity.hurt(this.damageSources().mobAttack(this), 2.0F);
                }
            }
        }
    }

    private void tryActivateCherryVioletComboFromCherry(ServerLevel serverLevel, @Nullable LivingEntity owner) {
        if (owner == null || !owner.isAlive()) return;
        if (this.cherryVioletComboCooldown > 0) return;

        List<VioletWolfEntity> nearbyViolets = this.level().getEntitiesOfClass(
                VioletWolfEntity.class,
                this.getBoundingBox().inflate(CHERRY_VIOLET_COMBO_RADIUS),
                this::isValidComboViolet
        );

        if (nearbyViolets.isEmpty()) return;

        VioletWolfEntity violetWolf = nearbyViolets.get(0);
        activateCherryVioletCombo(serverLevel, owner, violetWolf);
    }

    public boolean tryActivateCherryVioletComboFromViolet(VioletWolfEntity violetWolf) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return false;
        if (this.cherryVioletComboCooldown > 0) return false;
        if (!isValidComboViolet(violetWolf)) return false;

        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) return false;

        activateCherryVioletCombo(serverLevel, owner, violetWolf);
        return true;
    }

    private boolean isValidComboViolet(VioletWolfEntity violetWolf) {
        if (violetWolf == null || !violetWolf.isAlive()) return false;
        if (!this.isTame() || !violetWolf.isTame()) return false;
        if (this.getOwnerUUID() == null || violetWolf.getOwnerUUID() == null) return false;
        if (!this.getOwnerUUID().equals(violetWolf.getOwnerUUID())) return false;

        return this.distanceToSqr(violetWolf) <= CHERRY_VIOLET_COMBO_RADIUS * CHERRY_VIOLET_COMBO_RADIUS;
    }

    private void activateCherryVioletCombo(ServerLevel serverLevel, LivingEntity owner, VioletWolfEntity violetWolf) {
        this.cherryVioletComboCooldown = CHERRY_VIOLET_COMBO_COOLDOWN;
        this.cherryVioletComboActiveTicks = CHERRY_VIOLET_COMBO_ACTIVE_TIME;

        this.level().playSound(
                null,
                this.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL,
                1.1F,
                1.35F + this.random.nextFloat() * 0.25F
        );

        spawnCherryVioletComboParticles(serverLevel, this);
        spawnCherryVioletComboParticles(serverLevel, violetWolf);

        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(CHERRY_VIOLET_COMBO_EFFECT_RADIUS),
                entity -> entity.isAlive() && entity != this
        );

        applyCherryVioletComboBuff(this);
        applyCherryVioletComboBuff(violetWolf);

        for (LivingEntity entity : nearbyEntities) {
            if (isCherryVioletComboAlly(entity, owner)) {
                applyCherryVioletComboBuff(entity);
            } else if (isCherryVioletComboEnemy(entity, owner, violetWolf)) {
                applyCherryVioletComboDebuff(entity);
                entity.hurt(this.damageSources().mobAttack(this), 3.0F);
            }
        }
    }

    private boolean isCherryVioletComboAlly(LivingEntity entity, LivingEntity owner) {
        if (entity == owner) {
            return true;
        }

        if (entity == this) {
            return true;
        }

        if (entity instanceof TamableAnimal tamableAnimal && this.getOwnerUUID() != null) {
            return tamableAnimal.getOwnerUUID() != null
                    && tamableAnimal.getOwnerUUID().equals(this.getOwnerUUID());
        }

        return false;
    }

    private boolean isCherryVioletComboEnemy(LivingEntity entity, LivingEntity owner, VioletWolfEntity violetWolf) {
        if (isCherryVioletComboAlly(entity, owner)) {
            return false;
        }

        if (entity instanceof Player) {
            return false;
        }

        if (entity == this.getTarget()) {
            return true;
        }

        if (entity == violetWolf.getTarget()) {
            return true;
        }

        LivingEntity ownerAttacker = owner.getLastHurtByMob();
        if (ownerAttacker != null && entity == ownerAttacker) {
            return true;
        }

        return entity instanceof Enemy;
    }

    private void applyCherryVioletComboBuff(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                6 * 20,
                0,
                false,
                true,
                true
        ));

        entity.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                5 * 20,
                0,
                false,
                true,
                true
        ));

        entity.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                8 * 20,
                0,
                false,
                true,
                true
        ));

        entity.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION,
                6 * 20,
                0,
                false,
                true,
                true
        ));
    }

    private void applyCherryVioletComboDebuff(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                6 * 20,
                0,
                false,
                true,
                true
        ));

        entity.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                6 * 20,
                0,
                false,
                true,
                true
        ));
    }

    private void spawnCherryVioletComboParticles(ServerLevel serverLevel, LivingEntity center) {
        serverLevel.sendParticles(
                ParticleTypes.CHERRY_LEAVES,
                center.getX(),
                center.getY() + 0.65D,
                center.getZ(),
                45,
                2.2D,
                0.45D,
                2.2D,
                0.02D
        );

        serverLevel.sendParticles(
                ParticleTypes.WITCH,
                center.getX(),
                center.getY() + 0.55D,
                center.getZ(),
                28,
                1.8D,
                0.35D,
                1.8D,
                0.02D
        );

        serverLevel.sendParticles(
                ParticleTypes.PORTAL,
                center.getX(),
                center.getY() + 0.45D,
                center.getZ(),
                18,
                1.4D,
                0.25D,
                1.4D,
                0.04D
        );
    }

    public boolean isCherryVioletComboActive() {
        return this.cherryVioletComboActiveTicks > 0;
    }

    private void applyBlossomBuffs( LivingEntity entity ) {
        entity.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                8 * 20,
                0,
                false,
                true,
                true
        ));

        entity.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                4 * 20,
                0,
                false,
                true,
                true
        ));

        entity.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION,
                6 * 20,
                0,
                false,
                true,
                true
        ));
    }

    private void applyBlossomDebuffs( LivingEntity entity ) {
        entity.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                5 * 20,
                0,
                false,
                true,
                true
        ));

        entity.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                5 * 20,
                0,
                false,
                true,
                true
        ));
    }

    private boolean isFriendlyAlly( LivingEntity entity, LivingEntity owner ) {
        if (entity == owner) {
            return true;
        }

        if (entity instanceof Wolf wolf && wolf.isTame()) {
            LivingEntity wolfOwner = wolf.getOwner();
            return wolfOwner != null && wolfOwner == owner;
        }

        return false;
    }

    private boolean shouldAffectAsEnemy( LivingEntity entity, @Nullable LivingEntity ownerAttacker ) {
        if (entity == this.getOwner()) return false;
        if (entity == this) return false;

        if (entity == this.getTarget()) {
            return true;
        }

        if (ownerAttacker != null && entity == ownerAttacker) {
            return true;
        }

        return entity instanceof Enemy;
    }

    private void tickCalmPresence() {
        if (!this.isTame()) return;
        if (this.blossomSurgeActiveTicks > 0) return;
        if (this.getTarget() != null) return;
        if (this.tickCount % (5 * 20) != 0) return;

        LivingEntity owner = this.getOwner();
        if (!(owner instanceof Player player)) return;
        if (!player.isAlive()) return;

        if (this.distanceToSqr(player) > 6.0D * 6.0D) return;

        if (player.getHealth() < player.getMaxHealth()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    3 * 20,
                    0,
                    false,
                    true,
                    true
            ));

            if (this.level() instanceof ServerLevel serverLevel) {
                spawnBlossomParticles(serverLevel, 2.2D, 14);
            }
        }
    }

    private void spawnBlossomParticles( ServerLevel serverLevel, double radius, int count ) {
        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = this.random.nextDouble() * radius;

            double x = this.getX() + Math.cos(angle) * distance;
            double y = this.getY() + 0.35D + this.random.nextDouble() * 1.35D;
            double z = this.getZ() + Math.sin(angle) * distance;

            serverLevel.sendParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    x,
                    y,
                    z,
                    1,
                    0.02D,
                    0.04D,
                    0.02D,
                    0.0D
            );
        }
    }

    @Override
    public boolean hurt( net.minecraft.world.damagesource.DamageSource source, float amount ) {
        boolean hurt = super.hurt(source, amount);

        if (hurt && !this.level().isClientSide && this.isTame() && this.blossomSurgeCooldown <= 0) {
            triggerBlossomSurge();
        }

        return hurt;
    }

    public boolean isBlossomSurgeActive() {
        return this.blossomSurgeActiveTicks > 0;
    }

    public boolean shouldGlowEyes() {
        if (this.isCherryVioletComboActive()) {
            return true;
        }

        if (this.isBlossomSurgeActive()) {
            return true;
        }

        if (this.getTarget() != null) {
            return true;
        }

        return this.level().isNight();
    }

    public float getEyeGlowAlpha() {
        if (this.isCherryVioletComboActive()) {
            return 1.0F;
        }

        if (this.isBlossomSurgeActive()) {
            return 1.0F;
        }

        if (this.getTarget() != null) {
            return 0.85F;
        }

        if (this.level().isNight()) {
            return 0.55F;
        }

        return 0.0F;
    }

    public static boolean canSpawn(
            EntityType<CherryWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    public void addAdditionalSaveData( @NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("CollarColor", (byte) this.getCollarColor().getId());
        compound.putInt("CherryVioletComboCooldown", this.cherryVioletComboCooldown);
        compound.putInt("CherryVioletComboActiveTicks", this.cherryVioletComboActiveTicks);
        this.addPersistentAngerSaveData(compound);
    }

    public void readAdditionalSaveData( @NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.cherryVioletComboCooldown = compound.getInt("CherryVioletComboCooldown");
        this.cherryVioletComboActiveTicks = compound.getInt("CherryVioletComboActiveTicks");

        if (compound.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(compound.getInt("CollarColor")));

        }

        this.readPersistentAngerSaveData(this.level(), compound);
    }
    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(CherryWolfEntity.class, EntityDataSerializers.INT);
    }
}