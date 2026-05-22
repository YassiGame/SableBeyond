package me.yassigame.sable_beyond.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class SableBeyondCommand {

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext buildContext) {
        final var root = Commands.literal("sable_beyond")
                .requires(source -> source.hasPermission(2));

        SableBeyondMassCommand.addSubcommands(root);
        SableBeyondSystemInfoCommand.addSubcommands(root);
        dispatcher.register(root);
    }
}
