package com.ronm19.wolfism.entity.custom.elite;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.special.EndWolfEntity;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class VoidWolfEntity extends Wolf {
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED =
            SynchedEntityData.defineId(VoidWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;


    private int voidStepCooldown = 0;
    private int collapseHowlCooldown = 0;
    private int pressureTick = 0;
    private int rageTicks = 0;

    private static final int VOID_STEP_COOLDOWN = 100;
    private static final int COLLAPSE_HOWL_COOLDOWN = 240;

    private static final double VOID_PRESSURE_RADIUS = 5.5D;
    private static final double COLLAPSE_HOWL_RADIUS = 5.0D;

    public VoidWolfEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 44.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 36.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ENRAGED, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.PURPLE.getId());

    }

    public boolean isEnraged() {
        return this.entityData.get(DATA_ENRAGED);

    }

    public void setEnraged(boolean enraged) {
        this.entityData.set(DATA_ENRAGED, enraged);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            return;
        }

        if (this.voidStepCooldown > 0) {
            this.voidStepCooldown--;
        }

        if (this.collapseHowlCooldown > 0) {
            this.collapseHowlCooldown--;
        }

        if (this.rageTicks > 0) {
            this.rageTicks--;
        }

        LivingTargetState targetState = this.getLivingTargetState();

        if (targetState.hasTarget()) {
            this.rageTicks = Math.max(this.rageTicks, 60);
            this.setEnraged(true);
        } else {
            this.setEnraged(this.rageTicks > 0);
        }

        this.handleVoidPressure();

        if (targetState.hasTarget()) {
            this.tryVoidStep(targetState.target());
            this.tryCollapseHowl();
        }

        this.tryReturnToOwnerWithVoidStep();
    }

    private LivingTargetState getLivingTargetState() {
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            return new LivingTargetState(this.getTarget());
        }

        return new LivingTargetState(null);
    }

    private void handleVoidPressure() {
        this.pressureTick++;

        if (this.pressureTick < 20) {
            return;
        }

        this.pressureTick = 0;

        AABB area = this.getBoundingBox().inflate(VOID_PRESSURE_RADIUS);
        List<net.minecraft.world.entity.LivingEntity> targets = this.level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                area,
                this::isValidVoidTarget
        );

        if (targets.isEmpty()) {
            return;
        }

        this.rageTicks = Math.max(this.rageTicks, 60);
        this.setEnraged(true);

        for (net.minecraft.world.entity.LivingEntity target : targets) {
            double distance = this.distanceTo(target);

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));

            if (distance <= 3.0D && this.tickCount % 40 == 0) {
                target.hurt(this.damageSources().magic(), 1.5F);
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.PORTAL,
                        target.getX(),
                        target.getY() + 0.7D,
                        target.getZ(),
                        8,
                        0.25D,
                        0.35D,
                        0.25D,
                        0.02D
                );
            }
        }
    }

    private void tryCollapseHowl() {
        if (this.collapseHowlCooldown > 0) {
            return;
        }

        AABB area = this.getBoundingBox().inflate(COLLAPSE_HOWL_RADIUS);
        List<net.minecraft.world.entity.LivingEntity> targets = this.level().getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                area,
                this::isValidVoidTarget
        );

        boolean surrounded = targets.size() >= 3;
        boolean lowHealthPressure = this.getHealth() <= this.getMaxHealth() * 0.35F && !targets.isEmpty();

        if (!surrounded && !lowHealthPressure) {
            return;
        }

        this.collapseHowlCooldown = COLLAPSE_HOWL_COOLDOWN;
        this.rageTicks = 120;
        this.setEnraged(true);

        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));

        for (net.minecraft.world.entity.LivingEntity target : targets) {
            target.hurt(this.damageSources().magic(), 5.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));

            Vec3 pushDirection = target.position().subtract(this.position());

            if (pushDirection.lengthSqr() > 0.001D) {
                pushDirection = pushDirection.normalize().scale(0.55D);
                target.push(pushDirection.x, 0.18D, pushDirection.z);
            }
        }

        this.playSound(SoundEvents.WOLF_GROWL, 1.2F, 0.55F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    this.getX(),
                    this.getY() + 0.6D,
                    this.getZ(),
                    70,
                    1.4D,
                    0.7D,
                    1.4D,
                    0.08D
            );

            serverLevel.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    this.getX(),
                    this.getY() + 0.5D,
                    this.getZ(),
                    45,
                    1.1D,
                    0.5D,
                    1.1D,
                    0.04D
            );
        }
    }

    private void tryVoidStep(net.minecraft.world.entity.LivingEntity target) {
        // ✅ Sitting Void Wolf should not blink around
        if (this.isOrderedToSit() || this.isInSittingPose()) {
            return;
        }

        if (this.voidStepCooldown > 0) {
            return;
        }

        double distance = this.distanceTo(target);

        if (distance < 7.0D && !this.horizontalCollision) {
            return;
        }

        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double offsetX = Mth.cos((float) angle) * 2.2D;
        double offsetZ = Mth.sin((float) angle) * 2.2D;

        BlockPos basePos = BlockPos.containing(
                target.getX() + offsetX,
                target.getY(),
                target.getZ() + offsetZ
        );

        for (int yOffset = -2; yOffset <= 2; yOffset++) {
            BlockPos attemptPos = basePos.offset(0, yOffset, 0);

            if (this.canStandAt(attemptPos)) {
                this.performVoidStep(
                        attemptPos.getX() + 0.5D,
                        attemptPos.getY(),
                        attemptPos.getZ() + 0.5D
                );

                this.voidStepCooldown = VOID_STEP_COOLDOWN;
                return;
            }
        }
    }

    private void tryReturnToOwnerWithVoidStep() {
        if (!this.isTame()) {
            return;
        }

        // ✅ Do NOT teleport to owner while sitting
        if (this.isOrderedToSit() || this.isInSittingPose()) {
            return;
        }

        if (this.voidStepCooldown > 0) {
            return;
        }

        if (this.getOwner() == null) {
            return;
        }

        if (this.distanceToSqr(this.getOwner()) < 22.0D * 22.0D) {
            return;
        }

        BlockPos ownerPos = this.getOwner().blockPosition();

        for (int i = 0; i < 10; i++) {
            int xOffset = this.random.nextInt(7) - 3;
            int zOffset = this.random.nextInt(7) - 3;

            BlockPos attemptPos = ownerPos.offset(xOffset, 0, zOffset);

            for (int yOffset = -2; yOffset <= 2; yOffset++) {
                BlockPos finalPos = attemptPos.offset(0, yOffset, 0);

                if (this.canStandAt(finalPos)) {
                    this.performVoidStep(
                            finalPos.getX() + 0.5D,
                            finalPos.getY(),
                            finalPos.getZ() + 0.5D
                    );

                    this.voidStepCooldown = VOID_STEP_COOLDOWN;
                    return;
                }
            }
        }
    }

    private void performVoidStep(double x, double y, double z) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.PORTAL,
                    this.getX(),
                    this.getY() + 0.5D,
                    this.getZ(),
                    35,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.08D
            );
        }

        this.teleportTo(x, y, z);
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8F, 0.75F);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    this.getX(),
                    this.getY() + 0.5D,
                    this.getZ(),
                    35,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.08D
            );
        }
    }

    private boolean canStandAt(BlockPos pos) {
        BlockState feet = this.level().getBlockState(pos);
        BlockState head = this.level().getBlockState(pos.above());
        BlockState ground = this.level().getBlockState(pos.below());

        return feet.isAir()
                && head.isAir()
                && ground.isSolidRender(this.level(), pos.below());
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean success = super.doHurtTarget(entity);

        if (success && entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            this.rageTicks = 100;
            this.setEnraged(true);

            livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));

            if (this.random.nextFloat() < 0.25F) {
                livingEntity.hurt(this.damageSources().magic(), 2.0F);
            }

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.PORTAL,
                        livingEntity.getX(),
                        livingEntity.getY() + 0.7D,
                        livingEntity.getZ(),
                        12,
                        0.25D,
                        0.35D,
                        0.25D,
                        0.03D
                );
            }
        }

        return success;
    }

    private boolean isValidVoidTarget(net.minecraft.world.entity.LivingEntity livingEntity) {
        if (livingEntity == this) {
            return false;
        }

        if (!livingEntity.isAlive()) {
            return false;
        }

        if (this.isTame()) {
            if (this.getOwner() != null && livingEntity == this.getOwner()) {
                return false;
            }

            if (livingEntity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
                return !Objects.equals(tamableAnimal.getOwnerUUID(), this.getOwnerUUID());
            }
        }

        if (this.isAlliedTo(livingEntity)) {
            return false;
        }

        return livingEntity instanceof Enemy || livingEntity == this.getTarget();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (!this.level().isClientSide && hurt) {
            this.rageTicks = 120;
            this.setEnraged(true);
        }

        return hurt;
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        boolean isVoidBone = stack.is(ModItems.VOID_BONE.get());
        boolean isNormalBone = stack.is(Items.BONE);

        if (!this.isTame() && (isVoidBone || isNormalBone)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                float tameChance = isVoidBone ? 0.65F : 0.18F;

                if (this.random.nextFloat() < tameChance) {
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

    public DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public boolean hasArmor() {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR);
    }

    private void setCollarColor(DyeColor collarColor) {
        this.entityData.set(DATA_COLLAR_COLOR, collarColor.getId());
    }


    @Override
    public @Nullable VoidWolfEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        VoidWolfEntity baby = ModEntities.VOID_WOLF.get().create(level);

        if (baby != null && otherParent instanceof VoidWolfEntity otherVoidWolf) {
            if (this.isTame()) {
                baby.setOwnerUUID(this.getOwnerUUID());
                baby.setTame(true, true);

                if (this.random.nextBoolean()) {
                    baby.setCollarColor(this.getCollarColor());
                } else {
                    baby.setCollarColor(otherVoidWolf.getCollarColor());
                }
            }
        }

        return baby;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("CollarColor", (byte) this.getCollarColor().getId());
        tag.putInt("VoidStepCooldown", this.voidStepCooldown);
        tag.putInt("CollapseHowlCooldown", this.collapseHowlCooldown);
        tag.putInt("RageTicks", this.rageTicks);
        tag.putBoolean("Enraged", this.isEnraged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));

            this.voidStepCooldown = tag.getInt("VoidStepCooldown");
            this.collapseHowlCooldown = tag.getInt("CollapseHowlCooldown");
            this.rageTicks = tag.getInt("RageTicks");
            this.setEnraged(tag.getBoolean("Enraged"));
        }
    }

    public static boolean canSpawn(
            EntityType<VoidWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.END
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }


    private record LivingTargetState(@Nullable net.minecraft.world.entity.LivingEntity target) {
        private boolean hasTarget() {
            return this.target != null && this.target.isAlive();
        }
    }
    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(VoidWolfEntity.class, EntityDataSerializers.INT);
    }
}