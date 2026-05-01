package com.ronm19.wolfism.event;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.special.AngelWolfEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class AngelWolfEvents {

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getDistance() < 4.0F) {
            return;
        }

        boolean hasAngelWolfNearby = !player.level().getEntitiesOfClass(
                AngelWolfEntity.class,
                player.getBoundingBox().inflate(12.0D),
                wolf -> wolf.isAlive()
                        && wolf.isTame()
                        && wolf.isOwnedBy(player)
        ).isEmpty();

        if (!hasAngelWolfNearby) {
            return;
        }

        event.setDistance(0.0F);
        event.setDamageMultiplier(0.0F);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    player.getX(),
                    player.getY() + 0.2D,
                    player.getZ(),
                    16,
                    0.35D,
                    0.08D,
                    0.35D,
                    0.03D
            );

            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    player.getX(),
                    player.getY() + 0.8D,
                    player.getZ(),
                    10,
                    0.3D,
                    0.45D,
                    0.3D,
                    0.025D
            );
        }
    }
}