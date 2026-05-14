package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EndWolfEntity extends WolfismWolfEntity {
    private static final int OWNER_TELEPORT_COOLDOWN = 80;
    private static final int DEFENSIVE_TELEPORT_COOLDOWN = 120;

    private int endTeleportCooldown = 0;

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;

    public EndWolfEntity(EntityType<? extends WolfismWolfEntity > type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    protected void defineSynchedData( SynchedEntityData.Builder builder ) {
        super.defineSynchedData(builder);
        builder.define(DATA_COLLAR_COLOR, DyeColor.MAGENTA.getId());
    }

    @Override
    public void addAdditionalSaveData( CompoundTag tag ) {
        super.addAdditionalSaveData(tag);
        tag.putByte("CollarColor", (byte) this.getCollarColor().getId());
    }

    @Override
    public void readAdditionalSaveData( CompoundTag tag ) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("CollarColor", 99)) {
            this.setCollarColor(DyeColor.byId(tag.getInt("CollarColor")));
        }
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return !this.isPassenger();
    }

    @Override
    public boolean canChangeDimensions(Level currentLevel, Level destinationLevel) {
        return true;
    }

    private boolean isInsidePortalBlock() {
        BlockPos pos = this.blockPosition();

        return this.level().getBlockState(pos).is(Blocks.NETHER_PORTAL)
                || this.level().getBlockState(pos.above()).is(Blocks.NETHER_PORTAL)
                || this.level().getBlockState(pos.below()).is(Blocks.NETHER_PORTAL)
                || this.level().getBlockState(pos).is(Blocks.END_PORTAL)
                || this.level().getBlockState(pos.above()).is(Blocks.END_PORTAL)
                || this.level().getBlockState(pos.below()).is(Blocks.END_PORTAL)
                || this.level().getBlockState(pos).is(Blocks.END_GATEWAY)
                || this.level().getBlockState(pos.above()).is(Blocks.END_GATEWAY)
                || this.level().getBlockState(pos.below()).is(Blocks.END_GATEWAY);
    }

    public static boolean canSpawn(
            EntityType<EndWolfEntity> type,
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

    @Override
    public void tick() {
        super.tick();

        if (this.endTeleportCooldown > 0) {
            this.endTeleportCooldown--;
        }

        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            this.tryTeleportNearOwner(serverLevel);
            this.spawnEndIdleParticles(serverLevel);
        }
    }

    private void tryTeleportNearOwner(ServerLevel level) {
        if (this.isInsidePortalBlock() || this.isOnPortalCooldown()) return;
        if (this.endTeleportCooldown > 0) return;
        if (!this.isTame()) return;
        if (this.isOrderedToSit()) return;
        if (this.isPassenger()) return;
        if (this.isLeashed()) return;

        Entity owner = this.getOwner();
        if (!(owner instanceof Player player)) return;
        if (!player.isAlive()) return;

        double distanceToOwner = this.distanceToSqr(player);

        if (distanceToOwner >= 36.0D * 36.0D && distanceToOwner <= 128.0D * 128.0D) {
            if (this.teleportNearEntity(level, player, 4, 8)) {
                this.endTeleportCooldown = OWNER_TELEPORT_COOLDOWN;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (
                hurt
                        && !this.level().isClientSide
                        && this.level() instanceof ServerLevel serverLevel
                        && this.endTeleportCooldown <= 0
                        && !this.isInsidePortalBlock()
                        && !this.isOnPortalCooldown()
                        && amount > 0.0F
                        && source.getEntity() != null
                        && this.random.nextFloat() < 0.25F
        ) {
            if (this.teleportAwayFromEntity(serverLevel, source.getEntity())) {
                this.endTeleportCooldown = DEFENSIVE_TELEPORT_COOLDOWN;
            }
        }

        return hurt;
    }

    private boolean teleportNearEntity(ServerLevel level, Entity target, int minRange, int maxRange) {
        for (int attempts = 0; attempts < 16; attempts++) {
            int xOffset = this.random.nextInt(maxRange * 2 + 1) - maxRange;
            int zOffset = this.random.nextInt(maxRange * 2 + 1) - maxRange;

            if (Math.abs(xOffset) < minRange && Math.abs(zOffset) < minRange) {
                continue;
            }

            int x = target.getBlockX() + xOffset;
            int y = target.getBlockY() + this.random.nextInt(3) - 1;
            int z = target.getBlockZ() + zOffset;

            if (this.canEndTeleportTo(level, x, y, z)) {
                this.doEndTeleport(level, x + 0.5D, y, z + 0.5D);
                return true;
            }
        }

        return false;
    }

    private boolean teleportAwayFromEntity(ServerLevel level, Entity threat) {
        double awayX = this.getX() - threat.getX();
        double awayZ = this.getZ() - threat.getZ();

        double length = Math.sqrt(awayX * awayX + awayZ * awayZ);

        if (length < 0.001D) {
            awayX = this.random.nextDouble() - 0.5D;
            awayZ = this.random.nextDouble() - 0.5D;
            length = Math.sqrt(awayX * awayX + awayZ * awayZ);
        }

        awayX /= length;
        awayZ /= length;

        for (int attempts = 0; attempts < 12; attempts++) {
            double distance = 6.0D + this.random.nextDouble() * 6.0D;

            int x = this.blockPosition().getX() + (int) Math.round(awayX * distance) + this.random.nextInt(5) - 2;
            int y = this.blockPosition().getY() + this.random.nextInt(5) - 2;
            int z = this.blockPosition().getZ() + (int) Math.round(awayZ * distance) + this.random.nextInt(5) - 2;

            if (this.canEndTeleportTo(level, x, y, z)) {
                this.doEndTeleport(level, x + 0.5D, y, z + 0.5D);
                return true;
            }
        }

        return false;
    }

    private boolean canEndTeleportTo(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();

        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }

        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }

        if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            return false;
        }

        return level.noCollision(this, this.getBoundingBox().move(
                x + 0.5D - this.getX(),
                y - this.getY(),
                z + 0.5D - this.getZ()
        ));
    }

    private void doEndTeleport(ServerLevel level, double x, double y, double z) {
        level.sendParticles(
                ParticleTypes.PORTAL,
                this.getX(),
                this.getY() + 0.5D,
                this.getZ(),
                28,
                0.35D,
                0.45D,
                0.35D,
                0.08D
        );

        this.teleportTo(x, y, z);
        this.getNavigation().stop();

        level.sendParticles(
                ParticleTypes.PORTAL,
                this.getX(),
                this.getY() + 0.5D,
                this.getZ(),
                28,
                0.35D,
                0.45D,
                0.35D,
                0.08D
        );

        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.7F, 1.25F);
    }

    private void spawnEndIdleParticles(ServerLevel level) {
        if (this.random.nextInt(30) != 0) return;

        level.sendParticles(
                ParticleTypes.PORTAL,
                this.getRandomX(0.5D),
                this.getRandomY(),
                this.getRandomZ(0.5D),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.02D
        );
    }

    @Override
    public @NotNull InteractionResult mobInteract( Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTame() && this.isEndWolfTamingItem(stack)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                float tameChance = stack.is(Items.CHORUS_FRUIT) ? 0.45F : 0.33F;

                if (this.random.nextFloat() < tameChance) {
                    this.tame(player);
                    this.getNavigation().stop();
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

    private boolean isEndWolfTamingItem(ItemStack stack) {
        return stack.is(Items.BONE) || stack.is(Items.CHORUS_FRUIT);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.CHORUS_FRUIT);
    }

    @Override
    public boolean canMate(Animal otherAnimal) {
        if (!(otherAnimal instanceof EndWolfEntity otherEndWolf)) {
            return false;
        }

        return super.canMate(otherEndWolf);
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
    public @Nullable EndWolfEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        EndWolfEntity wolf = ModEntities.END_WOLF.get().create(level);

        if (wolf != null && otherParent instanceof EndWolfEntity other) {
            EndWolfEntity ownerParent = this.isTame() ? this : other.isTame() ? other : null;

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
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(EndWolfEntity.class, EntityDataSerializers.INT);
    }
}