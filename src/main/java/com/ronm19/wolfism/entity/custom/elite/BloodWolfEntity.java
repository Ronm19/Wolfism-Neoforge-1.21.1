package com.ronm19.wolfism.entity.custom.elite;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.client.renderer.BloodWolfRenderer;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BloodWolfEntity extends Wolf {
    private static final EntityDataAccessor<Boolean> BLOOD_FRENZY =
            SynchedEntityData.defineId(BloodWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(BloodWolfEntity.class, EntityDataSerializers.INT);




    private int bloodFrenzyTicks;
    private int ownerProtectionCooldown;

    public BloodWolfEntity(EntityType<? extends Wolf> entityType, Level level) {
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
        this.goalSelector.addGoal(1, new FloatGoal(this));
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
}