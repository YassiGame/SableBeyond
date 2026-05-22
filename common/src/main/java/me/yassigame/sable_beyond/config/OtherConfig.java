package me.yassigame.sable_beyond.config;

import me.yassigame.sable_beyond.SableBeyond;

import java.nio.file.Path;

public final class OtherConfig {
    public static final String FILE_NAME = "other.json";

    public boolean buton_on_mainmenu = true;
    public boolean buton_on_pausemenu = true;

    public static OtherConfig defaults() {
        return new OtherConfig();
    }

    public static OtherConfig load(final Path configDirectory) {
        return JsonConfigFile.load(filePath(configDirectory), OtherConfig.class, defaults());
    }

    public void save(final Path configDirectory) {
        JsonConfigFile.save(filePath(configDirectory), this);
    }

    public static Path filePath(final Path configDirectory) {
        return configDirectory.resolve(SableBeyond.MOD_ID).resolve(FILE_NAME);
    }
}
