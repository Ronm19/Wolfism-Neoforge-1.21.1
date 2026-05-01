package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VioletWolfEntity extends Wolf {
    private static final EntityDataAccessor<Boolean> DATA_PULSE_ACTIVE =
            SynchedEntityData.defineId(VioletWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    private static final int ARCANE_PULSE_COOLDOWN = 20 * 18;
    private static final int ARCANE_PULSE_ACTIVE_TIME = 20;
    private static final double ARCANE_PULSE_RADIUS = 6.0D;

    private int arcanePulseCooldown = 0;
    private int arcanePulseActiveTicks = 0;

    public VioletWolfEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PULSE_ACTIVE, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.PURPLE.getId());

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

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide()) {
            return;
        }

        if (this.arcanePulseCooldown > 0) {
            this.arcanePulseCooldown--;
        }

        if (this.arcanePulseActiveTicks > 0) {
            this.arcanePulseActiveTicks--;
            this.entityData.set(DATA_PULSE_ACTIVE, true);
        } else {
            this.entityData.set(DATA_PULSE_ACTIVE, false);
        }

        if (this.tickCount % 100 == 0) {
            this.tickComposureAura();
        }

        if (this.arcanePulseCooldown <= 0 && this.hasNearbyEnemy()) {
            this.doArcanePulse();
        }
    }

    private void doArcanePulse() {
        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(ARCANE_PULSE_RADIUS),
                this::isValidPulseTarget
        );

        if (targets.isEmpty()) {
            return;
        }

        this.arcanePulseCooldown = ARCANE_PULSE_COOLDOWN;
        this.arcanePulseActiveTicks = ARCANE_PULSE_ACTIVE_TIME;
        this.entityData.set(DATA_PULSE_ACTIVE, true);

        this.level().playSound(
                null,
                this.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.NEUTRAL,
                0.9F,
                0.75F + this.random.nextFloat() * 0.25F
        );

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.WITCH,
                    this.getX(),
                    this.getY() + 0.55D,
                    this.getZ(),
                    45,
                    2.4D,
                    0.25D,
                    2.4D,
                    0.02D
            );

            serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    this.getX(),
                    this.getY() + 0.35D,
                    this.getZ(),
                    28,
                    1.8D,
                    0.2D,
                    1.8D,
                    0.04D
            );
        }

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
        }

        LivingEntity owner = this.getOwner();
        if (owner != null && owner.distanceToSqr(this) <= 144.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));
        }

        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0));

        tryActivateCherryComboFromViolet(owner);
    }

    private void tryActivateCherryComboFromViolet(LivingEntity owner) {
        if (owner == null || !owner.isAlive()) return;
        if (!this.isTame()) return;
        if (this.getOwnerUUID() == null) return;

        List<CherryWolfEntity> nearbyCherries = this.level().getEntitiesOfClass(
                CherryWolfEntity.class,
                this.getBoundingBox().inflate(10.0D),
                cherryWolf -> cherryWolf.isAlive()
                        && cherryWolf.isTame()
                        && cherryWolf.getOwnerUUID() != null
                        && cherryWolf.getOwnerUUID().equals(this.getOwnerUUID())
        );

        for (CherryWolfEntity cherryWolf : nearbyCherries) {
            if (cherryWolf.tryActivateCherryVioletComboFromViolet(this)) {
                break;
            }
        }
    }

    private void tickComposureAura() {
        if (!this.isTame()) {
            return;
        }

        LivingEntity owner = this.getOwner();

        if (owner != null && owner.distanceToSqr(this) <= 100.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0));
        }

        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0));
    }

    private boolean hasNearbyEnemy() {
        return !this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(ARCANE_PULSE_RADIUS),
                this::isValidPulseTarget
        ).isEmpty();
    }

    private boolean isValidPulseTarget(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        if (entity == this) {
            return false;
        }

        LivingEntity owner = this.getOwner();

        if (owner != null && entity == owner) {
            return false;
        }

        if (entity instanceof Player) {
            return false;
        }

        if (entity instanceof TamableAnimal tamableAnimal && this.isTame()) {
            return tamableAnimal.getOwnerUUID() != null
                    && tamableAnimal.getOwnerUUID().equals(this.getOwnerUUID());
        }

        return entity instanceof Enemy || entity == this.getTarget();
    }

    public boolean isPulseActive() {
        return this.entityData.get(DATA_PULSE_ACTIVE);
    }

    public boolean shouldGlowEyes() {
        if (this.isPulseActive()) {
            return true;
        }

        if (this.getTarget() != null && this.getTarget().isAlive()) {
            return true;
        }

        return !this.level().isDay();
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.level().isClientSide() && !this.isTame() && this.isVioletTamingItem(stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            int tameChance = this.isArcaneBone(stack) ? 2 : 4;

            if (this.random.nextInt(tameChance) == 0) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @javax.annotation.Nullable
    public VioletWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        VioletWolfEntity wolf = (VioletWolfEntity) ModEntities.VIOLET_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof VioletWolfEntity wolf1) {
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

    private boolean isVioletTamingItem(ItemStack stack) {
        return stack.is(Items.BONE) || this.isArcaneBone(stack);
    }

    private boolean isArcaneBone(ItemStack stack) {
        return stack.is(ModItems.ARCANE_BONE.get());
    }

    public static boolean canSpawn(
            EntityType<VioletWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("CollarColor", (byte)this.getCollarColor().getId());
        this.addPersistentAngerSaveData(tag);
        tag.putInt("ArcanePulseCooldown", this.arcanePulseCooldown);
        tag.putInt("ArcanePulseActiveTicks", this.arcanePulseActiveTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.arcanePulseCooldown = tag.getInt("ArcanePulseCooldown");
        this.arcanePulseActiveTicks = tag.getInt("ArcanePulseActiveTicks");
        if (tag.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));
        }

        this.readPersistentAngerSaveData(this.level(), tag);
    }
    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(VioletWolfEntity.class, EntityDataSerializers.INT);
    }
}