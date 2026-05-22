package me.yassigame.sable_beyond.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MassConfigStatus {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<ConfigEditStatus> collect(final Path configRoot) {
        return List.of(
                inspect("dynamic_mass", configRoot.resolve(DynamicMassConfig.FILE_NAME),
                        DynamicMassConfig.class, DynamicMassConfig.defaults()),
                inspect("entity_mass", configRoot.resolve(EntityMassConfig.FILE_NAME),
                        EntityMassConfig.class, EntityMassConfig.defaults())
        );
    }

    private static <T> ConfigEditStatus inspect(
            final String name,
            final Path path,
            final Class<T> type,
            final T defaults
    ) {
        if (Files.notExists(path)) {
            return new ConfigEditStatus(name, path, ConfigEditStatus.State.MISSING,
                    "file is missing; defaults are active until the config is generated");
        }

        try {
            final String content = Files.readString(path, StandardCharsets.UTF_8);
            final T loaded = GSON.fromJson(content, type);
            if (loaded == null) {
                return new ConfigEditStatus(name, path, ConfigEditStatus.State.INVALID, "file is empty");
            }

            if (GSON.toJson(defaults).equals(GSON.toJson(loaded))) {
                return new ConfigEditStatus(name, path, ConfigEditStatus.State.DEFAULT, "matches defaults");
            }

            return new ConfigEditStatus(name, path, ConfigEditStatus.State.EDITED, "differs from defaults");
        } catch (IOException | RuntimeException exception) {
            return new ConfigEditStatus(name, path, ConfigEditStatus.State.INVALID, exception.getMessage());
        }
    }
}
