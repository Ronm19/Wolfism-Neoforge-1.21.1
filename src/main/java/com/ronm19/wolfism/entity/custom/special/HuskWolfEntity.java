package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HuskWolfEntity extends WolfismWolfEntity {
    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    public HuskWolfEntity( EntityType<? extends HuskWolfEntity> entityType, Level level ) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return WolfismWolfEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    @Override
    protected void defineSynchedData( SynchedEntityData.Builder builder ) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.YELLOW.getId());

    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Wild Husk Wolves are hostile before taming.
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
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand ) {
        ItemStack stack = player.getItemInHand(hand);

        // For now: Rotten Flesh.
        // Later we can swap this to ModItems.DRIED_FLESH.get().
        if (!this.isTame() && stack.is(Items.ROTTEN_FLESH)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // 33% tame chance
                if (this.getRandom().nextInt(3) == 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7); // hearts
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6); // smoke
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood( ItemStack stack ) {
        return stack.is(Items.ROTTEN_FLESH);
    }

    @Override
    public boolean doHurtTarget( @NotNull Entity target ) {
        boolean didHit = super.doHurtTarget(target);

        if (didHit && target instanceof LivingEntity livingTarget) {
            applyDryBite(livingTarget);
        }

        return didHit;
    }

    private void applyDryBite( LivingEntity target ) {
        RandomSource random = this.getRandom();

        // 20% chance: Hunger
        if (random.nextFloat() < 0.20F) {
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0), this);
        }

        // 15% chance: Slowness
        if (random.nextFloat() < 0.15F) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
        }

        // 5% chance: Weakness
        if (random.nextFloat() < 0.05F) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
        }
    }

    @Override
    public boolean canBeAffected( MobEffectInstance effect ) {
        // Husk Wolf is a dried undead desert creature.
        // It should not care about Hunger or Poison.
        if (effect.getEffect() == MobEffects.HUNGER || effect.getEffect() == MobEffects.POISON) {
            return false;
        }

        return super.canBeAffected(effect);
    }

    public @NotNull DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public boolean hasArmor() {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR);
    }

    private void setCollarColor( DyeColor collarColor ) {
        this.entityData.set(DATA_COLLAR_COLOR, collarColor.getId());
    }

    public boolean shouldEyesGlow() {
        if (this.isInvisible()) {
            return false;
        }

        long time = this.level().getDayTime() % 24000L;

        boolean isNight = time >= 13000L && time <= 23000L;
        boolean darkEnough = this.level().getRawBrightness(this.blockPosition(), 0) <= 7;

        return isNight || darkEnough;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Husk Wolf does NOT burn in sunlight.
        // Unlike Zombie Wolf, there is no sunlight-burning logic here.

        // Sand Stride: small speed boost while standing on sand-like blocks.
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            BlockPos below = this.blockPosition().below();

            boolean onSand =
                    this.level().getBlockState(below).is(net.minecraft.world.level.block.Blocks.SAND)
                            || this.level().getBlockState(below).is(net.minecraft.world.level.block.Blocks.RED_SAND)
                            || this.level().getBlockState(below).is(net.minecraft.world.level.block.Blocks.SUSPICIOUS_SAND);

            if (onSand) {
                this.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED,
                        40,
                        0,
                        true,
                        false,
                        false
                ));
            }
        }
    }

    @Override
    public @Nullable HuskWolfEntity getBreedOffspring( @NotNull ServerLevel level, @NotNull AgeableMob otherParent ) {
        HuskWolfEntity wolf = ModEntities.HUSK_WOLF.get().create(level);

        if (wolf != null && otherParent instanceof HuskWolfEntity other) {
            HuskWolfEntity ownerParent = this.isTame() ? this : other.isTame() ? other : null;

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
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(HuskWolfEntity.class, EntityDataSerializers.INT);
    }

    public static boolean canSpawn(
            EntityType<HuskWolfEntity> type,
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

        return validGround && hasSpace;
    }
}