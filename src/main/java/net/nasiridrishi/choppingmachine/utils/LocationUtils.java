package net.nasiridrishi.choppingmachine.utils;

import java.util.List;
import java.util.Objects;
import net.nasiridrishi.choppingmachine.machine.MachineInstance;
import net.nasiridrishi.choppingmachine.storage.MachineData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class LocationUtils {

  public static String toString(org.bukkit.Location location) {
    return Objects.requireNonNull(location.getWorld()).getName() + "," + location.getBlockX() + ","
        + location.getBlockY()
        + "," + location.getBlockZ();
  }

  public static Location fromString(String string) {
    String[] parts = string.split(",");
    World world = org.bukkit.Bukkit.getWorld(parts[0]);
    if (world == null) {
      throw new IllegalArgumentException("World " + parts[0] + " does not exist");
    }
    return new org.bukkit.Location(world, Integer.parseInt(parts[1]),
        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
  }

  public static boolean equals(MachineInstance machine1, MachineInstance machine2) {
    Location location1 = machine1.getLocation();
    Location location2 = machine2.getLocation();
    return location1.getWorld().equals(location2.getWorld())
        && location1.getBlockX() == location2.getBlockX()
        && location1.getBlockY() == location2.getBlockY()
        && location1.getBlockZ() == location2.getBlockZ();
  }

  public static boolean equals(MachineInstance machine1, MachineData machine2) {
    Location location1 = machine1.getLocation();
    Location location2;
    try {
      location2 = LocationUtils.fromString(machine2.getLocation());
    } catch (Exception e) {
      return false;
    }
    return Objects.equals(location1.getWorld(), location2.getWorld())
        && location1.getBlockX() == location2.getBlockX()
        && location1.getBlockY() == location2.getBlockY()
        && location1.getBlockZ() == location2.getBlockZ();
  }

  public static boolean equals(Location location1, Location location2) {
    return Objects.equals(location1.getWorld(), location2.getWorld())
        && location1.getBlockX() == location2.getBlockX()
        && location1.getBlockY() == location2.getBlockY()
        && location1.getBlockZ() == location2.getBlockZ();
  }

  public static double xzDistance(double x1, double y1, double x2, double y2) {
    return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
  }

  public static Block findBlockAround(Location block, Material type, int radius) {
    return findBlockAround(block.getBlock(), type, radius);
  }

  public static Block findBlockAround(Block block, Material type, int radius) {
    for (int i = 0; i < radius; i++) {
      for (int j = 0; j < radius; j++) {
        for (int k = 0; k < radius; k++) {
          Block b = block.getRelative(i, j, k);
          if (b.getType() == type) {
            return b;
          }
        }
      }
    }
    return null;
  }

  public static Block findBlockAround(Location block, Material type, List<BlockFace> faces,
      int radius) {
    for (int i = 0; i < radius; i++) {
      for (int j = 0; j < radius; j++) {
        for (int k = 0; k < radius; k++) {
          Block b = block.getBlock().getRelative(i, j, k);
          if (b.getType() == type) {
            return b;
          }
        }
      }
    }
    return null;
  }

  public static Block findBlockAround(Block block, Material type, List<BlockFace> faces) {
    for (BlockFace face : faces) {
      Block b = block.getRelative(face);
      if (b.getType() == type) {
        return b;
      }
    }
    return null;
  }

  public static Block findBlockAround(Block block, Material type) {
    List<BlockFace> faces = List.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH,
        BlockFace.EAST, BlockFace.WEST);
    return findBlockAround(block, type, faces);
  }

  public static Block findBlockAround(Block block, Material... type) {
    for (Material material : type) {
      Block b = findBlockAround(block, material);
      if (b != null) {
        return b;
      }
    }
    return null;
  }
}
