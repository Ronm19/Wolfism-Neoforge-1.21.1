package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import com.ronm19.wolfism.entity.custom.neutral.ArcticWolfEntity;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.NotNull;

public class ShadowWolfEntity extends WolfismWolfEntity {

    private int shadowRepositionCooldown = 0;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    public ShadowWolfEntity(EntityType<? extends WolfismWolfEntity > entityType, Level level) {
        super(entityType, level);

        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    protected void defineSynchedData( SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
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



    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new ShadowWolfStalkGoal(this));
        this.goalSelector.addGoal(4, new ShadowWolfAttackGoal(this, 1.25D, true));

        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.1D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Monster.class, true, target -> true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            if (this.shadowRepositionCooldown > 0) {
                this.shadowRepositionCooldown--;
            }

            if (this.isInDarkness()) {
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false));
            }
        }
    }

    public boolean isInDarkness() {
        BlockPos pos = this.blockPosition();

        int skyLight = this.level().getBrightness(LightLayer.SKY, pos);
        int blockLight = this.level().getBrightness(LightLayer.BLOCK, pos);

        return this.level().isNight() || (skyLight <= 7 && blockLight <= 7);
    }

    public boolean shouldGlowEyes() {
        long time = this.level().getDayTime() % 24000L;

        boolean isNightTime = time >= 13000L && time <= 23000L;

        int skyLight = this.level().getBrightness(LightLayer.SKY, this.blockPosition());
        int blockLight = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition());

        boolean isActuallyDarkArea = skyLight <= 4 && blockLight <= 8;

        boolean noSkyDimensionDark = !this.level().dimensionType().hasSkyLight()
                && blockLight <= 8;

        return isNightTime || isActuallyDarkArea || noSkyDimensionDark;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (!this.level().isClientSide && hurt && this.isInDarkness() && this.shadowRepositionCooldown <= 0) {
            LivingEntity attacker = null;

            if (source.getEntity() instanceof LivingEntity living) {
                attacker = living;
            }

            if (attacker != null) {
                this.shadowReposition(attacker);
                this.shadowRepositionCooldown = 120;
            }
        }

        return hurt;
    }

    private void shadowReposition(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        RandomSource random = this.getRandom();

        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 5.0D + random.nextDouble() * 4.0D;

            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            double y = target.getY();

            BlockPos pos = BlockPos.containing(x, y, z);

            if (serverLevel.getBlockState(pos.below()).isSolidRender(serverLevel, pos.below())
                    && serverLevel.getBlockState(pos).isAir()
                    && serverLevel.getBlockState(pos.above()).isAir()) {

                this.teleportTo(x, y, z);
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.45F, 0.75F);
                break;
            }
        }
    }

    @javax.annotation.Nullable
    public ShadowWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        ShadowWolfEntity wolf = (ShadowWolfEntity) ModEntities.SHADOW_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof ShadowWolfEntity wolf1) {
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
        ItemStack stack = player.getItemInHand(hand);

        if (!this.level().isClientSide) {
            if (!this.isTame()) {
                if (stack.is(Items.BONE) || stack.is(ModItems.SHADOW_BONE.get())) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }

                    boolean shadowBone = stack.is(ModItems.SHADOW_BONE.get());
                    int tameChance = shadowBone ? 2 : 4;

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
            }
        }

        return super.mobInteract(player, hand);
    }

    public static boolean canSpawn(
            EntityType<ShadowWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    private static class ShadowWolfStalkGoal extends Goal {
        private final ShadowWolfEntity wolf;
        private LivingEntity target;

        public ShadowWolfStalkGoal(ShadowWolfEntity wolf) {
            this.wolf = wolf;
        }

        @Override
        public boolean canUse() {
            if (this.wolf.isTame()) return false;
            if (!this.wolf.isInDarkness()) return false;

            LivingEntity target = this.wolf.getTarget();
            if (target == null || !target.isAlive()) return false;

            double distance = this.wolf.distanceToSqr(target);
            return distance > 16.0D && distance < 225.0D;
        }

        @Override
        public void start() {
            this.target = this.wolf.getTarget();
        }

        @Override
        public void tick() {
            if (this.target == null) return;

            this.wolf.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

            double distance = this.wolf.distanceToSqr(this.target);

            if (distance > 36.0D) {
                this.wolf.getNavigation().moveTo(this.target, 0.85D);
            } else {
                this.wolf.getNavigation().stop();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null
                    && this.target.isAlive()
                    && this.wolf.isInDarkness()
                    && !this.wolf.isTame()
                    && this.wolf.distanceToSqr(this.target) < 225.0D;
        }
    }

    private static class ShadowWolfAttackGoal extends MeleeAttackGoal {
        private final ShadowWolfEntity wolf;

        public ShadowWolfAttackGoal(ShadowWolfEntity wolf, double speedModifier, boolean followingTargetEvenIfNotSeen) {
            super(wolf, speedModifier, followingTargetEvenIfNotSeen);
            this.wolf = wolf;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            double distance = this.wolf.distanceToSqr(target);
            double attackReach = this.wolf.getBbWidth() * 2.0F * this.wolf.getBbWidth() * 2.0F + target.getBbWidth();

            if (distance <= attackReach) {
                this.resetAttackCooldown();
                this.wolf.doHurtTarget(target);

                if (this.wolf.isInDarkness() && this.wolf.shadowRepositionCooldown <= 0) {
                    this.wolf.shadowReposition(target);
                    this.wolf.shadowRepositionCooldown = 80;
                }
            }
        }
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
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(ShadowWolfEntity.class, EntityDataSerializers.INT);
    }
}