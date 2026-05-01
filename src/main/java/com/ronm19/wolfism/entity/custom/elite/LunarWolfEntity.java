package com.ronm19.wolfism.entity.custom.elite;

import com.ronm19.wolfism.entity.ModEntities;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class LunarWolfEntity extends Wolf {
    private int ownerBuffCooldown = 0;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(LunarWolfEntity.class, EntityDataSerializers.INT);

    public LunarWolfEntity( EntityType<? extends Wolf> entityType, Level level ) {
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
        this.goalSelector.addGoal(3, new LunarHowlGoal(this));
    }

    // ===== DATA =====
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.WHITE.getId());
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
}