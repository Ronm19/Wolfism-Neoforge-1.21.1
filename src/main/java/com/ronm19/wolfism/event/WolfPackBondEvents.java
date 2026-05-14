package com.ronm19.wolfism.event;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import com.ronm19.wolfism.entity.custom.elite.WolfKingEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = WolfismMod.MOD_ID)
public class WolfPackBondEvents {
    private static int tickCounter = 0;

    private static final int CHECK_INTERVAL = 80;

    private static final double PACK_RANGE = 18.0D;

    private static final int EFFECT_DURATION = 120;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;

        if (tickCounter < CHECK_INTERVAL) {
            return;
        }

        tickCounter = 0;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            applyPackBond(player);
        }
    }

    private static void applyPackBond(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        List<WolfismWolfEntity> ownedWolves = level.getEntitiesOfClass(
                WolfismWolfEntity.class,
                player.getBoundingBox().inflate(PACK_RANGE),
                wolf -> wolf.isAlive()
                        && wolf.isTame()
                        && wolf.getOwnerUUID() != null
                        && wolf.getOwnerUUID().equals(player.getUUID())
        );

        int packSize = ownedWolves.size();

        if (packSize < 2) {
            return;
        }

        boolean hasWolfKing = ownedWolves.stream().anyMatch(wolf -> wolf instanceof WolfKingEntity);

        for (WolfismWolfEntity wolf : ownedWolves) {
            applyBondEffects(wolf, packSize, hasWolfKing);
            spawnBondParticles(level, wolf, packSize, hasWolfKing);
        }
    }

    private static void applyBondEffects(WolfismWolfEntity wolf, int packSize, boolean hasWolfKing) {
        /*
         * 2+ wolves:
         * light pack recovery.
         */
        if (packSize >= 2 && wolf.getHealth() < wolf.getMaxHealth()) {
            wolf.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    EFFECT_DURATION,
                    0,
                    true,
                    false,
                    true
            ));
        }

        /*
         * 3+ wolves:
         * pack movement confidence.
         */
        if (packSize >= 3) {
            wolf.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    EFFECT_DURATION,
                    0,
                    true,
                    false,
                    true
            ));
        }

        /*
         * 5+ wolves:
         * pack force.
         */
        if (packSize >= 5) {
            wolf.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_BOOST,
                    EFFECT_DURATION,
                    0,
                    true,
                    false,
                    true
            ));
        }

        /*
         * Wolf King nearby:
         * royal pack protection.
         */
        if (hasWolfKing) {
            wolf.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    EFFECT_DURATION,
                    0,
                    true,
                    false,
                    true
            ));
        }
    }

    private static void spawnBondParticles(ServerLevel level, WolfismWolfEntity wolf, int packSize, boolean hasWolfKing) {
        if (wolf.getRandom().nextFloat() > 0.45F) {
            return;
        }

        if (hasWolfKing) {
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    wolf.getX(),
                    wolf.getY() + 0.75D,
                    wolf.getZ(),
                    6,
                    0.35D,
                    0.25D,
                    0.35D,
                    0.02D
            );

            return;
        }

        if (packSize >= 5) {
            level.sendParticles(
                    ParticleTypes.CRIT,
                    wolf.getX(),
                    wolf.getY() + 0.65D,
                    wolf.getZ(),
                    4,
                    0.3D,
                    0.25D,
                    0.3D,
                    0.02D
            );

            return;
        }

        level.sendParticles(
                ParticleTypes.HEART,
                wolf.getX(),
                wolf.getY() + 0.8D,
                wolf.getZ(),
                1,
                0.25D,
                0.2D,
                0.25D,
                0.01D
        );
    }
}