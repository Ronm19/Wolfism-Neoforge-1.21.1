package com.ronm19.wolfism.entity.custom.elite;

import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.item.ModItems;
import org.jetbrains.annotations.NotNull;

public class WitheredWolfEntity extends WolfismWolfEntity {
    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR =
            SynchedEntityData.defineId(WitheredWolfEntity.class, EntityDataSerializers.INT);

    public WitheredWolfEntity(EntityType<? extends WitheredWolfEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WolfismWolfEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.18D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        // Dark collar by default, fits the Withered Wolf.
        builder.define(DATA_COLLAR_COLOR, DyeColor.BLACK.getId());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Wild Withered Wolves are hostile before taming.
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
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame() && isTamingItem(stack)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // Strong final-batch wolf, so it is harder to tame.
                // 20% chance.
                if (this.getRandom().nextInt(5) == 0) {
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

    private boolean isTamingItem(ItemStack stack) {
        return stack.is(ModItems.WITHER_BONE.get());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isTamingItem(stack);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean didHit = super.doHurtTarget(target);

        if (didHit && target instanceof LivingEntity livingTarget) {
            applyWitherBite(livingTarget);
        }

        return didHit;
    }

    private void applyWitherBite(LivingEntity target) {
        RandomSource random = this.getRandom();

        // 20% chance: Wither
        if (random.nextFloat() < 0.20F) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0), this);
        }

        // 10% chance: Weakness
        if (random.nextFloat() < 0.10F) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
        }

        // 5% chance: Slowness
        if (random.nextFloat() < 0.05F) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0), this);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        // Withered Wolf should not care about poison or wither.
        if (effect.getEffect().equals(MobEffects.POISON)
                || effect.getEffect().equals(MobEffects.WITHER)) {
            return false;
        }

        return super.canBeAffected(effect);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Extra safety for fire/lava-style damage.
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }

        return super.hurt(source, amount);
    }

    public boolean shouldEyesGlow() {
        if (this.isInvisible()) {
            return false;
        }

        long time = this.level().getDayTime() % 24000L;
        boolean isNight = time >= 13000L && time <= 23000L;
        boolean darkEnough = this.level().getRawBrightness(this.blockPosition(), 0) <= 7;

        // Nether + darkness + night all fit Withered Wolf.
        return this.level().dimension() == Level.NETHER || isNight || darkEnough;
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

    @javax.annotation.Nullable
    @Override
    public WitheredWolfEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        WitheredWolfEntity wolf = (WitheredWolfEntity) ModEntities.WITHERED_WOLF.get().create(level);

        if (wolf != null && otherParent instanceof WitheredWolfEntity wolf1) {
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

    public static boolean canSpawn(
            EntityType<WitheredWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }

        if (level.getLevel().dimension() != Level.NETHER) {
            return false;
        }

        BlockPos below = pos.below();

        boolean validGround = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);

        boolean hasSpace = level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();

        return validGround && hasSpace;
    }
}