package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import com.ronm19.wolfism.entity.custom.neutral.ArcticWolfEntity;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GoldenWolfEntity extends WolfismWolfEntity  {
    private static final int GOLDEN_COURAGE_CHECK_INTERVAL = 40;
    private static final int GOLDEN_BLESSING_COOLDOWN_TICKS = 20 * 25;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;


    private int goldenBlessingCooldown = 0;

    public GoldenWolfEntity( EntityType<? extends WolfismWolfEntity> entityType, net.minecraft.world.level.Level level) {
        super(entityType, level);
    }


    protected void defineSynchedData( SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.YELLOW.getId());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.GOLD_INGOT);
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
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.goldenBlessingCooldown > 0) {
            this.goldenBlessingCooldown--;
        }

        if (!this.level().isClientSide) {
            handleGoldenCourage();
        } else {
            handleGoldenParticles();
        }
    }

    private void handleGoldenCourage() {
        if (this.tickCount % GOLDEN_COURAGE_CHECK_INTERVAL != 0) {
            return;
        }

        if (!this.isTame()) {
            return;
        }

        LivingEntity owner = this.getOwner();
        LivingEntity target = this.getTarget();

        if (owner == null || target == null) {
            return;
        }

        if (this.distanceToSqr(owner) > 12.0D * 12.0D) {
            return;
        }

        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, true, true));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, true, true));
    }

    private void handleGoldenParticles() {
        if (!this.shouldGlowEyes()) {
            return;
        }

        int chance = this.getTarget() != null ? 5 : 22;

        if (this.random.nextInt(chance) == 0) {
            this.level().addParticle(
                    ParticleTypes.END_ROD,
                    this.getRandomX(0.6D),
                    this.getRandomY() + 0.15D,
                    this.getRandomZ(0.6D),
                    0.0D,
                    0.015D,
                    0.0D
            );
        }
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity killedEntity) {
        boolean result = super.killedEntity(level, killedEntity);
        giveOwnerGoldenBlessing(level);
        return result;
    }

    private void giveOwnerGoldenBlessing(ServerLevel level) {
        if (!this.isTame()) {
            return;
        }

        if (this.goldenBlessingCooldown > 0) {
            return;
        }

        LivingEntity owner = this.getOwner();

        if (owner == null) {
            return;
        }

        if (this.distanceToSqr(owner) > 20.0D * 20.0D) {
            return;
        }

        owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 8, 0, false, true, true));
        this.goldenBlessingCooldown = GOLDEN_BLESSING_COOLDOWN_TICKS;

        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                owner.getX(),
                owner.getY() + 1.0D,
                owner.getZ(),
                8,
                0.45D,
                0.45D,
                0.45D,
                0.02D
        );
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame() && isTamingItem(stack)) {
            if (this.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            float tameChance = getTameChance(stack);

            if (this.random.nextFloat() < tameChance) {
                this.tame(player);
                this.getNavigation().stop();
                this.setTarget(null);
                this.setOrderedToSit(true);

                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }

            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private boolean isTamingItem(ItemStack stack) {
        return stack.is(Items.BONE) || stack.is(ModItems.GOLDEN_BONE.get());
    }

    private float getTameChance(ItemStack stack) {
        if (stack.is(ModItems.GOLDEN_BONE.get())) {
            return 0.70F;
        }

        return 0.33F;
    }

    public boolean shouldGlowEyes() {
        if (this.isInvisible()) {
            return false;
        }

        if (this.getTarget() != null || this.hurtTime > 0) {
            return true;
        }

        long time = this.level().getDayTime() % 24000L;

        boolean isNightTime = time >= 13000L && time <= 23000L;

        int skyLight = this.level().getBrightness(LightLayer.SKY, this.blockPosition());
        int blockLight = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition());

        boolean isActuallyDarkArea = skyLight <= 4 && blockLight <= 8;

        boolean noSkyDimensionDark = !this.level().dimensionType().hasSkyLight()
                && blockLight <= 8;

        return isNightTime || isActuallyDarkArea || noSkyDimensionDark;
    }

    @javax.annotation.Nullable
    public GoldenWolfEntity getBreedOffspring( ServerLevel level, AgeableMob otherParent) {
        GoldenWolfEntity wolf = (GoldenWolfEntity) ModEntities.GOLDEN_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof GoldenWolfEntity wolf1) {
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
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        // Prevent weird vanilla-style lightning transformation behavior if anything ever changes upstream.
    }
    public static boolean canSpawn(
            EntityType<GoldenWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
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
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(GoldenWolfEntity.class, EntityDataSerializers.INT);
    }
}