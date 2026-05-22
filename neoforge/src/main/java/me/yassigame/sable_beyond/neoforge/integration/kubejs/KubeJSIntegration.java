package me.yassigame.sable_beyond.neoforge.integration.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import me.yassigame.sable_beyond.SableBeyond;
import me.yassigame.sable_beyond.neoforge.integration.kubejs.bindings.SableBeyondEntityMassJS;

public class KubeJSIntegration implements KubeJSPlugin {
    @Override
    public void init() {
        KubeJSPlugin.super.init();
        SableBeyond.LOGGER.info("KubeJS integration loaded ;)");
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        switch (bindings.type()) {
            case SERVER -> {
                bindings.add("SableBeyondEntityMass", new SableBeyondEntityMassJS());
            }
        }
    }
}
