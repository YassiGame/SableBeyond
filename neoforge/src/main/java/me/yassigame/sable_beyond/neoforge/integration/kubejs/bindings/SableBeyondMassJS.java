package me.yassigame.sable_beyond.neoforge.integration.kubejs.bindings;

import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import me.yassigame.sable_beyond.api.mass.MassRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class SableBeyondMassJS {

    @Info(
            value = "Resolve the mass of an entity",
            params = {
                    @Param(name = "entity", value = "The entity to resolve the mass from")
            }
    )
    public double resolveMass(Entity entity) {
        return MassRegistry.resolveMass(entity);
    }

    @Info(
            value = "Resolve the mass info of an entity",
            params = {
                    @Param(name = "entity", value = "The entity to resolve the mass info from")
            }
    )
    public MassRegistry.MassResolution resolveMassInfo(Entity entity) {
        return MassRegistry.resolveMassInfo(entity);
    }

    @Info(
            value = "Resolve a custom mass formula for any entity. Living entities and item entities get extra variables.",
            params = {
                    @Param(name = "entity", value = "The entity used to build formula variables"),
                    @Param(name = "formula", value = "The formula to evaluate")
            }
    )
    public double resolveCustomEntityFormula(Entity entity, String formula) {
        return MassRegistry.evaluateEntityFormula(entity, formula);
    }

    @Info(
            value = "Set the Sable Beyond mass NBT override on an entity",
            params = {
                    @Param(name = "entity", value = "The entity to write mass NBT on"),
                    @Param(name = "mass", value = "The mass value to store")
            }
    )
    public double setMassNbt(Entity entity, double mass) {
        return MassRegistry.setMassNbt(entity, mass);
    }

    @Info(
            value = "Clear the Sable Beyond mass NBT override from an entity",
            params = {
                    @Param(name = "entity", value = "The entity to clear mass NBT from")
            }
    )
    public void clearMassNbt(Entity entity) {
        MassRegistry.clearMassNbt(entity);
    }

    @Info(
            value = "Is entity has mass enabled",
            params = {
                    @Param(name = "entity", value = "The entity to see if the mass is enabled")
            }
    )
    public boolean isMassAppliedEntity(Entity entity) {
        return MassRegistry.isMassAppliedEntity(entity);
    }

    @Info(
            value = "Get NBT key of sable beyond mass"
    )
    public String getNbtKey() {
        return MassRegistry.getNbtKey();
    }

    @Info(
            value = "Get the configured Sable Beyond living entity mass formula"
    )
    public String getLivingEntityFormula() {
        return MassRegistry.getLivingEntityFormula();
    }

    @Info(
            value = "Get sable beyond base mass"
    )
    public double getBaseMass() {
        return MassRegistry.getBaseMass();
    }

    @Info(
            value = "Get sable beyond volume multiplier"
    )
    public double getVolumeMultiplier() {
        return MassRegistry.getVolumeMultiplier();
    }


}
