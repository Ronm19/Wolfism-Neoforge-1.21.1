package com.ronm19.wolfism.entity.custom.elemental;

import com.ronm19.wolfism.entity.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EarthWolfEntity extends Wolf {

    // ===== DATA =====
    private static final EntityDataAccessor<Integer> TRUST =
            SynchedEntityData.defineId(EarthWolfEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR =
            SynchedEntityData.defineId(EarthWolfEntity.class, EntityDataSerializers.INT);

    // ===== CONSTANTS =====
    private static final int TRUST_PER_BONE = 25;
    private static final int TRUST_TO_TAME = 75;

    public EarthWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    // ===== ATTRIBUTES =====
    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    // ===== DATA =====
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TRUST, 0);
        builder.define(DATA_COLLAR_COLOR, DyeColor.GREEN.getId());
    }

    // ===== SAVE =====
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("EarthTrust", getTrust());
        tag.putByte("CollarColor", (byte) getCollarColor().getId());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("EarthTrust", Tag.TAG_INT)) {
            setTrust(tag.getInt("EarthTrust"));
        }

        if (tag.contains("CollarColor", 99)) {
            setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));
        }
    }

    // ===== TRUST =====
    public int getTrust() {
        return this.entityData.get(TRUST);
    }

    public void setTrust(int value) {
        this.entityData.set(TRUST, Math.max(0, Math.min(100, value)));
    }

    public void addTrust(int amount) {
        setTrust(getTrust() + amount);
    }

    // ===== COLLAR =====
    public DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    private void setCollarColor(DyeColor color) {
        this.entityData.set(DATA_COLLAR_COLOR, color.getId());
    }

    // ===== GOALS =====
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    // ===== EARTH PASSIVE =====
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            applyEarthenGrip();
        }
    }

    private void applyEarthenGrip() {
        BlockState state = this.level().getBlockState(this.blockPosition().below());

        double base = this.isTame() ? 0.20D : 0.10D;
        double target = isNaturalBlock(state) ? 0.35D : base;

        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(target);

        if (isNaturalBlock(state) && !this.isAggressive() && this.tickCount % 40 == 0) {
            this.heal(0.5F);
        }
    }

    private boolean isNaturalBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    // ===== TAMING =====
    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        if (this.isTame()) {
            return super.mobInteract(player, hand);
        }

        if (!stack.is(Items.BONE) || this.isAngry()) {
            return super.mobInteract(player, hand);
        }

        if (this.level().isClientSide) {
            return InteractionResult.CONSUME;
        }

        if (!isNaturalBlock(this.level().getBlockState(this.blockPosition().below()))) {
            this.level().broadcastEntityEvent(this, (byte)6);
            return InteractionResult.SUCCESS;
        }

        stack.consume(1, player);

        if (getTrust() >= TRUST_TO_TAME) {
            if (!EventHooks.onAnimalTame(this, player)) {
                this.tame(player);
                this.setHealth(30.0F);
                this.setOrderedToSit(true);
                this.setCollarColor(DyeColor.GREEN);
                this.level().broadcastEntityEvent(this, (byte)7);
            }
        } else {
            addTrust(TRUST_PER_BONE);
            this.level().broadcastEntityEvent(this, (byte)8);
        }

        return InteractionResult.SUCCESS;
    }

    // ===== PACK DEFENSE =====
    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {

            this.level().getEntitiesOfClass(
                    EarthWolfEntity.class,
                    this.getBoundingBox().inflate(10),
                    wolf -> wolf != this && !wolf.isTame()
            ).forEach(wolf -> wolf.setTarget(attacker));
        }

        return super.hurt(source, amount);
    }

    // ===== PARTICLES =====
    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);

        if (id == 8) {
            for (int i = 0; i < 5; i++) {
                this.level().addParticle(
                        ParticleTypes.HAPPY_VILLAGER,
                        this.getX(),
                        this.getY() + 0.5,
                        this.getZ(),
                        0, 0, 0
                );
            }
        }
    }

    // ===== BREEDING =====
    @Override
    public @Nullable EarthWolfEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        EarthWolfEntity wolf = ModEntities.EARTH_WOLF.get().create(level);

        if (wolf != null && otherParent instanceof EarthWolfEntity other) {

            wolf.setVariant(this.random.nextBoolean() ? this.getVariant() : other.getVariant());

            if (this.isTame()) {
                wolf.setOwnerUUID(this.getOwnerUUID());
                wolf.setTame(true, true);
                wolf.setCollarColor(this.random.nextBoolean() ? this.getCollarColor() : other.getCollarColor());
            }
        }

        return wolf;
    }
}