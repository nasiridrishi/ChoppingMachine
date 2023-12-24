package net.nasiridrishi.choppingmachine.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.Getter;
import lombok.NonNull;
import net.nasiridrishi.choppingmachine.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

  private final List<Location> path = new ArrayList<>();
  @Getter
  private final Location origin;
  @Getter
  private final Location targetLog;
  @Getter
  private PathState pathStatus = null;
  private int maxTries;

  public MachinePath(@NonNull Pathfinder pathfinder, @NonNull Location from,
      @NonNull Location treeLog, int maxTries) {

    this.origin = from.clone();
    this.targetLog = treeLog;
    Block target = getAirBlock(treeLog);
    if (target == null) {
      this.pathStatus = PathState.FAILED;
      return;
    }
    this.maxTries = maxTries;

    PathPosition start = BukkitMapper.toPathPosition(from);
    PathPosition end = BukkitMapper.toPathPosition(target.getLocation());
    CompletionStage<PathfinderResult> pathResult = pathfinder.findPath(start, end,
        new DirectPathfinderStrategy());
    pathResult.thenAccept((result) -> {
      pathStatus = result.getPathState();
      path.clear();
      if (result.successful() || result.hasFallenBack()) {
        this.pathStatus = PathState.FOUND;
        result.getPath().getPositions()
            .forEach(pathPosition -> path.add(BukkitMapper.toLocation(pathPosition)));
      }
    });
  }

  public boolean shouldTryAgain() {
    if (maxTries == 0) {
      return false;
    }
    maxTries--;
    return true;
  }

  public Location getNextPos() {
    if (this.pathStatus != PathState.FOUND || path.isEmpty()) {
      return null;
    }
    Location next = path.get(0);
    path.remove(0);
    //Path returns origin as first position, so we need to skip it
    if (LocationUtils.equals(next, origin)) {
      return getNextPos();
    }
    return getHighestNonAirBlock(next, 20);
  }

  private Location getHighestNonAirBlock(Location location, int maxDepth) {
    Block blockBelow = location.getBlock().getRelative(BlockFace.DOWN);
    if (blockBelow.getType() == Material.AIR) {
      if (maxDepth == 0) {
        return null;
      }
      maxDepth--;
      return getHighestNonAirBlock(blockBelow.getLocation(), maxDepth);
    }
    return blockBelow.getLocation().add(0, 1, 0).clone();
  }

  private Block getAirBlock(Location at) {
    return LocationUtils.findBlockAround(at, Material.AIR, 3);
  }


}

