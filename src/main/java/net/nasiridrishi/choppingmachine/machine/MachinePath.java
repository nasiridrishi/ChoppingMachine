package net.nasiridrishi.choppingmachine.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.Getter;
import lombok.NonNull;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import org.bukkit.Location;
import org.patheloper.api.pathing.Pathfinder;
import org.patheloper.api.pathing.result.PathState;
import org.patheloper.api.pathing.result.PathfinderResult;
import org.patheloper.api.pathing.strategy.strategies.DirectPathfinderStrategy;
import org.patheloper.api.wrapper.PathPosition;
import org.patheloper.mapping.bukkit.BukkitMapper;

/**
 * Represents a path that a machine will take to get to target tree.
 */
public class MachinePath {

  @Getter
  private PathState pathStatus = null;

  private final List<Location> path = new ArrayList<>();

  public MachinePath(@NonNull Pathfinder pathfinder, @NonNull Location from,
      @NonNull Location target) {

    ChoppingMachine.getInstance().getLogger().info(
        "Finding path from " + from + " to " + target + " with distance " + from.distance(target));
    PathPosition start = BukkitMapper.toPathPosition(from);
    PathPosition end = BukkitMapper.toPathPosition(target);
    CompletionStage<PathfinderResult> pathResult = pathfinder.findPath(start, end,
        new DirectPathfinderStrategy());
    pathResult.thenAcceptAsync((result) -> {
      pathStatus = result.getPathState();
      path.clear();
      if (result.successful() || result.hasFallenBack()) {
        result.getPath().getPositions().forEach(pathPosition -> {
          path.add(BukkitMapper.toLocation(pathPosition));
        });
      }
    });
  }

  public Location getNextPos() {
    if (path.isEmpty()) {
      return null;
    }
    Location next = path.get(0);
    path.remove(0);
    return next;

  }
}

