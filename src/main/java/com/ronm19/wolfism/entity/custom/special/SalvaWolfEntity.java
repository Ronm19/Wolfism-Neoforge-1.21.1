package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SalvaWolfEntity extends Wolf {
    private int royalGuardTicks = 0;
    private int noblePresenceCooldown = 0;
    private int judgmentBiteCooldown = 0;
    private int lastStandCooldown = 0;
    private int lastOwnerHurtTimestamp = -1;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(SalvaWolfEntity.class, EntityDataSerializers.INT);


    public SalvaWolfEntity(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 46.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.20D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.YELLOW.getId());
    }


    public static boolean canSpawn(
            EntityType<SalvaWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            tickCooldowns();
            handleOwnerProtection();
            handleNoblePresence();
            handleLastStandLoyalty();
        }
    }


    private void tickCooldowns() {
        if (royalGuardTicks > 0) royalGuardTicks--;
        if (noblePresenceCooldown > 0) noblePresenceCooldown--;
        if (judgmentBiteCooldown > 0) judgmentBiteCooldown--;
        if (lastStandCooldown > 0) lastStandCooldown--;
    }

    private void handleOwnerProtection() {
        if (!this.isTame()) return;

        Entity ownerEntity = this.getOwner();
        if (!(ownerEntity instanceof Player owner)) return;

        Entity attackerEntity = owner.getLastHurtByMob();
        int hurtTimestamp = owner.getLastHurtByMobTimestamp();

        if (!(attackerEntity instanceof net.minecraft.world.entity.LivingEntity attacker)) return;
        if (!attacker.isAlive()) return;
        if (hurtTimestamp == this.lastOwnerHurtTimestamp) return;
        if (this.distanceToSqr(owner) > 24.0D * 24.0D) return;

        this.lastOwnerHurtTimestamp = hurtTimestamp;
        this.royalGuardTicks = 160;

        this.setTarget(attacker);
        this.getNavigation().moveTo(attacker, 1.35D);

        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0));
    }

    private void handleNoblePresence() {
        if (!this.isTame()) return;
        if (this.noblePresenceCooldown > 0) return;

        this.noblePresenceCooldown = 100;

        List<Wolf> nearbyWolves = this.level().getEntitiesOfClass(
                Wolf.class,
                this.getBoundingBox().inflate(8.0D),
                wolf -> wolf.isAlive() && wolf.isTame() && isSameOwner(wolf)
        );

        for (Wolf wolf : nearbyWolves) {
            if (wolf == this) continue;

            wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
        }
    }

    private void handleLastStandLoyalty() {
        if (!this.isTame()) return;
        if (this.lastStandCooldown > 0) return;

        Entity ownerEntity = this.getOwner();
        if (!(ownerEntity instanceof Player owner)) return;

        if (this.distanceToSqr(owner) > 16.0D * 16.0D) return;

        float ownerHealthPercent = owner.getHealth() / owner.getMaxHealth();

        if (ownerHealthPercent <= 0.35F) {
            this.lastStandCooldown = 1200;
            this.royalGuardTicks = Math.max(this.royalGuardTicks, 200);

            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));

            owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0));
        }
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

    private boolean isSameOwner(Wolf wolf) {
        UUID owner = this.getOwnerUUID();
        UUID otherOwner = wolf.getOwnerUUID();

        return owner != null && owner.equals(otherOwner);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean result = super.doHurtTarget(target);

        if (!this.level().isClientSide && result && this.royalGuardTicks > 0 && this.judgmentBiteCooldown <= 0) {
            this.judgmentBiteCooldown = 180;

            target.hurt(this.damageSources().mobAttack(this), 4.0F);
            this.heal(2.0F);
        }

        return result;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame() && stack.is(Items.BONE)) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!this.isTame() && stack.is(ModItems.ROYAL_BONE.get())) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                if (this.random.nextInt(3) == 0) {
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

    public boolean shouldGlowEyes() {
        if (this.royalGuardTicks > 0) return true;
        if (this.getTarget() != null) return true;

        return !this.level().isDay()
                || this.level().getMaxLocalRawBrightness(this.blockPosition()) <= 7;
    }

    @Override
    public @Nullable SalvaWolfEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        SalvaWolfEntity wolf = ModEntities.SALVA_WOLF.get().create(level);

        if (wolf != null) {
            UUID owner = this.getOwnerUUID();

            if (owner == null && otherParent instanceof SalvaWolfEntity otherSalva) {
                owner = otherSalva.getOwnerUUID();
            }

            if (owner != null) {
                wolf.setOwnerUUID(owner);
                wolf.setTame(true, true);
            }

            if (otherParent instanceof SalvaWolfEntity otherSalva) {
                wolf.setCollarColor(this.random.nextBoolean() ? this.getCollarColor() : otherSalva.getCollarColor());
            } else {
                wolf.setCollarColor(this.getCollarColor());
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
}