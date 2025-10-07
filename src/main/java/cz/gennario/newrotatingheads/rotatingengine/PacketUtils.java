package cz.gennario.newrotatingheads.rotatingengine;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.google.common.collect.Lists;
import cz.gennario.newrotatingheads.utils.Utils;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PacketUtils {

    private static final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    public static void sendPacket(Player player, PacketContainer packet) {
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int generateRandomEntityId() {
        return Integer.parseInt(RandomStringUtils.random(8, false, true));
    }

    public static PacketContainer spawnEntityPacket(EntityType entityType, Location location, int entityId) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        // Entity ID
        packet.getIntegers().write(0, entityId);

        // Entity Type
        try {
            packet.getEntityTypeModifier().write(0, entityType);
        } catch (Exception e) {
            // Entity Type
            if (entityType.equals(EntityType.ARMOR_STAND)) {
                packet.getIntegers().write(6, 78);
            } else {
                packet.getIntegers().write(6, (int) entityType.getTypeId());
            }
            // Set optional velocity (/8000)
            packet.getIntegers().write(1, 0);
            packet.getIntegers().write(2, 0);
            packet.getIntegers().write(3, 0);
            // Set yaw pitch
            packet.getIntegers().write(4, 0);
            packet.getIntegers().write(5, 0);
            // Set object data
            packet.getIntegers().write(7, 0);
        }

        // Set location
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());

        try {
            packet.getBytes().write(0, (byte) location.getPitch());
            packet.getBytes().write(1, (byte) location.getYaw());
        } catch (Exception e) {
        }

        // Set UUID
        packet.getUUIDs().write(0, UUID.randomUUID());

        return packet;
    }

    public static WrappedDataWatcher getDataWatcher() {
        return new WrappedDataWatcher();
    }

    public static PacketContainer applyMetadata(int entityId, WrappedDataWatcher watcher) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entityId);

        try {
            final List<WrappedDataValue> wrappedDataValueList = Lists.newArrayList();
            watcher.getWatchableObjects().stream().filter(Objects::nonNull).forEach(entry -> {
                final WrappedDataWatcher.WrappedDataWatcherObject dataWatcherObject = entry.getWatcherObject();
                wrappedDataValueList.add(
                    new WrappedDataValue(dataWatcherObject.getIndex(), dataWatcherObject.getSerializer(), entry.getRawValue()));
            });
            packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);
        } catch (Exception e) {
            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
        }
        return packet;
    }

    public static WrappedDataWatcher setMetadata(WrappedDataWatcher watcher, int index, Class c, Object value) {
        watcher.setObject(
            new WrappedDataWatcher.WrappedDataWatcherObject(index, com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry.get(c)),
            value);

        return watcher;
    }

    public static WrappedDataWatcher setMetadata(WrappedDataWatcher watcher, int index, WrappedDataWatcher.Serializer serializer,
                                                 Object value) {
        watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(index, serializer), value);
        return watcher;
    }

    public static PacketContainer getEquipmentPacket(int entityId, Pair<EnumWrappers.ItemSlot, ItemStack>... items) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);

        packet.getIntegers().write(0, entityId);
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = Arrays.asList(items);
        packet.getSlotStackPairLists().write(0, list);
        return packet;
    }

    public static PacketContainer teleportEntityPacket(int entityID, Location location) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);

        packet.getIntegers().write(0, entityID);

        // Set location
        if (Utils.MINECRAFT_VERSION_21_2.atOrAbove()) {
            InternalStructure structure = packet.getStructures().getValues().get(0);

            StructureModifier<Vector> vectors = structure.getVectors();
            vectors.write(0, new Vector(location.getX(), location.getY(), location.getZ()));
            vectors.write(1, new Vector(0, 0, 0));

            structure.getFloat().write(0, location.getYaw());
        } else {
            packet.getDoubles().write(0, location.getX());
            packet.getDoubles().write(1, location.getY());
            packet.getDoubles().write(2, location.getZ());
        }

        packet.getBooleans().write(0, false);

        return packet;
    }

    public static PacketContainer destroyEntityPacket(int entityID) {
        List<Integer> entityIDList = new ArrayList<>();
        entityIDList.add(entityID);
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getModifier().writeDefaults();
        try {
            packet.getIntLists().write(0, entityIDList);
        } catch (Exception e) {
            packet.getIntegerArrays().write(0, entityIDList.stream().mapToInt(i -> i).toArray());
        }

        return packet;
    }

    public static PacketContainer getHeadRotatePacket(int entityId, Location location) {
        PacketContainer pc = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        pc.getModifier().writeDefaults();
        pc.getIntegers().write(0, entityId);
        pc.getBytes().write(0, (byte) getCompressedAngle(location.getYaw()));

        return pc;
    }

    public static PacketContainer getHeadLookPacket(int entityId, Location location) {
        PacketContainer pc = protocolManager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
        pc.getModifier().writeDefaults();
        pc.getIntegers().write(0, entityId);
        pc.getBytes().write(0, (byte) location.getYaw());
        pc.getBooleans().write(0, false);

        return pc;
    }

    public static PacketContainer getPassengerPacket(int vehicleId, int passengerCount, int... passengers) {
        PacketContainer pc = protocolManager.createPacket(PacketType.Play.Server.MOUNT);

        pc.getIntegers().write(0, vehicleId);
        pc.getIntegerArrays().write(0, passengers);

        return pc;
    }

    private static int getCompressedAngle(float value) {
        return (int) (value * 256.0F / 360.0F);
    }
}
