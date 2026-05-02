package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
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
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class WolfKingEntity extends Wolf {
    private static final EntityDataAccessor<Boolean> ROYAL_RAGE =
            SynchedEntityData.defineId(WolfKingEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(WolfKingEntity.class, EntityDataSerializers.INT);


    private static final int ROYAL_RAGE_DURATION = 20 * 14;
    private static final int ROYAL_RAGE_COOLDOWN = 20 * 55;
    private static final float ROYAL_BONE_TAME_CHANCE = 0.75F;

    private int royalAuraTick;
    private int royalRageTime;
    private int royalRageCooldown;

    public WolfKingEntity(EntityType<? extends WolfKingEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 52.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROYAL_RAGE, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.YELLOW.getId());
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            this.handleRoyalRage();

            this.royalAuraTick++;
            if (this.royalAuraTick >= 40) {
                this.royalAuraTick = 0;
                this.applyRoyalPresence();
            }
        }
    }

    private void handleRoyalRage() {
        if (this.royalRageCooldown > 0) {
            this.royalRageCooldown--;
        }

        if (this.royalRageTime > 0) {
            this.royalRageTime--;
            this.setRoyalRageActive(true);

            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 45, 1, true, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 45, 1, true, false, true));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 45, 0, true, false, true));

            return;
        }

        this.setRoyalRageActive(false);

        if (!this.isTame() || this.royalRageCooldown > 0) {
            return;
        }

        boolean ownerInDanger = this.isOwnerInDanger();
        boolean surroundedByEnemies = this.countNearbyEnemies(10.0D) >= 3;

        if (ownerInDanger || surroundedByEnemies) {
            this.startRoyalRage();
        }
    }

    private boolean isOwnerInDanger() {
        if (this.getOwner() == null) {
            return false;
        }

        return this.getOwner().getHealth() <= this.getOwner().getMaxHealth() * 0.35F;
    }

    private int countNearbyEnemies(double range) {
        List<Monster> enemies = this.level().getEntitiesOfClass(
                Monster.class,
                this.getBoundingBox().inflate(range),
                enemy -> enemy.isAlive() && !enemy.isAlliedTo(this)
        );

        return enemies.size();
    }

    private void startRoyalRage() {
        this.royalRageTime = ROYAL_RAGE_DURATION;
        this.royalRageCooldown = ROYAL_RAGE_COOLDOWN;
        this.setRoyalRageActive(true);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    this.getX(),
                    this.getY() + 0.8D,
                    this.getZ(),
                    35,
                    0.55D,
                    0.45D,
                    0.55D,
                    0.05D
            );
        }
    }

    private void applyRoyalPresence() {
        if (!this.isTame()) {
            return;
        }

        UUID ownerId = this.getOwnerUUID();
        if (ownerId == null) {
            return;
        }

        List<Wolf> alliedWolves = this.level().getEntitiesOfClass(
                Wolf.class,
                this.getBoundingBox().inflate(10.0D),
                wolf -> wolf.isAlive()
                        && wolf.isTame()
                        && wolf.getOwnerUUID() != null
                        && wolf.getOwnerUUID().equals(ownerId)
        );

        int nearbyAlliedWolves = 0;

        for (Wolf wolf : alliedWolves) {
            if (wolf == this) {
                continue;
            }

            nearbyAlliedWolves++;

            wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, this.isRoyalRageActive() ? 1 : 0, true, false, true));
            wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));

            if (this.isRoyalRageActive()) {
                wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
            }
        }

        if (nearbyAlliedWolves >= 2) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
        }

        if (nearbyAlliedWolves >= 4) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame() && stack.is(ModItems.ROYAL_BONE.get())) {
            if (this.level().isClientSide) {
                return InteractionResult.SUCCESS;
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (this.random.nextFloat() < ROYAL_BONE_TAME_CHANCE) {
                this.tame(player);
                this.getNavigation().stop();
                this.setTarget(null);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            this.getX(),
                            this.getY() + 0.8D,
                            this.getZ(),
                            18,
                            0.4D,
                            0.35D,
                            0.4D,
                            0.05D
                    );
                }
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }

            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);

        if (hurt && this.isRoyalRageActive()) {
            target.hurt(this.damageSources().mobAttack(this), 3.0F);
        }

        return hurt;
    }

    public boolean isRoyalRageActive() {
        return this.entityData.get(ROYAL_RAGE);
    }

    private void setRoyalRageActive(boolean value) {
        this.entityData.set(ROYAL_RAGE, value);
    }

    public boolean shouldGlowEyes() {
        return this.isRoyalRageActive() || this.isAggressive();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("CollarColor", (byte) this.getCollarColor().getId());
        tag.putInt("RoyalRageTime", this.royalRageTime);
        tag.putInt("RoyalRageCooldown", this.royalRageCooldown);
        tag.putBoolean("RoyalRageActive", this.isRoyalRageActive());
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
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.royalRageTime = tag.getInt("RoyalRageTime");
        this.royalRageCooldown = tag.getInt("RoyalRageCooldown");
        this.setRoyalRageActive(tag.getBoolean("RoyalRageActive"));

        if (tag.contains("CollarColor", 99))
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));

        }

    @Override
    public @Nullable WolfKingEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        WolfKingEntity baby = ModEntities.WOLF_KING.get().create(level);

        if (baby != null && otherParent instanceof WolfKingEntity other) {
            if (this.isTame()) {
                baby.setOwnerUUID(this.getOwnerUUID());
                baby.setTame(true, true);
                baby.setCollarColor(this.random.nextBoolean() ? this.getCollarColor() : other.getCollarColor());
            }
        }

        return baby;
    }

    public static boolean canSpawn(
            EntityType<WolfKingEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }
}