package me.yassigame.sable_beyond.config.cloth;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ClothButtonRowEntry extends AbstractConfigListEntry<Object> {
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_GAP = 3;
    private static final int ENTRY_PADDING = 2;
    private static final int MIN_BUTTON_WIDTH = 34;
    private static final int MAX_BUTTON_WIDTH = 110;
    private static final int HORIZONTAL_PADDING = 10;
    private static final int ICON_SIZE = 10;
    private static final int CONTENT_GAP = 4;

    private final List<IconUrlButton> buttons;

    public ClothButtonRowEntry(final List<ButtonLink> links) {
        super(Component.empty(), false);
        this.buttons = links.stream()
                .map(link -> new IconUrlButton(link))
                .toList();
    }

    @Override
    public void render(
            final GuiGraphics graphics,
            final int index,
            final int y,
            final int x,
            final int entryWidth,
            final int entryHeight,
            final int mouseX,
            final int mouseY,
            final boolean hovered,
            final float delta
    ) {
        if (buttons.isEmpty()) {
            return;
        }

        final List<Integer> buttonWidths = calculateButtonWidths(entryWidth);
        final int rowWidth = buttonWidths.stream().mapToInt(Integer::intValue).sum()
                + BUTTON_GAP * (buttons.size() - 1);
        final int startX = x + (entryWidth - rowWidth) / 2;
        final int buttonY = y + ENTRY_PADDING;
        int buttonX = startX;

        for (int i = 0; i < buttons.size(); i++) {
            final Button button = buttons.get(i);
            final int buttonWidth = buttonWidths.get(i);
            button.setRectangle(buttonWidth, BUTTON_HEIGHT, buttonX, buttonY);
            button.render(graphics, mouseX, mouseY, delta);
            buttonX += buttonWidth + BUTTON_GAP;
        }
    }

    private List<Integer> calculateButtonWidths(final int entryWidth) {
        final List<Integer> widths = buttons.stream()
                .map(IconUrlButton::preferredWidth)
                .toList();
        final int gapTotal = BUTTON_GAP * (buttons.size() - 1);
        final int rowWidth = widths.stream().mapToInt(Integer::intValue).sum() + gapTotal;
        if (rowWidth <= entryWidth) {
            return widths;
        }

        final int sharedWidth = Math.max(MIN_BUTTON_WIDTH, (entryWidth - gapTotal) / buttons.size());
        return buttons.stream()
                .map(button -> sharedWidth)
                .toList();
    }

    @Override
    public int getItemHeight() {
        return BUTTON_HEIGHT + ENTRY_PADDING * 2;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return new ArrayList<>(buttons);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return new ArrayList<>(buttons);
    }

    public record ButtonLink(Component label, String url, @Nullable ButtonIcon icon) {
        public static ButtonLink of(final Component label, final String url) {
            return new ButtonLink(label, url, null);
        }

        public static ButtonLink of(final Component label, final String url, final ResourceLocation icon) {
            return new ButtonLink(label, url, ButtonIcon.of(icon));
        }

        public static ButtonLink of(final Component label, final String url, final ButtonIcon icon) {
            return new ButtonLink(label, url, icon);
        }
    }

    public record ButtonIcon(ResourceLocation texture, int textureWidth, int textureHeight) {
        public static ButtonIcon of(final ResourceLocation texture) {
            return new ButtonIcon(texture, 16, 16);
        }

        public static ButtonIcon of(final ResourceLocation texture, final int textureWidth, final int textureHeight) {
            return new ButtonIcon(texture, textureWidth, textureHeight);
        }
    }

    private static final class IconUrlButton extends Button {
        private final ButtonLink link;

        private IconUrlButton(final ButtonLink link) {
            super(0, 0, MAX_BUTTON_WIDTH, BUTTON_HEIGHT, link.label(),
                    button -> Util.getPlatform().openUri(link.url()),
                    DEFAULT_NARRATION);
            this.link = link;
        }

        private int preferredWidth() {
            int width = Minecraft.getInstance().font.width(getMessage()) + HORIZONTAL_PADDING * 2;
            if (link.icon() != null) {
                width += ICON_SIZE + CONTENT_GAP;
            }

            return Math.max(MIN_BUTTON_WIDTH, Math.min(MAX_BUTTON_WIDTH, width));
        }

        @Override
        protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float delta) {
            final ButtonIcon icon = link.icon();
            if (icon == null) {
                super.renderWidget(graphics, mouseX, mouseY, delta);
                return;
            }

            final Component originalMessage = getMessage();
            setMessage(Component.empty());
            super.renderWidget(graphics, mouseX, mouseY, delta);
            setMessage(originalMessage);

            final int textWidth = Minecraft.getInstance().font.width(getMessage());
            final int contentWidth = ICON_SIZE + CONTENT_GAP + textWidth;
            final int iconX = getX() + Math.max(4, (getWidth() - contentWidth) / 2);
            final int iconY = getY() + (getHeight() - ICON_SIZE) / 2;
            graphics.blit(icon.texture(), iconX, iconY, ICON_SIZE, ICON_SIZE,
                    0.0F, 0.0F, icon.textureWidth(), icon.textureHeight(), icon.textureWidth(), icon.textureHeight());
            graphics.drawString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    iconX + ICON_SIZE + CONTENT_GAP,
                    getY() + (getHeight() - 8) / 2,
                    active ? 0xFFFFFF : 0xA0A0A0,
                    false
            );
        }
    }
}
