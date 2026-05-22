package me.yassigame.sable_beyond.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.yassigame.sable_beyond.config.ConfigEditStatus;
import me.yassigame.sable_beyond.config.MassConfigStatus;
import me.yassigame.sable_beyond.platform.LoadedModInfo;
import me.yassigame.sable_beyond.platform.ModPlatform;
import me.yassigame.sable_beyond.utils.HttpUtil;
import me.yassigame.sable_beyond.utils.MCLogApi;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SableBeyondSystemInfoCommand {
    private static final MCLogApi MC_LOG = new MCLogApi();

    public static void addSubcommands(final LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("system_info").executes(context -> get_system_info(context.getSource())));
    }

    public static int get_system_info(final CommandSourceStack source) {
        final List<LoadedModInfo> mods = getSortedLoadedMods();
        final List<ConfigEditStatus> configStatuses = MassConfigStatus.collect(ModPlatform.getConfigPath());
        final String link = MC_LOG.post(buildSystemInfoContent(mods, configStatuses));
        source.sendSuccess(() -> buildSystemInfoMessage(configStatuses, link), true);
        return 1;
    }

    private static List<LoadedModInfo> getSortedLoadedMods() {
        return ModPlatform.getLoadedMods().stream()
                .sorted(Comparator.comparing(LoadedModInfo::id))
                .toList();
    }

    private static Component buildSystemInfoMessage(final List<ConfigEditStatus> configStatuses, final String link) {
        final Runtime runtime = Runtime.getRuntime();
        final MutableComponent message = Component.literal(String.format(Locale.ROOT,
                "Sable Beyond system info\n" +
                        "Minecraft: %s\n" +
                        "Loader: %s\n" +
                        "Java: %s (%s)\n" +
                        "OS: %s %s (%s / %s)\n" +
                        "Memory: %s used / %s max\n" +
                        "CPU threads: %d\n" +
                        "Config edited: %s\n" +
                        "Full report: ",
                SharedConstants.getCurrentVersion().getName(),
                ModPlatform.getLoaderName(),
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                Util.getPlatform().name(),
                formatBytes(runtime.totalMemory() - runtime.freeMemory()),
                formatBytes(runtime.maxMemory()),
                runtime.availableProcessors(),
                hasEditedConfig(configStatuses) ? "yes" : "no"));

        return appendLinkOrError(message, link, "Open full system report");
    }

    private static Component buildConfigStatusMessage(final List<ConfigEditStatus> configStatuses) {
        final MutableComponent message = Component.literal("Sable Beyond config status:");
        for (final ConfigEditStatus status : configStatuses) {
            message.append(Component.literal("\n- " + status.name() + ": "));
            message.append(Component.literal(status.state().name()).withStyle(colorFor(status.state())));
            message.append(Component.literal(" (" + status.detail() + ")"));
        }
        return message;
    }

    private static ChatFormatting colorFor(final ConfigEditStatus.State state) {
        return switch (state) {
            case DEFAULT -> ChatFormatting.GREEN;
            case EDITED -> ChatFormatting.YELLOW;
            case MISSING, INVALID -> ChatFormatting.RED;
        };
    }

    private static boolean hasEditedConfig(final List<ConfigEditStatus> configStatuses) {
        return configStatuses.stream().anyMatch(ConfigEditStatus::isEdited);
    }

    private static String buildSystemInfoContent(
            final List<LoadedModInfo> mods,
            final List<ConfigEditStatus> configStatuses
    ) {
        final Runtime runtime = Runtime.getRuntime();
        final StringBuilder content = new StringBuilder();
        content.append("system_info:")
                .append('\n')
                .append("minecraft_version: ")
                .append(SharedConstants.getCurrentVersion().getName())
                .append('\n')
                .append("loader: ")
                .append(ModPlatform.getLoaderName())
                .append('\n')
                .append("java_version: ")
                .append(System.getProperty("java.version"))
                .append('\n')
                .append("java_vendor: ")
                .append(System.getProperty("java.vendor"))
                .append('\n')
                .append("jvm: ")
                .append(System.getProperty("java.vm.name"))
                .append('\n')
                .append("os: ")
                .append(System.getProperty("os.name"))
                .append(' ')
                .append(System.getProperty("os.version"))
                .append(" (")
                .append(System.getProperty("os.arch"))
                .append(")")
                .append('\n')
                .append("mc_os: ")
                .append(Util.getPlatform().name())
                .append('\n')
                .append("cpu_threads: ")
                .append(runtime.availableProcessors())
                .append('\n')
                .append("memory_used_mb: ")
                .append(toMegabytes(runtime.totalMemory() - runtime.freeMemory()))
                .append('\n')
                .append("memory_committed_mb: ")
                .append(toMegabytes(runtime.totalMemory()))
                .append('\n')
                .append("memory_max_mb: ")
                .append(toMegabytes(runtime.maxMemory()))
                .append('\n')
                .append('\n');

        appendConfigStatusContent(content, configStatuses);
        content.append('\n');
        appendModListContent(content, mods);
        return content.toString();
    }

    private static String buildModListContent(final List<LoadedModInfo> mods) {
        final StringBuilder content = new StringBuilder();
        content.append("version: ")
                .append(SharedConstants.getCurrentVersion().getName())
                .append('\n');
        appendModListContent(content, mods);
        return content.toString();
    }

    private static void appendConfigStatusContent(
            final StringBuilder content,
            final List<ConfigEditStatus> configStatuses
    ) {
        content.append("config_status:")
                .append('\n');
        for (final ConfigEditStatus status : configStatuses) {
            content.append("- ")
                    .append(status.name())
                    .append(": ")
                    .append(status.state())
                    .append(" | edited=")
                    .append(status.isEdited())
                    .append(" | detail=")
                    .append(status.detail())
                    .append('\n');
        }
    }

    private static void appendModListContent(final StringBuilder content, final List<LoadedModInfo> mods) {
        content.append("mod_list:")
                .append('\n');
        for (final LoadedModInfo mod : mods) {
            content.append("- ")
                    .append(mod.id())
                    .append(": ")
                    .append(mod.version())
                    .append('\n');
        }
    }

    private static String buildModListText(final List<LoadedModInfo> mods) {
        return mods.stream()
                .map(mod -> mod.id() + ":" + mod.version())
                .toList()
                .toString();
    }

    private static Component buildModListMessage(final String modsText, final String link) {
        final MutableComponent message = Component.literal(String.format(Locale.ROOT,
                "The mod list: %s. \nlink of your mod list: ",
                modsText));

        return appendLinkOrError(message, link, "Open mod list");
    }

    private static Component appendLinkOrError(
            final MutableComponent message,
            final String link,
            final String hoverText
    ) {
        if (!HttpUtil.isHttpUrl(link)) {
            return message.append(Component.literal(link).withStyle(ChatFormatting.RED));
        }

        return message.append(Component.literal(link).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverText)))));
    }

    private static String formatBytes(final long bytes) {
        return toMegabytes(bytes) + " MB";
    }

    private static long toMegabytes(final long bytes) {
        return bytes / 1024L / 1024L;
    }
}
