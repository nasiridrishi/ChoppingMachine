package net.nasiridrishi.choppingmachine.utils;

import java.util.Objects;
import net.nasiridrishi.choppingmachine.machine.MachineInstance;
import net.nasiridrishi.choppingmachine.storage.MachineData;
import org.bukkit.Location;
import org.bukkit.World;

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
}
