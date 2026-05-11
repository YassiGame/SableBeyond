package me.yassigame.sable_beyond.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class SableBeyondSystemInfo {

    public static void addSubcommands(final LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("system_info")
                .then(Commands.literal("entity")
                        .executes(context -> get_system_info())));
    }

    public static int get_system_info() {
        return 1;
    }
}
