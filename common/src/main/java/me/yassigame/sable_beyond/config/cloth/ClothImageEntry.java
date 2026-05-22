package me.yassigame.sable_beyond.config.cloth;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class ClothImageEntry extends AbstractConfigListEntry<Object> {
    private final ResourceLocation texture;
    private final int imageWidth;
    private final int imageHeight;
    private final int textureWidth;
    private final int textureHeight;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final int frameColumns;
    private final long frameDurationMillis;

    public ClothImageEntry(
            final ResourceLocation texture,
            final int imageWidth,
            final int imageHeight,
            final int textureWidth,
            final int textureHeight
    ) {
        this(texture, imageWidth, imageHeight, textureWidth, textureHeight, textureWidth, textureHeight, 1, 1, 0L);
    }

    public ClothImageEntry(
            final ResourceLocation texture,
            final int imageWidth,
            final int imageHeight,
            final int textureWidth,
            final int textureHeight,
            final int frameWidth,
            final int frameHeight,
            final int frameCount,
            final int frameColumns,
            final long frameDurationMillis
    ) {
        super(Component.empty(), false);
        this.texture = texture;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.frameWidth = Math.max(1, frameWidth);
        this.frameHeight = Math.max(1, frameHeight);
        this.frameCount = Math.max(1, frameCount);
        this.frameColumns = Math.max(1, frameColumns);
        this.frameDurationMillis = Math.max(0L, frameDurationMillis);
    }

    public static ClothImageEntry spritesheetGrid(
            final ResourceLocation texture,
            final int imageWidth,
            final int imageHeight,
            final int frameWidth,
            final int frameHeight,
            final int frameCount,
            final int frameColumns,
            final long frameDurationMillis
    ) {
        final int columns = Math.max(1, frameColumns);
        final int rows = Math.max(1, (int) Math.ceil((double) Math.max(1, frameCount) / columns));

        return new ClothImageEntry(
                texture,
                imageWidth,
                imageHeight,
                frameWidth * columns,
                frameHeight * rows,
                frameWidth,
                frameHeight,
                frameCount,
                columns,
                frameDurationMillis
        );
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
        final int imageX = x + (entryWidth - imageWidth) / 2;
        final int imageY = y + 4;
        final int frame = currentFrame();
        final float u = (float) ((frame % frameColumns) * frameWidth);
        final float v = (float) ((frame / frameColumns) * frameHeight);

        graphics.blit(texture, imageX, imageY, imageWidth, imageHeight,
                u, v, frameWidth, frameHeight, textureWidth, textureHeight);
    }

    private int currentFrame() {
        if (frameCount <= 1 || frameDurationMillis <= 0L) {
            return 0;
        }

        return (int) ((System.currentTimeMillis() / frameDurationMillis) % frameCount);
    }

    @Override
    public int getItemHeight() {
        return imageHeight + 8;
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
        return List.of();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of();
    }
}
