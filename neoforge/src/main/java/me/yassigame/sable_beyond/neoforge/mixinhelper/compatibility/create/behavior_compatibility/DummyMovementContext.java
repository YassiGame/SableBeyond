package me.yassigame.sable_beyond.neoforge.mixinhelper.compatibility.create.behavior_compatibility;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

// inspired from the sable code
// for now is not used it will have a usage in the near future
public class DummyMovementContext extends MovementContext {

    public DummyMovementContext() {
        super(null, new StructureTemplate.StructureBlockInfo(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), null), null);
    }

    public void update(final Level level, final BlockPos pos, final BlockState state, @Nullable final CompoundTag blockEntityData) {
        this.world = level;
        this.state = state;
        this.localPos = pos;
        this.blockEntityData = blockEntityData;
        this.position = pos.getCenter();
        this.motion = Vec3.ZERO;
        this.relativeMotion = Vec3.ZERO;
        this.disabled = false;
    }

    public void update(final Level level, final BlockPos pos, final BlockState state, @Nullable final CompoundTag blockEntityData,
                       final Vec3 position, final Vec3 motion, final Vec3 relativeMotion) {
        this.world = level;
        this.state = state;
        this.localPos = pos;
        this.blockEntityData = blockEntityData;
        this.position = position;
        this.motion = motion;
        this.relativeMotion = relativeMotion;
        this.disabled = false;
    }
}
