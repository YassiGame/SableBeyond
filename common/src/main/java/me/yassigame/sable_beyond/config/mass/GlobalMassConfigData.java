package me.yassigame.sable_beyond.config.mass;

import java.util.List;

// TODO adding a threshold for the mass to not exceed

public final class GlobalMassConfigData {
    public List<String> _comment = List.of(
            "enabled -> toggles the entire sublevel mass application system"
    );
    public boolean enabled = true;

    public List<String> _comment_experimental = List.of(
            "activate this two settings to have hello neighbor ahhhhh physics",
            "-> experimental player mass is trash for now",
            "-> experimental player sublevel interaction can be cool but very buggy"
    );
    public boolean experimental_player_mass = false;
    public boolean experimental_player_sublevel_interaction = false;

    public static GlobalMassConfigData defaults() {
        return new GlobalMassConfigData();
    }
}
