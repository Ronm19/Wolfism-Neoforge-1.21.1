package com.ronm19.wolfism.entity.custom.special;

import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

public class ZombieWolfEntity extends WolfismWolfEntity {

    public ZombieWolfEntity( EntityType<? extends ZombieWolfEntity> entityType, Level level ) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return WolfismWolfEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.10D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Wild Zombie Wolves are hostile before taming.
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
    public InteractionResult mobInteract( Player player, InteractionHand hand ) {
        ItemStack stack = player.getItemInHand(hand);

        // Rotten Flesh taming
        if (!this.isTame() && stack.is(Items.ROTTEN_FLESH)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                // 33% tame chance
                if (this.getRandom().nextInt(3) == 0) {
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

    @Override
    public boolean isFood( ItemStack stack ) {
        return stack.is(Items.ROTTEN_FLESH);
    }

    @Override
    public boolean doHurtTarget( Entity target ) {
        boolean didHit = super.doHurtTarget(target);

        if (didHit && target instanceof LivingEntity livingTarget) {
            applyDecayBite(livingTarget);
        }

        return didHit;
    }

    private void applyDecayBite( LivingEntity target ) {
        RandomSource random = this.getRandom();

        // 20% chance: Weakness
        if (random.nextFloat() < 0.20F) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
        }

        // 10% chance: Hunger
        if (random.nextFloat() < 0.10F) {
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0), this);
        }

        // 5% chance: Poison
        if (random.nextFloat() < 0.05F) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), this);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Wild Zombie Wolves burn in sunlight.
        // Tamed Zombie Wolves do NOT burn, because that would be annoying for pets.
        if (!this.level().isClientSide
                && !this.isTame()
                && this.isAlive()
                && this.level().isDay()
                && this.level().canSeeSky(this.blockPosition())
                && !this.isInWaterRainOrBubble()) {
            this.isSunBurnTick();
        }
    }

    public static boolean canSpawn(
            EntityType<ZombieWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if (spawnType == MobSpawnType.SPAWN_EGG) {
            return true;
        }

        if (level.getLevel().dimension() != Level.OVERWORLD) {
            return false;
        }

        BlockPos below = pos.below();

        boolean validGround = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);

        boolean hasSpace = level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();

        long time = level.getLevel().getDayTime() % 24000L;
        boolean isNight = time >= 13000L && time <= 23000L;

        return validGround && hasSpace && isNight;
    }
}