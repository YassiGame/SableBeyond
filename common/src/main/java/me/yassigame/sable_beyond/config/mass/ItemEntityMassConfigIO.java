package me.yassigame.sable_beyond.config.mass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.api.mass.MassRegistry;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ItemEntityMassConfigIO {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static void loadIntoRegistry(final Path configPath) {
        MassRegistry.applyItemEntityConfig(load(configPath));
    }

    public static ItemEntityMassConfigData load(final Path configPath) {
        final ItemEntityMassConfigData defaults = ItemEntityMassConfigData.defaults();

        try {
            Files.createDirectories(configPath.getParent());

            if (Files.notExists(configPath)) {
                Files.writeString(configPath, GSON.toJson(defaults), StandardCharsets.UTF_8);
            }

            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                final ItemEntityMassConfigData loaded = GSON.fromJson(reader, ItemEntityMassConfigData.class);
                if (loaded == null) {
                    SableBeyond.LOGGER.warn("Item entity mass config at {} was empty, using defaults.", configPath);
                    return defaults;
                }

                return loaded;
            }
        } catch (final Exception exception) {
            SableBeyond.LOGGER.error("Failed to load item entity mass config at {}, using defaults.", configPath, exception);
            return defaults;
        }
    }

}
