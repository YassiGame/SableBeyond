package me.yassigame.sable_beyond.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.yassigame.sable_beyond.SableBeyond;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonConfigFile {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static <T> T load(final Path path, final Class<T> type, final T defaults) {
        try {
            Files.createDirectories(path.getParent());

            if (Files.notExists(path)) {
                save(path, defaults);
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                final T loaded = GSON.fromJson(reader, type);
                if (loaded == null) {
                    SableBeyond.LOGGER.warn("Config at {} was empty, using defaults.", path);
                    save(path, defaults);
                    return defaults;
                }

                save(path, loaded);
                return loaded;
            }
        } catch (Exception exception) {
            SableBeyond.LOGGER.error("Failed to load config at {}, using defaults.", path, exception);
            save(path, defaults);
            return defaults;
        }
    }

    public static void save(final Path path, final Object config) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            SableBeyond.LOGGER.error("Failed to save config at {}", path, exception);
        }
    }
}
