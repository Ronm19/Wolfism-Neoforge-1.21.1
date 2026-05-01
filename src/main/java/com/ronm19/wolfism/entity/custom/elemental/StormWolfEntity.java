package com.ronm19.wolfism.entity.custom.elemental;

import com.ronm19.wolfism.entity.ModEntities;
import com.ronm19.wolfism.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class StormWolfEntity extends Wolf {

    private static final EntityDataAccessor<Boolean> CHARGED =
            SynchedEntityData.defineId(StormWolfEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR;
    private static final int CHARGED_BONE_TAME_CHANCE = 2;

    private int packCallCooldown = 0;

    public StormWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGED, false);
        builder.define(DATA_COLLAR_COLOR, DyeColor.LIGHT_BLUE.getId());
    }

    public boolean isCharged() {
        return this.entityData.get(CHARGED);
    }

    public void setCharged(boolean value) {
        this.entityData.set(CHARGED, value);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new StormWolfPackAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0D, 6.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new StormAggroGoal(this));
        this.targetSelector.addGoal(5, new StormPackAssistGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.packCallCooldown > 0) {
            this.packCallCooldown--;
        }

        if (!this.level().isClientSide) {
            boolean exposed = this.level().isRainingAt(this.blockPosition().above());
            boolean storm = this.level().isThundering() && exposed;
            boolean rain = this.level().isRaining() && exposed;

            this.setCharged(storm || rain);

            AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(storm ? 0.35D : rain ? 0.33D : 0.31D);
            }
        }

        if (this.level().isClientSide && this.isCharged()) {
            if (this.random.nextFloat() < 0.2F) {
                this.level().addParticle(
                        ParticleTypes.ELECTRIC_SPARK,
                        this.getX(),
                        this.getY() + 0.5,
                        this.getZ(),
                        0, 0, 0
                );
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean success = super.doHurtTarget(target);

        if (success && this.isCharged()) {
            target.hurt(this.damageSources().magic(), 2.0F);
        }

        return success;
    }

    public static boolean canSpawn(
            EntityType<StormWolfEntity> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == Level.OVERWORLD
                && pos.getY() >= 64
                && Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (!this.level().isClientSide || this.isBaby() && this.isFood(itemstack)) {
            if (this.isTame()) {
                if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                    FoodProperties foodproperties = itemstack.getFoodProperties(this);
                    float f = foodproperties != null ? (float) foodproperties.nutrition() : 1.0F;
                    this.heal(2.0F * f);
                    itemstack.consume(1, player);
                    this.gameEvent(GameEvent.EAT);
                    return InteractionResult.sidedSuccess(this.level().isClientSide());
                } else {
                    if (item instanceof DyeItem dyeitem) {
                        if (this.isOwnedBy(player)) {
                            DyeColor dyecolor = dyeitem.getDyeColor();
                            if (dyecolor != this.getCollarColor()) {
                                this.setCollarColor(dyecolor);
                                itemstack.consume(1, player);
                                return InteractionResult.SUCCESS;
                            }

                            return super.mobInteract(player, hand);
                        }
                    }

                    if (itemstack.is(Items.WOLF_ARMOR) && this.isOwnedBy(player) && this.getBodyArmorItem().isEmpty() && !this.isBaby()) {
                        this.setBodyArmorItem(itemstack.copyWithCount(1));
                        itemstack.consume(1, player);
                        return InteractionResult.SUCCESS;
                    } else if (!itemstack.canPerformAction(ItemAbilities.SHEARS_REMOVE_ARMOR)
                            || !this.isOwnedBy(player)
                            || !this.hasArmor()
                            || EnchantmentHelper.has(this.getBodyArmorItem(), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) && !player.isCreative()) {
                        if (((Ingredient) ((ArmorMaterial) ArmorMaterials.ARMADILLO.value()).repairIngredient().get()).test(itemstack)
                                && this.isInSittingPose()
                                && this.hasArmor()
                                && this.isOwnedBy(player)
                                && this.getBodyArmorItem().isDamaged()) {
                            itemstack.shrink(1);
                            this.playSound(SoundEvents.WOLF_ARMOR_REPAIR);
                            ItemStack itemstack2 = this.getBodyArmorItem();
                            int i = (int) ((float) itemstack2.getMaxDamage() * 0.125F);
                            itemstack2.setDamageValue(Math.max(0, itemstack2.getDamageValue() - i));
                            return InteractionResult.SUCCESS;
                        } else {
                            InteractionResult interactionresult = super.mobInteract(player, hand);
                            if (!interactionresult.consumesAction() && this.isOwnedBy(player)) {
                                this.setOrderedToSit(!this.isOrderedToSit());
                                this.jumping = false;
                                this.navigation.stop();
                                this.setTarget((LivingEntity) null);
                                return InteractionResult.SUCCESS_NO_ITEM_USED;
                            } else {
                                return interactionresult;
                            }
                        }
                    } else {
                        itemstack.hurtAndBreak(1, player, getSlotForHand(hand));
                        this.playSound(SoundEvents.ARMOR_UNEQUIP_WOLF);
                        ItemStack itemstack1 = this.getBodyArmorItem();
                        this.setBodyArmorItem(ItemStack.EMPTY);
                        this.spawnAtLocation(itemstack1);
                        return InteractionResult.SUCCESS;
                    }
                }
            } else if (itemstack.is(ModItems.CHARGED_BONE) && !this.isAngry()) {
                itemstack.consume(1, player);
                this.tryToTame(player, CHARGED_BONE_TAME_CHANCE);
                return InteractionResult.SUCCESS;
            } else {
                return super.mobInteract(player, hand);
            }
        } else {
            boolean flag = this.isOwnedBy(player)
                    || this.isTame()
                    || itemstack.is(ModItems.CHARGED_BONE) && !this.isTame() && !this.isAngry();
            return flag ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
    }

    private void tryToTame(Player player, int chanceBound) {
        if (this.random.nextInt(chanceBound) == 0 && !EventHooks.onAnimalTame(this, player)) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget((LivingEntity) null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte) 7);
        } else {
            this.level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    @javax.annotation.Nullable
    public StormWolfEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        StormWolfEntity wolf = (StormWolfEntity) ModEntities.STORM_WOLF.get().create(level);
        if (wolf != null && otherParent instanceof StormWolfEntity wolf1) {
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

    public @NotNull DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    public boolean hasArmor() {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR);
    }

    private void setCollarColor(DyeColor collarColor) {
        this.entityData.set(DATA_COLLAR_COLOR, collarColor.getId());
    }

    private static class StormAggroGoal extends NearestAttackableTargetGoal<Player> {
        private final StormWolfEntity wolf;

        public StormAggroGoal(StormWolfEntity wolf) {
            super(wolf, Player.class, true);
            this.wolf = wolf;
        }

        @Override
        public boolean canUse() {
            return !wolf.isTame()
                    && wolf.level().isThundering()
                    && super.canUse();
        }
    }

    private static class StormPackAssistGoal extends Goal {

        private final StormWolfEntity wolf;

        public StormPackAssistGoal(StormWolfEntity wolf) {
            this.wolf = wolf;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return wolf.getTarget() != null
                    && wolf.level().isThundering()
                    && wolf.packCallCooldown <= 0;
        }

        @Override
        public void start() {
            LivingEntity target = wolf.getTarget();
            if (target == null) return;

            List<StormWolfEntity> pack = wolf.level().getEntitiesOfClass(
                    StormWolfEntity.class,
                    wolf.getBoundingBox().inflate(16),
                    w -> w != wolf && w.getTarget() == null && !w.isTame()
            );

            for (StormWolfEntity ally : pack) {
                ally.setTarget(target);
            }

            wolf.packCallCooldown = 80;
        }
    }

    private static class StormWolfPackAttackGoal extends Goal {

        private final StormWolfEntity wolf;
        private final double speed;

        public StormWolfPackAttackGoal(StormWolfEntity wolf, double speed, boolean follow) {
            this.wolf = wolf;
            this.speed = speed;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return wolf.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = wolf.getTarget();
            if (target == null) return;

            wolf.getNavigation().moveTo(target, speed);

            if (wolf.distanceTo(target) < 2.0F) {
                wolf.doHurtTarget(target);
            }
        }
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_AMBIENT;
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    static {
        DATA_COLLAR_COLOR = SynchedEntityData.defineId(StormWolfEntity.class, EntityDataSerializers.INT);
    }
}