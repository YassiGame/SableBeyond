package me.yassigame.sable_beyond.config;

import java.nio.file.Path;

public record ConfigEditStatus(String name, Path path, State state, String detail) {
    public enum State {
        DEFAULT,
        EDITED,
        MISSING,
        INVALID
    }

    public boolean isEdited() {
        return state == State.EDITED || state == State.INVALID;
    }
}
