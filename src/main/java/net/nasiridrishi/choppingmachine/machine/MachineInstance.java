package net.nasiridrishi.choppingmachine.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.types.BaseMachine;
import net.nasiridrishi.choppingmachine.utils.LocationUtils;
import net.nasiridrishi.choppingmachine.utils.MachineHologram;
import net.nasiridrishi.choppingmachine.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A machine instance is a machine that has been placed in the world.
 */

public class MachineInstance {

  @Getter
  private final BaseMachine type;
  @Setter
  @Getter
  private Location location;
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

  private boolean chestFull = false;

  private int tickCounter = -1;
  private boolean searchingTree = false;
  private MachineHologram hologram;
  private String status = "§7Idle";
  @Setter
  @Getter
  private Material lastChoppedLog;

  private MachinePath foundTree;

  public MachineInstance(@NonNull BaseMachine type, @NonNull Location location,
      @NonNull UUID owner) {

    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(location, "location cannot be null");
    Objects.requireNonNull(owner, "owner cannot be null");

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
      notifyOwner("§cChest full at " + location.getBlockX() + ", " + location.getBlockY() + ", "
              + location.getBlockZ() + "in world " + location.getWorld().getName(),
          Sound.BLOCK_NOTE_BLOCK_BELL);
      chestFull = true;
      return false;
    } else {
      choppedLogs.clear();
      return true;
    }
  }

  private boolean checkChest() {
    if (chest == null) {
      return false;
    }
    if (chestFull) {
      if (chest.getInventory().firstEmpty() != -1) {
        chestFull = false;
        return true;
      }
      return false;
    }
    return true;
  }

  private boolean isInLoadedChunk() {
    return isInLoadedChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
  }

  private boolean isInLoadedChunk(int x, int y) {
    return location.getWorld().isChunkLoaded(x >> 4, y >> 4);
  }

  public void onTick() {
    //check if owner is online
    if (Bukkit.getPlayer(owner) == null) {
      return;
    }
    //check if machine is in loaded chunk
    if (!isInLoadedChunk()) {
      ChoppingMachine.getInstance().getLogger().warning("Tried to tick machine in unloaded chunk");
      lastChoppedLog = null;
      return;
    }
    if (!checkCounter() || !verify(true)) {
      lastChoppedLog = null;
      return;
    }

    if (!checkChest()) {
      updateHologram();
      return;
    }

    if (!addLogToChest()) {
      return;
    }

    //check for boosters or destructive blocks around machine
    if (checkDestructiveNearby()) {
      return;
    }

    //chop if foundLogs is not empty
    if (!foundLogs.isEmpty()) {
      this.status = "§aChopping logs..";
      chopLogs();
    } else if (foundTree == null) {
      if (!searchingTree) {
        this.status = "§eSearching for Tree..";
        lastChoppedLog = null;
        searchingTree = true;
        searchForTree();
      }
    } else {
      this.status = "§eMoving to Tree..";
      handleMoveToTree();
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

  private void chopLogs() {
    if (checkBoosters()) {
      return;
    }
    AtomicReference<Block> choppedBlock = new AtomicReference<>();
    foundLogs.stream().findFirst().ifPresent(block -> {
      if (isInLoadedChunk(block.getX() >> 4, block.getZ() >> 4)) {
        type.attemptChop(this, block);
      }
      choppedBlock.set(block);
      this.lastChoppedLog = block.getType();
    });
    if (choppedBlock.get() != null) {
      foundLogs.remove(choppedBlock.get());
    }
  }

  /**
   * Searches for a tree in a radius around the machine
   */
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
                //check if block is a log block and is above dirt block
                if (type.isWoodenLog(block.getType())
                    && block.getRelative(BlockFace.DOWN).getType() == Material.DIRT) {
                  setDestinationTree(block.getLocation());
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

  private void setDestinationTree(Location location) {
    this.foundTree = new MachinePath(MachineManager.getInstance().getPathfinder(),
        this.getLocation(),
        location, 10);
  }

  private Logger logger() {
    return ChoppingMachine.getInstance().getLogger();
  }

  private void handleMoveToTree() {
    Location origin = foundTree.getOrigin().clone();

    if (LocationUtils.xzDistance(origin.getX(), origin.getZ(), foundTree.getTargetLog().getX(),
        foundTree.getTargetLog().getZ()) <= 2) {
      this.foundLogs.addAll(findConnectedLogs(foundTree.getTargetLog()));
      foundTree = null;
      return;
    }
    if (!foundTree.shouldTryAgain()) {
      foundTree = null;
      return;
    }
    Location nextPos = foundTree.getNextPos();
    if (nextPos == null) {
      foundTree = null;
      return;
    }
    updatePos(nextPos);
  }

  private void updatePos(Location newPos) {
    //move blocks
    location.getBlock().setType(Material.AIR);
    newPos.getBlock().setType(type.getMachineBlockMaterial());
    //move chest if chest is placed
    if (chest != null) {
      Location nextChestLoc = newPos.clone().add(0, 1, 0);
      nextChestLoc.getBlock().setType(Material.CHEST);
      Chest nextChest = (Chest) nextChestLoc.getBlock().getState();
      if (!chest.getInventory().isEmpty()) {
        for (ItemStack itemStack : chest.getInventory().getContents()) {
          if (itemStack != null) {
            nextChest.getInventory().addItem(itemStack.clone());
          }
        }
      }
      chest.getInventory().clear();
      chest.getBlock().setType(Material.AIR);
      this.chest = nextChest;
    }

    //update location through machine manager
    MachineManager.getInstance().updateMachineLocation(this,
        newPos);
  }

  private Set<Block> findConnectedLogs(Location primaryLog) {
    return findConnectedLogs(primaryLog.getBlock(), null);
  }


  /**
   * Recursive method to get all connected logs to a log
   * <p>
   * exploring in all directions to find connected logs
   */
  private Set<Block> findConnectedLogs(Block primaryLog, Set<Block> foundLogs) {
    if (foundLogs == null) {
      foundLogs = new HashSet<>();
    }

    if (!foundLogs.contains(primaryLog) && type.isWoodenLog(primaryLog.getType())) {
      foundLogs.add(primaryLog);

      // Check all faces around primaryLog
      BlockFace[] faces = {
          BlockFace.UP,
          BlockFace.DOWN,
          BlockFace.NORTH,
          BlockFace.EAST,
          BlockFace.SOUTH,
          BlockFace.WEST,
          BlockFace.NORTH_EAST,
          BlockFace.NORTH_WEST,
          BlockFace.SOUTH_EAST,
          BlockFace.SOUTH_WEST};

      for (BlockFace face : faces) {
        Block block = primaryLog.getRelative(face);
        if (block.getLocation().distanceSquared(location)
            <= type.getSearchRadius() * type.getSearchRadius()) {
          findConnectedLogs(block, foundLogs);
        }
      }
    }

    return foundLogs;
  }

  private boolean checkCounter() {
    if (tickCounter == -1) {
      tickCounter = type.getTickInterval();
    }
    return tickCounter-- == 0;
  }

  public boolean verify(boolean setBlock) {
    boolean verified = location.getBlock().getType() == type.getMachineItem().getType();
    if (!verified && setBlock) {
      location.getBlock().setType(type.getMachineBlockMaterial());
      return true;
    }
    return verified;
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
        Material.POPPY, Material.DANDELION, Material.SUNFLOWER, Material.LILAC, Material.PEONY,
        Material.LARGE_FERN, Material.CHORUS_FLOWER};

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

                //check if chest is placed
                if (chest != null) {
                  //drop chest items
                  for (ItemStack itemStack : chest.getInventory().getContents()) {
                    if (itemStack != null) {
                      Objects.requireNonNull(location.getWorld())
                          .dropItemNaturally(location, itemStack);
                    }
                  }
                  //set chest to air
                  chest.getBlock().setType(Material.AIR);
                }
                //notify owner
                notifyOwner("§cMachine destroyed due to destructive block nearby at "
                        + location.getBlockX() + ", " + location.getBlockY() + ", "
                        + location.getBlockZ() + "in world " + location.getWorld().getName(),
                    Sound.BLOCK_NOTE_BLOCK_BELL);
              });
          return true;
        }
      }
    }
    return false;
  }

  //get owner if online
  private Player getOnlineOwner() {
    return Bukkit.getPlayer(owner);
  }

  private void notifyOwner(String message, Sound sound) {
    Player player = getOnlineOwner();
    if (player != null) {
      player.sendMessage(message);
      if (sound != null) {
        player.playSound(player.getLocation(), sound, 1, 1);
      }
    }
  }

}
