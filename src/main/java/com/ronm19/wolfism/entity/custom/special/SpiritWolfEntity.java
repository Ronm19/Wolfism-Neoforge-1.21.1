package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class SpiritWolfEntity extends WolfismWolfEntity {

    private static final int SPIRIT_VEIL_COOLDOWN_TICKS = 40 * 20;
    private static final int SPIRIT_VEIL_DURATION_TICKS = 8 * 20;

    private int spiritVeilCooldown = 0;
    private int spiritVeilTicks = 0;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;


    public SpiritWolfEntity(EntityType<? extends WolfismWolfEntity > entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    protected void defineSynchedData( SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.WHITE.getId());
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
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.35F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.15D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Monster.class, false, this::isUndeadTarget));
    }

    @javax.annotation.Nullable
    public SpiritWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        SpiritWolfEntity wolf = (SpiritWolfEntity) ModEntities.SPIRIT_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof SpiritWolfEntity wolf1) {
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
    public void tick() {
        super.tick();

        if (this.spiritVeilCooldown > 0) {
            this.spiritVeilCooldown--;
        }

        if (this.spiritVeilTicks > 0) {
            this.spiritVeilTicks--;

            if (this.level().isClientSide) {
                spawnSpiritVeilParticles();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (!this.level().isClientSide && hurt && this.isTame()) {
            LivingEntity owner = this.getOwner();

            if (owner != null && this.distanceTo(owner) <= 18.0F) {
                tryActivateSpiritVeil(owner);
            }
        }

        return hurt;
    }

    private void tryActivateSpiritVeil(LivingEntity owner) {
        if (this.spiritVeilCooldown > 0 || this.spiritVeilTicks > 0) {
            return;
        }

        this.spiritVeilCooldown = SPIRIT_VEIL_COOLDOWN_TICKS;
        this.spiritVeilTicks = SPIRIT_VEIL_DURATION_TICKS;

        owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SPIRIT_VEIL_DURATION_TICKS, 0));
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SPIRIT_VEIL_DURATION_TICKS, 0));

        this.level().playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, this.getSoundSource(), 0.8F, 1.35F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.8D, this.getZ(), 18, 0.45D, 0.45D, 0.45D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, owner.getX(), owner.getY() + 1.0D, owner.getZ(), 22, 0.55D, 0.65D, 0.55D, 0.02D);
        }
    }

    private void spawnSpiritVeilParticles() {
        if (this.random.nextFloat() < 0.35F) {
            this.level().addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getRandomX(0.5D),
                    this.getRandomY(),
                    this.getRandomZ(0.5D),
                    0.0D,
                    0.01D,
                    0.0D
            );
        }
    }

    public boolean isSpiritVeilActive() {
        return this.spiritVeilTicks > 0;
    }

    public boolean shouldGlowEyes() {
        if (this.isAngry() || this.isSpiritVeilActive()) {
            return true;
        }

        if (!this.level().isClientSide) {
            return false;
        }

        long time = this.level().getDayTime() % 24000L;
        boolean night = time >= 13000L && time <= 23000L;
        boolean dark = this.level().getMaxLocalRawBrightness(this.blockPosition()) <= 7;

        return night || dark;
    }

    private boolean isUndeadTarget(LivingEntity entity) {
        return entity instanceof Zombie
                || entity instanceof ZombieVillager
                || entity instanceof Husk
                || entity instanceof Drowned
                || entity instanceof AbstractSkeleton
                || entity instanceof Stray
                || entity instanceof WitherSkeleton
                || entity instanceof Phantom;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean result = super.doHurtTarget(target);

        if (result && target instanceof LivingEntity livingTarget) {
            if (isUndeadTarget(livingTarget)) {
                livingTarget.hurt(this.damageSources().mobAttack(this), 2.0F);
            }

            if (this.random.nextFloat() < 0.25F) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 4 * 20, 0));
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.SOUL,
                        livingTarget.getX(),
                        livingTarget.getY() + 0.6D,
                        livingTarget.getZ(),
                        10,
                        0.35D,
                        0.35D,
                        0.35D,
                        0.01D
                );
            }
        }

        return result;
    }

    public float getSpiritGlowIntensity() {
        return this.isSpiritVeilActive() ? 1.0F : 0.55F;
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.level().isClientSide && !this.isTame()) {
            boolean isNormalBone = stack.is(Items.BONE);
            boolean isSpiritBone = stack.is(ModItems.SPIRIT_BONE.get());

            if (isNormalBone || isSpiritBone) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                int tameChance = isSpiritBone ? 2 : 3;

                if (this.random.nextInt(tameChance) == 0) {
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
        }

        return super.mobInteract(player, hand);
    }

    public static boolean canSpawn(
            EntityType<SpiritWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn( @NotNull ServerLevelAccessor level, @NotNull DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    public void addAdditionalSaveData( @NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("CollarColor", (byte)this.getCollarColor().getId());
        this.addPersistentAngerSaveData(compound);
    }

    public void readAdditionalSaveData( @NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(compound.getInt("CollarColor")));
        }

        this.readPersistentAngerSaveData(this.level(), compound);
    }
    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(SpiritWolfEntity.class, EntityDataSerializers.INT);
    }
}