package de.maxi.boatfix;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_BOAT_FIX;

    static {
        BUILDER.push("Boat Fix Settings");

        ENABLE_BOAT_FIX = BUILDER
                .comment("Enable eating while moving in boats")
                .define("enableBoatFix", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
