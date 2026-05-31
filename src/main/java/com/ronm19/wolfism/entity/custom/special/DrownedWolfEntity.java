package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DrownedWolfEntity extends WolfismWolfEntity {

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;


    public DrownedWolfEntity( EntityType<? extends DrownedWolfEntity> entityType, Level level ) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return WolfismWolfEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.10D);
    }

    @Override
    protected void defineSynchedData( SynchedEntityData.Builder builder ) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.CYAN.getId());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                target -> !this.isTame()
        ));

        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(
                this,
                Villager.class,
                10,
                true,
                false,
                target -> !this.isTame()
        ));

        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(
                this,
                IronGolem.class,
                10,
                true,
                false,
                target -> !this.isTame()
        ));
    }

    @Override
    public InteractionResult mobInteract( Player player, InteractionHand hand ) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame() && isTamingItem(stack)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                if (this.getRandom().nextInt(3) == 0) {
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

    private boolean isTamingItem( ItemStack stack ) {
        return stack.is(Items.ROTTEN_FLESH)
                || stack.is(Items.COD)
                || stack.is(Items.SALMON);
    }

    @Override
    public boolean isFood( ItemStack stack ) {
        return isTamingItem(stack);
    }

    @Override
    public boolean doHurtTarget( Entity target ) {
        boolean didHit = super.doHurtTarget(target);

        if (didHit && target instanceof LivingEntity livingTarget) {
            applyDrownedBite(livingTarget);
        }

        return didHit;
    }

    private void applyDrownedBite( LivingEntity target ) {
        RandomSource random = this.getRandom();

        // 20% chance: Slowness
        if (random.nextFloat() < 0.20F) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
        }

        // 10% chance: Mining Fatigue
        if (random.nextFloat() < 0.10F) {
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 0), this);
        }

        // 5% chance: Weakness
        if (random.nextFloat() < 0.05F) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
        }
    }

    @Override
    public @Nullable DrownedWolfEntity getBreedOffspring( @NotNull ServerLevel level, @NotNull AgeableMob otherParent ) {
        DrownedWolfEntity wolf = ModEntities.DROWNED_WOLF.get().create(level);

        if (wolf != null && otherParent instanceof DrownedWolfEntity other) {
            DrownedWolfEntity ownerParent = this.isTame() ? this : other.isTame() ? other : null;

            if (ownerParent != null) {
                wolf.setOwnerUUID(ownerParent.getOwnerUUID());
                wolf.setTame(true, true);

                wolf.setCollarColor(
                        this.random.nextBoolean()
                                ? this.getCollarColor()
                                : other.getCollarColor()
                );
            }
        }

        return wolf;
    }

    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(DrownedWolfEntity.class, EntityDataSerializers.INT);
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
    protected int decreaseAirSupply( int air ) {
        return this.getMaxAirSupply();
    }

    @Override
    protected int increaseAirSupply( int air ) {
        return this.getMaxAirSupply();
    }

    @Override
    public boolean canBeAffected( MobEffectInstance effect ) {
        if (effect.getEffect().equals(MobEffects.POISON)) {
            return false;
        }

        return super.canBeAffected(effect);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isInWaterOrBubble()) {
            this.setAirSupply(this.getMaxAirSupply());
        }

        if (!this.level().isClientSide && this.tickCount % 20 == 0 && this.isInWaterOrBubble()) {
            this.addEffect(new MobEffectInstance(
                    MobEffects.DOLPHINS_GRACE,
                    40,
                    0,
                    true,
                    false,
                    false
            ));
        }
    }

    public boolean shouldEyesGlow() {
        if (this.isInvisible()) {
            return false;
        }

        long time = this.level().getDayTime() % 24000L;
        boolean isNight = time >= 13000L && time <= 23000L;

        return isNight || this.isInWaterOrBubble();
    }

    public static boolean canSpawn(
            EntityType<DrownedWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }

        if (level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        BlockPos below = pos.below();

        boolean validGround = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);

        boolean hasSpace = level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();

        boolean nearWater = isNearWater(level, pos);

        return validGround && hasSpace && nearWater;
    }

    private static boolean isNearWater(ServerLevelAccessor level, BlockPos pos) {
        for (BlockPos nearbyPos : BlockPos.betweenClosed(
                pos.offset(-5, -1, -5),
                pos.offset(5, 1, 5)
        )) {
            if (level.getBlockState(nearbyPos).is(Blocks.WATER)) {
                return true;
            }
        }

        return false;
    }
}