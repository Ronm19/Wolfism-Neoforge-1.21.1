package com.ronm19.wolfism.entity.ai.custom.black_wolf;

import com.ronm19.wolfism.entity.custom.neutral.BlackWolfEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class BlackWolfWatchPlayerGoal extends Goal {

    private final BlackWolfEntity wolf;
    private Player targetPlayer;
    private int watchTime;

    public BlackWolfWatchPlayerGoal(BlackWolfEntity wolf) {
        this.wolf = wolf;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (wolf.isTame()) return false;
        if (!wolf.isNightActive()) return false;
        if (wolf.getTarget() != null) return false;
        if (wolf.getRandom().nextInt(80) != 0) return false;

        Player nearest = wolf.level().getNearestPlayer(wolf, 10.0D);
        if (nearest == null) return false;
        if (nearest.isCreative() || nearest.isSpectator()) return false;

        this.targetPlayer = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetPlayer != null
                && targetPlayer.isAlive()
                && watchTime > 0
                && wolf.distanceToSqr(targetPlayer) <= 100.0D
                && wolf.getTarget() == null
                && wolf.isNightActive();
    }

    @Override
    public void start() {
        this.watchTime = 30 + wolf.getRandom().nextInt(30); // 1.5 to 3 sec
        wolf.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        this.watchTime = 0;
    }

    @Override
    public void tick() {
        if (targetPlayer == null) return;

        wolf.getNavigation().stop();
        wolf.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);
        watchTime--;
    }
}