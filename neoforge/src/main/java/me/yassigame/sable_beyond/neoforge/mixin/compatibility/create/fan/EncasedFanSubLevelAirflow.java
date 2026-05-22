package me.yassigame.sable_beyond.neoforge.mixin.compatibility.create.fan;

import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.fan_airflow.EncasedFanSubLevelAirflowHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(EncasedFanBlockEntity.class)
public abstract class EncasedFanSubLevelAirflow {
    @Inject(method = "tick", at = @At("TAIL"))
    private void sableBeyond$registerForSubLevelAirflow(final CallbackInfo ci) {
        EncasedFanSubLevelAirflowHelper.registerTickingFan((EncasedFanBlockEntity) (Object) this);
    }
}
