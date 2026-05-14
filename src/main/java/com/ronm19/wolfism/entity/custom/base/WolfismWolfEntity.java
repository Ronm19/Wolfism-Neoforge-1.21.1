package com.ronm19.wolfism.entity.custom.base;

import com.ronm19.wolfism.entity.command.WolfismCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import com.ronm19.wolfism.item.custom.WolfStaffItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class WolfismWolfEntity extends Wolf {
    private static final EntityDataAccessor<Integer> DATA_WOLFISM_COMMAND =
            SynchedEntityData.defineId(WolfismWolfEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Optional<BlockPos>> DATA_GUARD_POS =
            SynchedEntityData.defineId(WolfismWolfEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    protected WolfismWolfEntity(EntityType<? extends WolfismWolfEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WOLFISM_COMMAND, WolfismCommand.FOLLOW.getId());
        builder.define(DATA_GUARD_POS, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        /*
         * Shared Wolfism command goals.
         *
         * HOLD and GUARD_POSITION are universal.
         * HUNT exists here as shared infrastructure, but it only activates for wolves
         * that support the HUNT command.
         */
        this.goalSelector.addGoal(1, new WolfismHoldCommandGoal(this));
        this.goalSelector.addGoal(2, new WolfismGuardPositionGoal(this, 1.15D, 16.0D));
        this.goalSelector.addGoal(2, new WolfismHuntCommandGoal(this, 1.2D, 14.0D));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof WolfStaffItem wolfStaffItem) {
            return wolfStaffItem.handleWolfEntityClick(this.level(), player, this);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();

        /*
         * Hard safety lock.
         *
         * Vanilla sitting alone is not always enough for modded wolves because custom goals,
         * owner-follow logic, or pack logic can still push movement.
         *
         * If Wolf Staff command is HOLD, the wolf must stay still.
         */
        if (!this.level().isClientSide && this.isHoldingCommand()) {
            this.forceWolfismHoldStill();
        }
    }

    public WolfismCommand getWolfismCommand() {
        return WolfismCommand.byId(this.entityData.get(DATA_WOLFISM_COMMAND));
    }

    public void setWolfismCommand(WolfismCommand command) {
        this.setWolfismCommand(command, true);
    }

    private void setWolfismCommand(WolfismCommand command, boolean updateGuardPosition) {
        if (!this.supportsCommand(command)) {
            command = WolfismCommand.FOLLOW;
        }

        this.entityData.set(DATA_WOLFISM_COMMAND, command.getId());

        if (command == WolfismCommand.HOLD) {
            this.forceWolfismHoldStill();
        } else {
            this.setOrderedToSit(false);
            this.setInSittingPose(false);
        }

        if (command == WolfismCommand.GUARD_POSITION) {
            if (updateGuardPosition) {
                this.setGuardPosition(this.blockPosition());
            }
        } else {
            this.clearGuardPosition();
        }
    }

    public void cycleWolfismCommand() {
        WolfismCommand current = this.getWolfismCommand();
        WolfismCommand next = current.next();

        int safety = 0;

        while (!this.supportsCommand(next) && safety < WolfismCommand.values().length) {
            next = next.next();
            safety++;
        }

        this.setWolfismCommand(next);
    }

    /**
     * Default shared command support.
     *
     * Every Wolfism wolf supports:
     * - FOLLOW
     * - HOLD
     * - GUARD_POSITION
     *
     * HUNT is disabled by default so only selected wolves can unlock it.
     */
    public boolean supportsCommand(WolfismCommand command) {
        return switch (command) {
            case FOLLOW, HOLD, GUARD_POSITION -> true;

            case HUNT,
                 PROTECT,
                 NOBLE_GUARD,
                 PRESSURE,
                 ROYAL_COMMAND,
                 NIGHT_WATCH -> false;
        };
    }

    public boolean canReceiveWolfStaffCommand(Player player) {
        return this.isTame() && this.isOwnedBy(player);
    }

    public boolean isInCommand(WolfismCommand command) {
        return this.getWolfismCommand() == command;
    }

    public boolean isFollowingCommand() {
        return this.isInCommand(WolfismCommand.FOLLOW);
    }

    public boolean isHoldingCommand() {
        return this.isInCommand(WolfismCommand.HOLD);
    }

    public boolean isGuardingPositionCommand() {
        return this.isInCommand(WolfismCommand.GUARD_POSITION);
    }

    public boolean isHuntingCommand() {
        return this.isInCommand(WolfismCommand.HUNT);
    }

    public boolean canUseCommandCombat() {
        return this.isTame()
                && !this.isOrderedToSit()
                && !this.isHoldingCommand();
    }

    protected void forceWolfismHoldStill() {
        this.setOrderedToSit(true);
        this.setInSittingPose(true);
        this.setTarget(null);
        this.getNavigation().stop();

        /*
         * Stop horizontal sliding / custom movement.
         * Keeps vertical motion so gravity still works naturally.
         */
        this.getMoveControl().strafe(0.0F, 0.0F);
        this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
    }

    public Optional<BlockPos> getGuardPosition() {
        return this.entityData.get(DATA_GUARD_POS);
    }

    public void setGuardPosition(@Nullable BlockPos pos) {
        this.entityData.set(DATA_GUARD_POS, Optional.ofNullable(pos == null ? null : pos.immutable()));
    }

    public void clearGuardPosition() {
        this.entityData.set(DATA_GUARD_POS, Optional.empty());
    }

    /**
     * Compatibility helpers for any existing code that already uses commandPosition naming.
     */
    public void setCommandPosition(BlockPos pos) {
        this.setGuardPosition(pos);
        this.setWolfismCommand(WolfismCommand.GUARD_POSITION, false);
    }

    @Nullable
    public BlockPos getCommandPosition() {
        return this.getGuardPosition().orElse(null);
    }

    public boolean hasCommandPosition() {
        return this.getGuardPosition().isPresent();
    }

    public boolean isOwnedBySameOwner(TamableAnimal other) {
        if (!this.isTame() || !other.isTame()) {
            return false;
        }

        UUID thisOwner = this.getOwnerUUID();
        UUID otherOwner = other.getOwnerUUID();

        return thisOwner != null && thisOwner.equals(otherOwner);
    }

    public boolean canWolfismTarget(LivingEntity target, boolean allowAnimals) {
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (target == this) {
            return false;
        }

        /*
         * Wolfism wolves should never hunt/attack each other through staff commands.
         * This protects both tamed and untamed Wolfism wolves.
         */
        if (target instanceof WolfismWolfEntity) {
            return false;
        }

        LivingEntity owner = this.getOwner();
        if (owner != null && target == owner) {
            return false;
        }

        if (target instanceof Player) {
            return false;
        }

        if (target instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame()) {
            if (this.isOwnedBySameOwner(tamableAnimal)) {
                return false;
            }

            return false;
        }

        if (target instanceof Monster) {
            return true;
        }

        return allowAnimals && target instanceof Animal;
    }

    @Nullable
    public LivingEntity findNearestWolfismTarget(double range, boolean allowAnimals) {
        AABB area = this.getBoundingBox().inflate(range);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> this.canWolfismTarget(target, allowAnimals)
        );

        return targets.stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
    }

    @Nullable
    public LivingEntity findNearestWolfismTargetAround(BlockPos center, double range, boolean allowAnimals) {
        AABB area = new AABB(center).inflate(range);

        List<LivingEntity> targets = this.level().getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> this.canWolfismTarget(target, allowAnimals)
        );

        return targets.stream()
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(
                        center.getX() + 0.5D,
                        center.getY() + 0.5D,
                        center.getZ() + 0.5D
                )))
                .orElse(null);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.putInt("WolfismCommand", this.getWolfismCommand().getId());

        Optional<BlockPos> guardPos = this.getGuardPosition();

        if (guardPos.isPresent()) {
            BlockPos pos = guardPos.get();

            tag.putBoolean("HasWolfismCommandPosition", true);
            tag.putInt("WolfismCommandX", pos.getX());
            tag.putInt("WolfismCommandY", pos.getY());
            tag.putInt("WolfismCommandZ", pos.getZ());
        } else {
            tag.putBoolean("HasWolfismCommandPosition", false);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        WolfismCommand savedCommand = WolfismCommand.FOLLOW;

        if (tag.contains("WolfismCommand")) {
            savedCommand = WolfismCommand.byId(tag.getInt("WolfismCommand"));
        }

        if (tag.getBoolean("HasWolfismCommandPosition")) {
            this.setGuardPosition(new BlockPos(
                    tag.getInt("WolfismCommandX"),
                    tag.getInt("WolfismCommandY"),
                    tag.getInt("WolfismCommandZ")
            ));
        } else {
            this.clearGuardPosition();
        }

        if (this.supportsCommand(savedCommand)) {
            this.entityData.set(DATA_WOLFISM_COMMAND, savedCommand.getId());

            if (savedCommand == WolfismCommand.HOLD) {
                this.forceWolfismHoldStill();
            } else {
                this.setOrderedToSit(false);
                this.setInSittingPose(false);
            }
        } else {
            this.setWolfismCommand(WolfismCommand.FOLLOW);
        }
    }

    private static double getWolfismAttackReachSqr(WolfismWolfEntity wolf, LivingEntity target) {
        double attackReach = wolf.getBbWidth() * 2.2D + target.getBbWidth();
        return attackReach * attackReach;
    }

    private static class WolfismHoldCommandGoal extends Goal {
        private final WolfismWolfEntity wolf;

        public WolfismHoldCommandGoal(WolfismWolfEntity wolf) {
            this.wolf = wolf;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK, Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return this.wolf.isAlive()
                    && this.wolf.isTame()
                    && this.wolf.isInCommand(WolfismCommand.HOLD);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.wolf.forceWolfismHoldStill();
        }

        @Override
        public void tick() {
            this.wolf.forceWolfismHoldStill();
        }

        @Override
        public void stop() {
            if (!this.wolf.isInCommand(WolfismCommand.HOLD)) {
                this.wolf.setOrderedToSit(false);
                this.wolf.setInSittingPose(false);
            }
        }
    }

    private static class WolfismGuardPositionGoal extends Goal {
        private final WolfismWolfEntity wolf;
        private final double speedModifier;
        private final double defendRange;

        private int attackCooldown;

        public WolfismGuardPositionGoal(WolfismWolfEntity wolf, double speedModifier, double defendRange) {
            this.wolf = wolf;
            this.speedModifier = speedModifier;
            this.defendRange = defendRange;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.wolf.isAlive()
                    && this.wolf.isTame()
                    && !this.wolf.isHoldingCommand()
                    && this.wolf.isInCommand(WolfismCommand.GUARD_POSITION)
                    && this.wolf.hasCommandPosition();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            BlockPos guardPos = this.wolf.getCommandPosition();

            if (guardPos == null) {
                return;
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            LivingEntity target = this.wolf.getTarget();

            if (target == null
                    || !target.isAlive()
                    || !this.wolf.canWolfismTarget(target, false)
                    || !this.isTargetInsideGuardRange(target, guardPos)) {
                target = this.wolf.findNearestWolfismTargetAround(guardPos, this.defendRange, false);
                this.wolf.setTarget(target);
            }

            if (target != null
                    && target.isAlive()
                    && this.wolf.canWolfismTarget(target, false)
                    && this.isTargetInsideGuardRange(target, guardPos)) {
                this.moveAndAttackTarget(target);
                return;
            }

            this.returnToGuardPosition(guardPos);
        }

        private boolean isTargetInsideGuardRange(LivingEntity target, BlockPos guardPos) {
            return target.distanceToSqr(
                    guardPos.getX() + 0.5D,
                    guardPos.getY() + 0.5D,
                    guardPos.getZ() + 0.5D
            ) <= this.defendRange * this.defendRange;
        }

        private void moveAndAttackTarget(LivingEntity target) {
            this.wolf.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.wolf.getNavigation().moveTo(target, this.speedModifier);

            if (this.wolf.distanceToSqr(target) <= getWolfismAttackReachSqr(this.wolf, target)
                    && this.attackCooldown <= 0) {
                this.attackCooldown = 20;
                this.wolf.doHurtTarget(target);
            }
        }

        private void returnToGuardPosition(BlockPos guardPos) {
            double distanceToGuard = this.wolf.distanceToSqr(
                    guardPos.getX() + 0.5D,
                    guardPos.getY(),
                    guardPos.getZ() + 0.5D
            );

            if (distanceToGuard > 3.0D) {
                this.wolf.getNavigation().moveTo(
                        guardPos.getX() + 0.5D,
                        guardPos.getY(),
                        guardPos.getZ() + 0.5D,
                        this.speedModifier
                );
            } else {
                this.wolf.getNavigation().stop();
            }
        }
    }

    private static class WolfismHuntCommandGoal extends Goal {
        private final WolfismWolfEntity wolf;
        private final double speedModifier;
        private final double huntRange;

        private int targetSearchCooldown;
        private int attackCooldown;

        public WolfismHuntCommandGoal(WolfismWolfEntity wolf, double speedModifier, double huntRange) {
            this.wolf = wolf;
            this.speedModifier = speedModifier;
            this.huntRange = huntRange;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.wolf.isAlive()
                    && this.wolf.isTame()
                    && !this.wolf.isHoldingCommand()
                    && this.wolf.supportsCommand(WolfismCommand.HUNT)
                    && this.wolf.isInCommand(WolfismCommand.HUNT);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            if (this.targetSearchCooldown > 0) {
                this.targetSearchCooldown--;
            }

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }

            LivingEntity target = this.wolf.getTarget();

            if (target != null && (!target.isAlive() || !this.wolf.canWolfismTarget(target, true))) {
                this.wolf.setTarget(null);
                target = null;
                this.wolf.getNavigation().stop();
            }

            if (target == null && this.targetSearchCooldown <= 0) {
                this.targetSearchCooldown = 20;
                target = this.wolf.findNearestWolfismTarget(this.huntRange, true);
                this.wolf.setTarget(target);
            }

            if (target == null) {
                this.wolf.getNavigation().stop();
                return;
            }

            this.wolf.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.wolf.getNavigation().moveTo(target, this.speedModifier);

            if (this.wolf.distanceToSqr(target) <= getWolfismAttackReachSqr(this.wolf, target)
                    && this.attackCooldown <= 0) {
                this.attackCooldown = 20;
                this.wolf.doHurtTarget(target);
            }
        }
    }
}