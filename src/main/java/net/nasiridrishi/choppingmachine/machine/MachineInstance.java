package net.nasiridrishi.choppingmachine.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.types.BaseMachine;
import net.nasiridrishi.choppingmachine.utils.MachineHologram;
import net.nasiridrishi.choppingmachine.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

/**
 * A machine instance is a machine that has been placed in the world.
 */

public class MachineInstance {

  @Getter
  private final BaseMachine type;
  @Getter
  private final Location location;
  @Getter
  private final UUID owner;
  /**
   * List of logs found while searching for logs
   */
  private final ArrayList<Block> foundLogs = new ArrayList<>();
  //List of chopped logs to be added to chest
  @Getter
  private final List<ItemStack> choppedLogs = new ArrayList<>();
  @Getter
  private Chest chest;
  private int tickCounter = -1;
  private boolean searchingTree = false;
  private MachineHologram hologram;
  private String status = "§7Idle";
  @Setter
  @Getter
  private Material lastChoppedLog;

  public MachineInstance(@NonNull BaseMachine type, @NonNull Location location,
      @NonNull UUID owner) {
    this.type = type;
    this.location = location;
    this.owner = owner;

    //check if chest is already placed
    Block block = location.clone().add(0, 1, 0).getBlock();
    if (block.getState() instanceof Chest) {
      this.chest = (Chest) block.getState();
    }
  }

  public void destroy() {
    if (hologram != null) {
      hologram.destroy();
      hologram = null;
    }
    ChoppingMachine.getInstance().getLogger()
        .info("Destroying machine with uid: " + getUid());
  }


  public void onChestPlaced(@NonNull Chest chest) {
    this.chest = chest;
    updateHologram();
  }

  public void onChestRemoved() {
    this.chest = null;
    updateHologram();
  }

  /**
   * @return false if could not add all the items to chest. meaning chest is full
   */
  private boolean addLogToChest() {
    if (choppedLogs.isEmpty()) {
      return true;
    }
    HashMap<Integer, ItemStack> left = chest.getInventory()
        .addItem(choppedLogs.toArray(new ItemStack[0]));
    if (!left.isEmpty()) {
      choppedLogs.clear();
      choppedLogs.addAll(left.values());
      this.status = "§cChest full";
      return false;
    } else {
      choppedLogs.clear();
      return true;
    }
  }

  private boolean isInLoadedChunk() {
    return isInLoadedChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
  }

  private boolean isInLoadedChunk(int x, int y) {
    return location.getWorld().isChunkLoaded(x >> 4, y >> 4);
  }

  public void onTick() {
    if (!isInLoadedChunk()) {
      ChoppingMachine.getInstance().getLogger().warning("Tried to tick machine in unloaded chunk");
      lastChoppedLog = null;
      return;
    }
    if (!checkCounter() || !verify()) {
      lastChoppedLog = null;
      return;
    }

    //check for destructive blocks around machine
    if (checkDestructiveNearby()) {
      return;
    }

    if (chest == null || !addLogToChest()) {
      updateHologram();
      return;
    }

    //chop if foundLogs is not empty
    if (!foundLogs.isEmpty()) {
      if (checkBoosters()) {
        return;
      }
      this.status = "§aChopping logs..";
      AtomicReference<Block> choppedBlock = new AtomicReference<>();
      foundLogs.stream().findFirst().ifPresent(block -> {
        if (isInLoadedChunk(block.getX() >> 4, block.getZ() >> 4)) {
          type.attemptChop(this, block);
        }
        //marking as chopped even if actually not chopped to remove it from list anyway.
        choppedBlock.set(block);
        this.lastChoppedLog = block.getType();
      });
      if (choppedBlock.get() != null) {
        foundLogs.remove(choppedBlock.get());
      }
    } else if (!searchingTree) {
      this.status = "§eSearching for Tree..";
      lastChoppedLog = null;
      searchingTree = true;
      searchForTree();
    }
    updateHologram();
  }

  public void updateHologram() {
    if (hologram != null) {
      hologram.updateHologramLines();
    } else if (ChoppingMachine.getInstance().isHologramsEnabled()) {
      hologram = new MachineHologram(this);
    }
  }

  /**
   * Searches for a tree in a radius around the machine
   */
//  private void searchForTree() {
//    Bukkit.getScheduler().runTaskAsynchronously(ChoppingMachine.getInstance(), () -> {
//      int radius = type.getSearchRadius();
//      for (int x = -radius; x <= radius; x++) { //x axis
//        for (int y = -radius; y <= radius; y++) { //y axis
//          for (int z = -radius; z <= radius; z++) { //z axis
//            Location location = this.location.clone();
//            Block block = location.add(x, y, z).getBlock();
//            if (!isInLoadedChunk(block.getX() >> 4, block.getZ() >> 4)) {
//              continue;
//            }
//            if (type.isWoodenLog(block.getType())) {
//              foundLogs.addAll(getConnectedLogs(block, null));
//              searchingTree = false;
//              return;
//            }
//          }
//        }
//      }
//      searchingTree = false;
//    });
//  }
  private void searchForTree() {
    Bukkit.getScheduler().runTaskAsynchronously(ChoppingMachine.getInstance(), () -> {
      int radius = type.getSearchRadius();
      Location centerLocation = this.location.clone();

      // Search in a spiral pattern from the center outwards
      for (int r = 0; r <= radius; r++) {
        for (int x = -r; x <= r; x++) {
          for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
              // Check only blocks on the edges of the current "ring"
              if (Math.abs(x) == r || Math.abs(y) == r || Math.abs(z) == r) {
                Location currentLocation = centerLocation.clone().add(x, y, z);
                Block block = currentLocation.getBlock();
                if (!isInLoadedChunk(block.getX() >> 4, block.getZ() >> 4)) {
                  continue;
                }
                if (type.isWoodenLog(block.getType())) {
                  foundLogs.addAll(getConnectedLogs(block, null));
                  searchingTree = false;
                  return;
                }
              }
            }
          }
        }
      }
      searchingTree = false;
    });
  }


  /**
   * Recursive method to get all connected logs to a log
   * <p>
   * exploring in all directions to find connected logs
   */
  private Set<Block> getConnectedLogs(Block primaryLog, Set<Block> foundLogs) {
    if (foundLogs == null) {
      foundLogs = new HashSet<>();
    }

    if (!foundLogs.contains(primaryLog) && type.isWoodenLog(primaryLog.getType())) {
      foundLogs.add(primaryLog);

      // Directions to explore: up, down, left, right, up-left, up-right, down-left, down-right
      int[][] directions = {{0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {1, 1, 0}, {-1, 1, 0},
          {1, -1, 0}, {-1, -1, 0}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};

      // Explore in each direction
      for (int[] direction : directions) {
        int xOffset = direction[0];
        int yOffset = direction[1];
        int zOffset = direction[2];

        Block block = primaryLog.getRelative(xOffset, yOffset, zOffset);
        if (block.getLocation().distanceSquared(location)
            <= type.getSearchRadius() * type.getSearchRadius()) {
          getConnectedLogs(block, foundLogs);
        }
      }
    }

    return foundLogs;
  }

//  /**
//   * Recursive method to get all connected logs to a log
//   *
//   * @param direction: 1 = up, -1 = down, 2 = left, -2 = right, 0 = none
//   */
//  private ArrayList<Block> getConnectedLogs(Block primaryLog, ArrayList<Block> foundLogs,
//      int direction) {
//    if (foundLogs == null) {
//      foundLogs = new ArrayList<>();
//      foundLogs.add(primaryLog);
//      getConnectedLogs(primaryLog, foundLogs, 1);
//      getConnectedLogs(primaryLog, foundLogs, -1);
//      getConnectedLogs(primaryLog, foundLogs, 2);
//      getConnectedLogs(primaryLog, foundLogs, -2);
//    } else if (direction == 1) {
//      Block block = primaryLog.getRelative(0, 1, 0);
//      if (type.isWoodenLog(block.getType())
//          && block.getLocation().distance(location) <= type.getSearchRadius()) {
//        foundLogs.add(block);
//        getConnectedLogs(block, foundLogs, 1);
//      }
//    } else if (direction == -1) {
//      Block block = primaryLog.getRelative(0, -1, 0);
//      if (type.isWoodenLog(block.getType())
//          && block.getLocation().distance(location) <= type.getSearchRadius()) {
//        foundLogs.add(block);
//        getConnectedLogs(block, foundLogs, -1);
//      }
//    } else if (direction == 2) {
//      Block block = primaryLog.getRelative(1, 0, 0);
//      if (type.isWoodenLog(block.getType())
//          && block.getLocation().distance(location) <= type.getSearchRadius()) {
//        foundLogs.add(block);
//        getConnectedLogs(block, foundLogs, 2);
//      }
//    } else if (direction == -2) {
//      Block block = primaryLog.getRelative(-1, 0, 0);
//      if (type.isWoodenLog(block.getType())
//          && block.getLocation().distance(location) <= type.getSearchRadius()) {
//        foundLogs.add(block);
//        getConnectedLogs(block, foundLogs, -2);
//      }
//
//    }
//    return foundLogs;
//  }

  private boolean checkCounter() {
    if (tickCounter == -1) {
      tickCounter = type.getTickInterval();
    }
    return tickCounter-- == 0;
  }

  public boolean verify() {
    return location.getBlock().getType() == type.getMachineItem().getType();
  }

  /**
   * Instance id unique to location and machine type
   */
  public String getUid() {
    return Utils.formulateMachineUid(this);
  }

  public Location getBlockLc() {
    return new Location(location.getWorld(), location.getBlockX(), location.getBlockY() + 1.5,
        location.getBlockZ());
  }

  public String getStatus() {
    if (chest == null) {
      return "§cChest unlinked";
    }
    return status;
  }

  private boolean checkBoosters() {
    Material[] boosters = {
        Material.SPONGE,
        Material.WET_SPONGE,
        Material.ICE,
        Material.PACKED_ICE,
        Material.BLUE_ICE,
        Material.FROSTED_ICE,
    };
    BlockFace[] directions = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST,
        BlockFace.SOUTH, BlockFace.WEST};
    for (BlockFace direction : directions) {
      Block block = location.getBlock().getRelative(direction);
      for (Material booster : boosters) {
        if (block.getType() == booster) {
          block.setType(Material.AIR);
          foundLogs.forEach(log -> {
            this.type.attemptChop(this, log);
          });
          foundLogs.clear();
          return true;
        }
      }
    }
    return false;
  }

  private boolean checkDestructiveNearby() {
    Material[] destructiveBlocks = {Material.FERN, Material.RED_MUSHROOM,
        Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM, Material.BROWN_MUSHROOM_BLOCK,
        Material.POPPY, Material.DANDELION, Material.DEAD_BUSH, Material.GRASS, Material.TALL_GRASS,
        Material.LARGE_FERN, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
        Material.TALL_GRASS, Material.LARGE_FERN, Material.VINE, Material.SWEET_BERRY_BUSH,
        Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
        Material.MELON_STEM, Material.PUMPKIN_STEM, Material.COCOA, Material.NETHER_WART,
        Material.BAMBOO, Material.SUGAR_CANE, Material.CACTUS, Material.CHORUS_PLANT,
        Material.CHORUS_FLOWER, Material.KELP_PLANT, Material.KELP, Material.SEAGRASS,
        Material.TALL_SEAGRASS, Material.TWISTING_VINES_PLANT, Material.WEEPING_VINES_PLANT,
        Material.GLOW_LICHEN, Material.AZALEA, Material.FLOWERING_AZALEA};

    //faces
    BlockFace[] directions = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST,
        BlockFace.SOUTH, BlockFace.WEST};

    for (BlockFace direction : directions) {
      Block block = location.getBlock().getRelative(direction);
      for (Material destructiveBlock : destructiveBlocks) {
        if (block.getType() == destructiveBlock) {
          ChoppingMachine.getInstance().getServer().getScheduler()
              .runTask(ChoppingMachine.getInstance(), () -> {
                Objects.requireNonNull(location.getWorld())
                    .dropItemNaturally(location, type.getMachineItem());
                //destroy machine
                MachineManager.getInstance().destroyMachine(this);
                //smoke
                ChoppingMachine.getInstance().getParticleApi().LIST_1_8.SMOKE_LARGE
                    .packetMotion(true, location.clone().add(0.5, 0.5, 0.5).toVector(), 0D, 0.1D,
                        0D)
                    .sendTo(Utils.getNearbyPlayers(location, getType().getSearchRadius()));
                //sound
                Objects.requireNonNull(location.getWorld())
                    .playSound(location, Sound.ENTITY_ITEM_BREAK, 1, 1);
                //set air
                location.getBlock().setType(Material.AIR);
              });
          return true;
        }
      }
    }
    return false;
  }
}
