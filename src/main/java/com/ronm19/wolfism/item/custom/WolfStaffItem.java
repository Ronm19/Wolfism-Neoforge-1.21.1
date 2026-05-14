package com.ronm19.wolfism.item.custom;

import com.ronm19.wolfism.entity.command.WolfismCommand;
import com.ronm19.wolfism.entity.custom.base.WolfismWolfEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class WolfStaffItem extends Item {
    private static final double COMMAND_RANGE = 50.0D;
    private static final int COMMAND_COOLDOWN_TICKS = 7;

    public WolfStaffItem(Properties properties) {
        super(properties);
    }

    /*
     * Fallback direct entity click support.
     *
     * The main reliable path is WolfismWolfEntity#mobInteract,
     * but keeping this here does not hurt.
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof WolfismWolfEntity wolf)) {
            return InteractionResult.PASS;
        }

        return this.handleWolfEntityClick(player.level(), player, wolf);
    }

    /*
     * Called from WolfismWolfEntity before vanilla wolf interaction can steal the click.
     *
     * Right-click wolf = basic command cycle for one wolf.
     * Sneak + right-click wolf = special same-type squad toggle.
     */
    public InteractionResult handleWolfEntityClick(Level level, Player player, WolfismWolfEntity wolf) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            return this.handleSameTypeSquadCommand(level, player, wolf);
        }

        return this.handleSingleWolfBasicCommand(level, player, wolf);
    }

    /*
     * Air use:
     *
     * Right-click air = basic command cycle for all nearby owned wolves.
     * Sneak + right-click air = recall all nearby owned wolves to FOLLOW.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.success(stack);
        }

        List<WolfismWolfEntity> ownedWolves = getOwnedWolvesNearby(level, player);

        if (ownedWolves.isEmpty()) {
            sendActionBar(player,
                    Component.literal("No owned Wolfism wolves nearby.")
                            .withStyle(ChatFormatting.DARK_RED)
            );

            this.addCooldown(player);
            return InteractionResultHolder.success(stack);
        }

        if (player.isShiftKeyDown()) {
            this.handleRecallAllCommand(level, player, ownedWolves);
            return InteractionResultHolder.success(stack);
        }

        this.handleAllNearbyBasicCommand(level, player, ownedWolves);
        return InteractionResultHolder.success(stack);
    }

    /*
     * Normal right-click wolf:
     *
     * Basic command cycle only.
     * This intentionally skips HUNT / PROTECT / PRESSURE / etc.
     */
    private InteractionResult handleSingleWolfBasicCommand(Level level, Player player, WolfismWolfEntity wolf) {
        if (!canCommandWolf(player, wolf)) {
            this.addCooldown(player);
            return InteractionResult.SUCCESS;
        }

        WolfismCommand nextCommand = getNextBasicCommand(wolf.getWolfismCommand());
        applyCommandToWolf(player, wolf, nextCommand, false);

        sendActionBar(player,
                Component.literal("Wolf: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(nextCommand.getDisplayName())
                                .withStyle(getCommandColor(nextCommand)))
        );

        playStaffSound(level, player, nextCommand);
        this.addCooldown(player);

        return InteractionResult.SUCCESS;
    }

    /*
     * Normal right-click air:
     *
     * Basic command cycle for all nearby owned Wolfism wolves.
     *
     * If all nearby wolves are synced on a basic command:
     * FOLLOW -> HOLD -> GUARD_POSITION -> FOLLOW
     *
     * If they are mixed/out of sync/some are in special commands:
     * first click syncs all back to FOLLOW.
     */
    private void handleAllNearbyBasicCommand(Level level, Player player, List<WolfismWolfEntity> ownedWolves) {
        WolfismCommand nextCommand = getNextBasicGroupCommand(ownedWolves);

        this.applyCommandToGroup(player, ownedWolves, nextCommand, nextCommand == WolfismCommand.FOLLOW);

        sendActionBar(player,
                Component.literal("Pack: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(nextCommand.getDisplayName())
                                .withStyle(getCommandColor(nextCommand)))
                        .append(Component.literal(" [" + ownedWolves.size() + "]")
                                .withStyle(ChatFormatting.DARK_GRAY))
        );

        playStaffSound(level, player, nextCommand);
        this.addCooldown(player);
    }

    /*
     * Sneak + right-click wolf:
     *
     * If this wolf type has a special command:
     * - toggles same-type squad into special command
     * - if all are already in special command, returns them to FOLLOW
     *
     * If this wolf type has no special command:
     * - cycles same-type squad through basic commands only
     */
    private InteractionResult handleSameTypeSquadCommand(Level level, Player player, WolfismWolfEntity referenceWolf) {
        if (!canCommandWolf(player, referenceWolf)) {
            this.addCooldown(player);
            return InteractionResult.SUCCESS;
        }

        List<WolfismWolfEntity> squad = getOwnedWolvesOfSameTypeNearby(level, player, referenceWolf);

        if (squad.isEmpty()) {
            sendActionBar(player,
                    Component.literal("No same-type wolves nearby.")
                            .withStyle(ChatFormatting.DARK_RED)
            );

            this.addCooldown(player);
            return InteractionResult.SUCCESS;
        }

        WolfismCommand specialCommand = getPrimarySpecialCommand(referenceWolf);

        if (specialCommand != null) {
            WolfismCommand commandToApply = areAllWolvesInCommand(squad, specialCommand)
                    ? WolfismCommand.FOLLOW
                    : specialCommand;

            this.applyCommandToGroup(player, squad, commandToApply, commandToApply == WolfismCommand.FOLLOW);

            sendActionBar(player,
                    Component.literal("Squad special: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(commandToApply.getDisplayName())
                                    .withStyle(getCommandColor(commandToApply)))
                            .append(Component.literal(" [" + squad.size() + "]")
                                    .withStyle(ChatFormatting.DARK_GRAY))
            );

            playStaffSound(level, player, commandToApply);
            this.addCooldown(player);

            return InteractionResult.SUCCESS;
        }

        /*
         * Fallback for wolves with no special command:
         * same-type squad basic cycle only.
         */
        WolfismCommand nextCommand = getNextBasicGroupCommand(squad);
        this.applyCommandToGroup(player, squad, nextCommand, nextCommand == WolfismCommand.FOLLOW);

        sendActionBar(player,
                Component.literal("Squad: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(nextCommand.getDisplayName())
                                .withStyle(getCommandColor(nextCommand)))
                        .append(Component.literal(" [" + squad.size() + "]")
                                .withStyle(ChatFormatting.DARK_GRAY))
        );

        playStaffSound(level, player, nextCommand);
        this.addCooldown(player);

        return InteractionResult.SUCCESS;
    }

    /*
     * Basic cycle:
     *
     * FOLLOW -> HOLD -> GUARD_POSITION -> FOLLOW
     *
     * Any special/custom command returns to FOLLOW.
     */
    private static WolfismCommand getNextBasicCommand(WolfismCommand current) {
        return switch (current) {
            case FOLLOW -> WolfismCommand.HOLD;
            case HOLD -> WolfismCommand.GUARD_POSITION;
            case GUARD_POSITION -> WolfismCommand.FOLLOW;

            case HUNT,
                 PROTECT,
                 NOBLE_GUARD,
                 PRESSURE,
                 ROYAL_COMMAND,
                 NIGHT_WATCH -> WolfismCommand.FOLLOW;
        };
    }

    /*
     * Group basic cycle:
     *
     * If the group is synced on a basic command, advance together.
     * If not synced, force everyone to FOLLOW first.
     */
    private static WolfismCommand getNextBasicGroupCommand(List<WolfismWolfEntity> wolves) {
        if (wolves.isEmpty()) {
            return WolfismCommand.FOLLOW;
        }

        if (!areAllWolvesInSameBasicCommand(wolves)) {
            return WolfismCommand.FOLLOW;
        }

        return getNextBasicCommand(wolves.get(0).getWolfismCommand());
    }

    private static boolean areAllWolvesInSameBasicCommand(List<WolfismWolfEntity> wolves) {
        if (wolves.isEmpty()) {
            return true;
        }

        WolfismCommand firstCommand = wolves.get(0).getWolfismCommand();

        if (!isBasicCommand(firstCommand)) {
            return false;
        }

        for (WolfismWolfEntity wolf : wolves) {
            if (wolf.getWolfismCommand() != firstCommand) {
                return false;
            }

            if (!isBasicCommand(wolf.getWolfismCommand())) {
                return false;
            }
        }

        return true;
    }

    private static boolean isBasicCommand(WolfismCommand command) {
        return command == WolfismCommand.FOLLOW
                || command == WolfismCommand.HOLD
                || command == WolfismCommand.GUARD_POSITION;
    }

    @Nullable
    private static WolfismCommand getPrimarySpecialCommand(WolfismWolfEntity wolf) {
        if (wolf.supportsCommand(WolfismCommand.HUNT)) {
            return WolfismCommand.HUNT;
        }

        if (wolf.supportsCommand(WolfismCommand.PROTECT)) {
            return WolfismCommand.PROTECT;
        }

        if (wolf.supportsCommand(WolfismCommand.NOBLE_GUARD)) {
            return WolfismCommand.NOBLE_GUARD;
        }

        if (wolf.supportsCommand(WolfismCommand.PRESSURE)) {
            return WolfismCommand.PRESSURE;
        }

        if (wolf.supportsCommand(WolfismCommand.ROYAL_COMMAND)) {
            return WolfismCommand.ROYAL_COMMAND;
        }

        if (wolf.supportsCommand(WolfismCommand.NIGHT_WATCH)) {
            return WolfismCommand.NIGHT_WATCH;
        }

        return null;
    }

    private static boolean areAllWolvesInCommand(List<WolfismWolfEntity> wolves, WolfismCommand command) {
        for (WolfismWolfEntity wolf : wolves) {
            if (wolf.getWolfismCommand() != command) {
                return false;
            }
        }

        return true;
    }

    private void handleRecallAllCommand(Level level, Player player, List<WolfismWolfEntity> ownedWolves) {
        this.applyCommandToGroup(player, ownedWolves, WolfismCommand.FOLLOW, true);

        sendActionBar(player,
                Component.literal("Recalled ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.valueOf(ownedWolves.size()))
                                .withStyle(ChatFormatting.GREEN))
                        .append(Component.literal(" wolves")
                                .withStyle(ChatFormatting.GRAY))
        );

        playStaffSound(level, player, WolfismCommand.FOLLOW);
        this.addCooldown(player);
    }

    private static boolean canCommandWolf(Player player, WolfismWolfEntity wolf) {
        if (!wolf.isTame()) {
            sendActionBar(player,
                    Component.literal("This wolf is not tamed.")
                            .withStyle(ChatFormatting.RED)
            );
            return false;
        }

        if (!wolf.isOwnedBy(player)) {
            sendActionBar(player,
                    Component.literal("This wolf does not obey you.")
                            .withStyle(ChatFormatting.RED)
            );
            return false;
        }

        return true;
    }

    private void applyCommandToGroup(Player player, List<WolfismWolfEntity> wolves, WolfismCommand command, boolean recallToPlayer) {
        for (WolfismWolfEntity wolf : wolves) {
            applyCommandToWolf(player, wolf, command, recallToPlayer);
        }
    }

    private static void applyCommandToWolf(Player player, WolfismWolfEntity wolf, WolfismCommand command, boolean recallToPlayer) {
        wolf.setWolfismCommand(command);
        wolf.setTarget(null);

        if (command == WolfismCommand.FOLLOW || recallToPlayer) {
            wolf.getNavigation().moveTo(player, 1.25D);
        }
    }

    private static List<WolfismWolfEntity> getOwnedWolvesNearby(Level level, Player player) {
        return level.getEntitiesOfClass(
                WolfismWolfEntity.class,
                player.getBoundingBox().inflate(COMMAND_RANGE),
                wolf -> wolf.isAlive()
                        && wolf.isTame()
                        && wolf.isOwnedBy(player)
        );
    }

    private static List<WolfismWolfEntity> getOwnedWolvesOfSameTypeNearby(Level level, Player player, WolfismWolfEntity referenceWolf) {
        EntityType<?> targetType = referenceWolf.getType();

        return level.getEntitiesOfClass(
                WolfismWolfEntity.class,
                player.getBoundingBox().inflate(COMMAND_RANGE),
                wolf -> wolf.isAlive()
                        && wolf.isTame()
                        && wolf.isOwnedBy(player)
                        && wolf.getType() == targetType
        );
    }

    private static ChatFormatting getCommandColor(WolfismCommand command) {
        return switch (command) {
            case FOLLOW -> ChatFormatting.GREEN;
            case HOLD -> ChatFormatting.YELLOW;
            case GUARD_POSITION -> ChatFormatting.AQUA;
            case HUNT -> ChatFormatting.RED;

            case PROTECT -> ChatFormatting.LIGHT_PURPLE;
            case NOBLE_GUARD -> ChatFormatting.GOLD;
            case PRESSURE -> ChatFormatting.DARK_PURPLE;
            case ROYAL_COMMAND -> ChatFormatting.DARK_RED;
            case NIGHT_WATCH -> ChatFormatting.BLUE;
        };
    }

    private static void playStaffSound(Level level, Player player, WolfismCommand command) {
        float pitch = switch (command) {
            case FOLLOW -> 1.2F;
            case HOLD -> 0.9F;
            case GUARD_POSITION -> 1.0F;
            case HUNT -> 0.65F;

            case PROTECT -> 1.35F;
            case NOBLE_GUARD -> 0.85F;
            case PRESSURE -> 0.55F;
            case ROYAL_COMMAND -> 0.75F;
            case NIGHT_WATCH -> 1.15F;
        };

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WOLF_AMBIENT,
                SoundSource.PLAYERS,
                0.5F,
                pitch
        );
    }

    private static void sendActionBar(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private void addCooldown(Player player) {
        player.getCooldowns().addCooldown(this, COMMAND_COOLDOWN_TICKS);
    }
}