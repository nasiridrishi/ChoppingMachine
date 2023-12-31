package net.nasiridrishi.choppingmachine.utils;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Utils {


  public static List<Vector> getLine(Vector start, Vector end, double step) {
    List<Vector> lineVectors = new ArrayList<>();

    Vector direction = end.clone().subtract(start).normalize();

    double distance = start.distance(end);
    int steps = (int) (distance / step);

    for (int i = 0; i < steps; i++) {
      double factor = i * 0.1;
      Vector interpolated = start.clone().add(direction.clone().multiply(factor));
      lineVectors.add(interpolated);
    }

    lineVectors.add(end.clone());

    return lineVectors;
  }

  public static List<Vector> circularVect(Vector center, double radius) {
    List<Vector> circleVectors = new ArrayList<>();

    for (double i = 0; i < 360; i += 10) {
      double x = center.getX() + radius * Math.cos(i);
      double z = center.getZ() + radius * Math.sin(i);
      circleVectors.add(new Vector(x, center.getY(), z));
    }

    return circleVectors;
  }

  public static List<Player> getNearbyPlayers(Location location, int radius) {
    List<Player> nearbyPlayers = new ArrayList<>();
    if (location.getWorld() == null) {
      return nearbyPlayers;
    }
    for (Player player : location.getWorld().getPlayers()) {
      if (player.getLocation().distance(location) <= radius) {
        nearbyPlayers.add(player);
      }
    }
    return nearbyPlayers;
  }

  public static int randomInt(int i, int i1) {
    return (int) (Math.random() * (i1 - i)) + i;
  }
}
