package com.ronm19.wolfism.entity.command;

public enum WolfismCommand {
    FOLLOW(0, "Follow"),
    HOLD(1, "Hold"),
    GUARD_POSITION(2, "Guard Position"),
    HUNT(3, "Hunt"),

    PROTECT(4, "Protect"),
    NOBLE_GUARD(5, "Noble Guard"),
    PRESSURE(6, "Pressure"),
    ROYAL_COMMAND(7, "Royal Command"),
    NIGHT_WATCH(8, "Night Watch");

    private final int id;
    private final String displayName;

    WolfismCommand(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static WolfismCommand byId(int id) {
        for (WolfismCommand command : values()) {
            if (command.id == id) {
                return command;
            }
        }

        return FOLLOW;
    }

    public WolfismCommand next() {
        WolfismCommand[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}