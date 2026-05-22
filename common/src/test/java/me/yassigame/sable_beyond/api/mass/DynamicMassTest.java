package me.yassigame.sable_beyond.api.mass;

import me.yassigame.sable_beyond.config.SableBeyondConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicMassTest {
    private double previousBucketMass;

    @BeforeEach
    void setUp() {
        previousBucketMass = SableBeyondConfig.dynamicMass().mass_of_bucket;
        DynamicMass.clearAllBlockMasses();
    }

    @AfterEach
    void tearDown() {
        SableBeyondConfig.dynamicMass().mass_of_bucket = previousBucketMass;
        DynamicMass.clearAllBlockMasses();
    }

    @Test
    void convertsLiquidAmountToMassFromConfiguredBucketMass() {
        SableBeyondConfig.dynamicMass().mass_of_bucket = 2.5;

        assertEquals(2.5, DynamicMass.liquidToMass(1000.0));
        assertEquals(0.625, DynamicMass.liquidToMass(250.0));
        assertEquals(0.0, DynamicMass.liquidToMass(0.0));
    }

    @Test
    void storesAndReadsCachedBlockMassByDimensionAndPosition() {
        final DynamicMass.BlockMassKey key = key(Level.OVERWORLD, new BlockPos(4, 64, -8));

        DynamicMass.putBlockMass(key, 12.75);

        final OptionalDouble mass = DynamicMass.getBlockMass(key);
        assertTrue(mass.isPresent());
        assertEquals(12.75, mass.orElseThrow());
        assertTrue(DynamicMass.hasBlockMass(key));
    }

    @Test
    void keepsMassOverridesSeparatedByDimension() {
        final BlockPos pos = new BlockPos(1, 70, 1);
        final DynamicMass.BlockMassKey overworldKey = key(Level.OVERWORLD, pos);
        final DynamicMass.BlockMassKey netherKey = key(Level.NETHER, pos);

        DynamicMass.putBlockMass(overworldKey, 3.0);
        DynamicMass.putBlockMass(netherKey, 9.0);

        assertEquals(3.0, DynamicMass.getBlockMass(overworldKey).orElseThrow());
        assertEquals(9.0, DynamicMass.getBlockMass(netherKey).orElseThrow());
    }

    @Test
    void discardRemovesCachedMassWithoutTouchingOtherBlocks() {
        final DynamicMass.BlockMassKey removedKey = key(Level.OVERWORLD, new BlockPos(2, 65, 2));
        final DynamicMass.BlockMassKey keptKey = key(Level.OVERWORLD, new BlockPos(3, 65, 2));

        DynamicMass.putBlockMass(removedKey, 4.0);
        DynamicMass.putBlockMass(keptKey, 8.0);

        assertTrue(DynamicMass.discardBlockMass(removedKey));
        assertFalse(DynamicMass.hasBlockMass(removedKey));
        assertEquals(8.0, DynamicMass.getBlockMass(keptKey).orElseThrow());
    }

    @Test
    void discardMissingMassReturnsFalse() {
        assertFalse(DynamicMass.discardBlockMass(key(Level.OVERWORLD, BlockPos.ZERO)));
    }

    @Test
    void clearAllBlockMassesRemovesEveryCachedOverride() {
        DynamicMass.putBlockMass(key(Level.OVERWORLD, new BlockPos(1, 64, 1)), 1.0);
        DynamicMass.putBlockMass(key(Level.NETHER, new BlockPos(2, 64, 2)), 2.0);

        DynamicMass.clearAllBlockMasses();

        assertTrue(DynamicMass.getBlockMasses().isEmpty());
    }

    @Test
    void rejectsInvalidMassOverridesBeforeCaching() {
        final DynamicMass.BlockMassKey key = key(Level.OVERWORLD, BlockPos.ZERO);

        assertThrows(IllegalArgumentException.class, () -> DynamicMass.putBlockMass(key, -1.0));
        assertThrows(IllegalArgumentException.class, () -> DynamicMass.putBlockMass(key, Double.NaN));
        assertFalse(DynamicMass.hasBlockMass(key));
    }

    private static DynamicMass.BlockMassKey key(final ResourceKey<Level> dimension, final BlockPos pos) {
        return new DynamicMass.BlockMassKey(dimension, pos.asLong());
    }
}
