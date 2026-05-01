package com.ronm19.wolfism.event;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.entity.custom.special.EndWolfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class EndWolfSpawnEvents {

    private static int tickTimer = 0;

    @SubscribeEvent
    public static void onServerTick( ServerTickEvent.Post event) {
        tickTimer++;

        // Check every 20 seconds.
        if (tickTimer < 400) {
            return;
        }

        tickTimer = 0;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }

            if (level.dimension() != Level.END) {
                continue;
            }

            if (player.isSpectator()) {
                continue;
            }

            trySpawnEndWolfNearPlayer(level, player);
        }
    }

    private static void trySpawnEndWolfNearPlayer(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.random;

        // Do not spam them.
        int nearbyEndWolves = level.getEntitiesOfClass(
                EndWolfEntity.class,
                player.getBoundingBox().inflate(96.0D)
        ).size();

        if (nearbyEndWolves >= 2) {
            return;
        }

        // Special Wolf rarity.
        // For testing, temporarily change 8 to 1.
        if (random.nextInt(1) != 0) {
            return;
        }

        for (int attempts = 0; attempts < 24; attempts++) {
            BlockPos pos = findSpawnPos(level, player.blockPosition(), random);

            if (pos == null) {
                continue;
            }

            EndWolfEntity wolf = ModEntities.END_WOLF.get().create(level);

            if (wolf == null) {
                return;
            }

            wolf.moveTo(
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    random.nextFloat() * 360.0F,
                    0.0F
            );

            if (level.noCollision(wolf) && !level.containsAnyLiquid(wolf.getBoundingBox())) {
                level.addFreshEntityWithPassengers(wolf);
                wolf.finalizeSpawn(
                        level,
                        level.getCurrentDifficultyAt(pos),
                        MobSpawnType.NATURAL,
                        null
                );
                return;
            }
        }
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos playerPos, RandomSource random) {
        int distance = 32 + random.nextInt(40);

        int xOffset = random.nextInt(distance * 2 + 1) - distance;
        int zOffset = random.nextInt(distance * 2 + 1) - distance;

        // Avoid spawning too close.
        if (Math.abs(xOffset) < 24 && Math.abs(zOffset) < 24) {
            return null;
        }

        int x = playerPos.getX() + xOffset;
        int z = playerPos.getZ() + zOffset;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(x, playerPos.getY() + 32, z);

        for (int y = playerPos.getY() + 32; y >= level.getMinBuildHeight(); y--) {
            mutable.set(x, y, z);

            BlockPos below = mutable.below();

            BlockState ground = level.getBlockState(below);
            BlockState body = level.getBlockState(mutable);
            BlockState head = level.getBlockState(mutable.above());

            boolean validGround = ground.isFaceSturdy(level, below, Direction.UP);
            boolean bodyClear = body.getCollisionShape(level, mutable).isEmpty();
            boolean headClear = head.getCollisionShape(level, mutable.above()).isEmpty();

            if (validGround && bodyClear && headClear) {
                // Optional: only real End biomes.
                if (!level.getBiome(mutable).is(BiomeTags.IS_END)) {
                    return null;
                }

                return mutable.immutable();
            }
        }

        return null;
    }
}