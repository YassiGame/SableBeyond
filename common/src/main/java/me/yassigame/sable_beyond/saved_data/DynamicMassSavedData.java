package me.yassigame.sable_beyond.saved_data;

import me.yassigame.sable_beyond.api.mass.DynamicMass;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DynamicMassSavedData extends SavedData {
    private static final String DATA_NAME = "sable_beyond_dynamic_mass";
    private static final Factory<DynamicMassSavedData> FACTORY = new Factory<>(
            DynamicMassSavedData::new,
            DynamicMassSavedData::load,
            null
    );

    private final ConcurrentMap<DynamicMass.BlockMassKey, Double> masses = new ConcurrentHashMap<>();

    public static @Nullable DynamicMassSavedData get(MinecraftServer server) {
        if (server == null || server.overworld() == null) {
            return null;
        }

        return getFromStorageLevel(server.overworld());
    }

    public static @Nullable DynamicMassSavedData get(ServerLevel level) {
        if (level == null) {
            return null;
        }

        final ServerLevel overworld = level.getServer().overworld();
        if (overworld != null) {
            return getFromStorageLevel(overworld);
        }

        return level.dimension().equals(Level.OVERWORLD) ? getFromStorageLevel(level) : null;
    }

    private static DynamicMassSavedData getFromStorageLevel(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void setMass(final DynamicMass.BlockMassKey key, final double mass) {
        if (!Double.isFinite(mass) || mass < 0.0) {
            return;
        }

        masses.put(key, mass);
        setDirty();
    }

    public void removeMass(final DynamicMass.BlockMassKey key) {
        masses.remove(key);
        setDirty();
    }

    public void clearMasses() {
        masses.clear();
        setDirty();
    }

    public Map<DynamicMass.BlockMassKey, Double> getMasses() {
        return Map.copyOf(masses);
    }

    private static void readMasses(ListTag list, ConcurrentMap<DynamicMass.BlockMassKey, Double> masses) {
        masses.clear();

        for (Tag rawTag : list) {
            if (!(rawTag instanceof CompoundTag tag)) {
                continue;
            }

            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimensionId == null) {
                continue;
            }

            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            long pos = tag.getLong("Pos");
            double mass = tag.getDouble("Mass");
            if (!Double.isFinite(mass) || mass < 0.0) {
                continue;
            }

            masses.put(new DynamicMass.BlockMassKey(dimension, pos), mass);
        }
    }


    private static ListTag writeMasses(ConcurrentMap<DynamicMass.BlockMassKey, Double> masses) {
        ListTag list = new ListTag();

        masses.forEach((key, mass) -> {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", key.dimension().location().toString());
            tag.putLong("Pos", key.pos());
            tag.putDouble("Mass", mass);
            list.add(tag);
        });

        return list;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("Masses", writeMasses(masses));
        return tag;
    }

    public static DynamicMassSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DynamicMassSavedData data = new DynamicMassSavedData();
        readMasses(tag.getList("Masses", Tag.TAG_COMPOUND), data.masses);
        return data;
    }

}
