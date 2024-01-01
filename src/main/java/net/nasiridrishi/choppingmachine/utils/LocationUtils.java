package net.nasiridrishi.choppingmachine.utils;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class LocationUtils {

  public static String toString(org.bukkit.Location location) {
    return Objects.requireNonNull(location.getWorld()).getName() + "," + location.getBlockX() + ","
        + location.getBlockY()
        + "," + location.getBlockZ();
  }


  public static boolean equals(Location location1, Location location2) {
    return Objects.equals(location1.getWorld(), location2.getWorld())
        && location1.getBlockX() == location2.getBlockX()
        && location1.getBlockY() == location2.getBlockY()
        && location1.getBlockZ() == location2.getBlockZ();
  }

  /**
   * finds the distance between two locations in the xz plane
   */
  public static double xzDistance(double x1, double z1, double x2, double z2) {
    return Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((z2 - z1), 2));
  }

  public static double xzDistance(Location loc1, Location loc2) {
    return xzDistance(loc1.getX(), loc1.getZ(), loc2.getX(), loc2.getZ());
  }


  public static Block findBlockAround(Location block, Material type, int radius) {
    return findBlockAround(block.getBlock(), type, radius);
  }

  /**
   * finds a specific block type around a block in a given radius
   */
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

}
