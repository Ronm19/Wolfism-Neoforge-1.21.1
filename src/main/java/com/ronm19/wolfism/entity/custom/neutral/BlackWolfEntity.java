package com.ronm19.wolfism.entity.custom.neutral;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.ai.custom.black_wolf.BlackWolfWatchPlayerGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BlackWolfEntity extends Wolf {

    private static final EntityDataAccessor<Boolean> NIGHT_ACTIVE =
            SynchedEntityData.defineId(BlackWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;


    public BlackWolfEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
    }

    // ----------------------------
    // DATA
    // ----------------------------


    @Override
    protected void defineSynchedData( SynchedEntityData.Builder builder ) {
        super.defineSynchedData(builder);
        builder.define(NIGHT_ACTIVE, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.YELLOW.getId());
    }

    public boolean isNightActive() {
        return this.entityData.get(NIGHT_ACTIVE);
    }

    private void setNightActive(boolean active) {
        this.entityData.set(NIGHT_ACTIVE, active);
    }

    // ----------------------------
    // ATTRIBUTES
    // ----------------------------

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    // ----------------------------
    // GOALS
    // ----------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));

        // Final identity polish
        this.goalSelector.addGoal(6, new BlackWolfWatchPlayerGoal(this));

        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));

        this.targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Sheep.class, true, null));
        this.targetSelector.addGoal(5, new NonTameRandomTargetGoal<>(this, Rabbit.class, true, null));
        this.targetSelector.addGoal(6, new NonTameRandomTargetGoal<>(this, Chicken.class, true, null));
    }
    // ----------------------------
    // NIGHT SYSTEM (CORE FEATURE)
    // ----------------------------

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.isNightActive()) {

            Player player = this.level().getNearestPlayer(this, 8.0D);

            if (player != null && !this.isTame()) {
                // subtle effect: slow player reaction slightly
                player.setSprinting(false); // tiny pressure feel
            }
        }

        if (!this.level().isClientSide) {
            boolean isNight = this.level().isNight();
            boolean isDark = this.level().getMaxLocalRawBrightness(this.blockPosition()) < 8;

            boolean shouldBeActive = isNight || isDark;

            if (shouldBeActive != this.isNightActive()) {
                setNightActive(shouldBeActive);

                // Apply/remove speed boost
                if (shouldBeActive) {
                    Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED))
                            .setBaseValue(0.36D); // boosted
                } else {
                    Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED))
                            .setBaseValue(0.32D); // normal
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.isNightActive()) {

            LivingEntity target = this.getTarget();

            if (target != null) {

                double distance = this.distanceTo(target);

                // 🔥 If target is escaping → push harder
                if (distance > 6.0D) {
                    this.getNavigation().moveTo(target, 1.4D); // faster chase
                }

                // 🔥 If close → stay aggressive
                if (distance < 4.0D) {
                    this.setSprinting(true);
                }
            }
        }
    }

    // ----------------------------
    // TAMING (Night Bonus)
    // ----------------------------

    @Override
    public @NotNull InteractionResult mobInteract( Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if (!this.level().isClientSide && !this.isTame() && itemstack.is(Items.BONE)) {

            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            boolean isNight = this.level().isNight();

            // 🔥 Night bonus taming chance
            int chance = isNight ? 2 : 3; // 50% at night, ~33% at day

            if (this.random.nextInt(chance) == 0) {
                this.tame(player);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte)7); // hearts
            } else {
                this.level().broadcastEntityEvent(this, (byte)6); // smoke
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

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof Creeper || target instanceof Ghast) {
            return false;
        }

        // 🔥 At night → more aggressive target acceptance
        if (this.isNightActive()) {
            return true;
        }

        return super.wantsToAttack(target, owner);
    }


    // ----------------------------
    // BREEDING
    // ----------------------------

    @javax.annotation.Nullable
    public BlackWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        BlackWolfEntity wolf = (BlackWolfEntity) ModEntities.BLACK_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof BlackWolfEntity wolf1) {
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
    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(BlackWolfEntity.class, EntityDataSerializers.INT);
    }
}