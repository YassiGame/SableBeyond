package me.yassigame.sable_beyond.gui;

import me.yassigame.sable_beyond.config.SableBeyondConfig;
import me.yassigame.sable_beyond.platform.ModPlatform;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;

import java.util.List;
import java.util.Optional;

public final class SableBeyondMenuButtonPlacer {
    private static final int BUTTON_SIZE = 20;
    private static final int OFFSET = 4;
    private static final MenuRows MAIN_MENU = new MenuRows(List.of(
            new MenuRow("menu.singleplayer"),
            new MenuRow("menu.multiplayer"),
            new MenuRow("fml.menu.mods", "menu.online"),
            new MenuRow("narrator.button.language", "narrator.button.accessibility")
    ));
    private static final MenuRows PAUSE_MENU = new MenuRows(List.of(
            new MenuRow("menu.returnToGame"),
            new MenuRow("gui.advancements", "gui.stats"),
            new MenuRow("menu.sendFeedback", "menu.reportBugs"),
            new MenuRow("menu.options", "menu.shareToLan"),
            new MenuRow("menu.returnToMenu")
    ));

    public static Optional<SableBeyondIconButton> createButton(final Screen screen, final Iterable<? extends GuiEventListener> listeners) {
        if (screen instanceof TitleScreen && SableBeyondConfig.other().buton_on_mainmenu) {
            return createButtonNextTo(listeners, MAIN_MENU, 0, false);
        }

        if (screen instanceof PauseScreen && SableBeyondConfig.other().buton_on_pausemenu) {
            return createButtonNextTo(listeners, PAUSE_MENU, 0, false);
        }

        return Optional.empty();
    }

    private static Optional<SableBeyondIconButton> createButtonNextTo(
            final Iterable<? extends GuiEventListener> listeners,
            final MenuRows menuRows,
            final int rowIndex,
            final boolean leftButton
    ) {
        if (rowIndex < 0 || rowIndex >= menuRows.rows().size()) {
            return Optional.empty();
        }

        final MenuRow row = menuRows.rows().get(rowIndex);
        final String targetMessage = I18n.get(leftButton ? row.leftTextKey() : row.rightTextKey());
        for (final GuiEventListener listener : listeners) {
            if (!(listener instanceof final AbstractWidget widget)) {
                continue;
            }

            if (!widget.getMessage().getString().equals(targetMessage)) {
                continue;
            }

            return Optional.of(new SableBeyondIconButton(
                    widget.getX() + widget.getWidth() + OFFSET,
                    widget.getY(),
                    button -> ModPlatform.openConfigScreen()
            ));
        }

        return Optional.empty();
    }

    private record MenuRow(String leftTextKey, String rightTextKey) {
        private MenuRow(final String centeredTextKey) {
            this(centeredTextKey, centeredTextKey);
        }
    }

    private record MenuRows(List<MenuRow> rows) {
    }
}
