package net.nasiridrishi.choppingmachine.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.types.BaseMachineType;
import net.nasiridrishi.choppingmachine.utils.LocationUtils;
import net.nasiridrishi.choppingmachine.utils.MachineHologram;
import net.nasiridrishi.choppingmachine.utils.MachineStatus;
import net.nasiridrishi.choppingmachine.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/**
 * A machine instance is a machine that has been placed in the world.
 */
public class MachineInstance extends Location {

  private static final List<Location> foundTress = new ArrayList<>();
  private static int runTimeId = 0;
  @Getter
  private final String worldName;
  @Getter
  private final BaseMachineType type;
  @Getter
  private final UUID owner;
  /**
   * List of logs found while searching for logs
   */
  private final ArrayList<Block> currentTreeLogs = new ArrayList<>();
  //List of chopped logs to be added to chest
  @Getter
  private final List<ItemStack> choppedLogs = new ArrayList<>();
  /**
   * -- GETTER -- Instance id unique to location and machine type
   */
  @Getter
  private final int id = runTimeId++;
  @Getter
  private Chest chest;
  private boolean chestFull = false;
  private int tickCounter = -1;
  private boolean searchingTree = false;
  private MachineHologram hologram;
  private MachineStatus status = MachineStatus.IDLE;
  @Setter
  @Getter
  private Material lastChoppedLog;
  private MachinePath foundTree;
  private BukkitTask searchTreeTask;

  public MachineInstance(@NonNull BaseMachineType type, @NonNull UUID owner, @NonNull int x,
      @NonNull int y, @NonNull int z, @NonNull String world) {
    this(type, owner, x, y, z, Objects.requireNonNull(Bukkit.getWorld(world)));
  }

  public MachineInstance(@NonNull BaseMachineType type,
      @NonNull UUID owner, @NonNull int x, @NonNull int y, @NonNull int z, @NonNull World world) {
    super(world, x, y, z);

    this.worldName = world.getName();

    Objects.requireNonNull(type, "type cannot be null");
    Objects.requireNonNull(owner, "owner cannot be null");

    this.type = type;
    this.owner = owner;

    //check if chest is already placed
    Block block = getBlock().getRelative(BlockFace.UP);
    if (block.getState() instanceof Chest) {
      this.chest = (Chest) block.getState();
    }
  }

  private static void removeFoundTree(Location location) {
    for (Location foundTree : foundTress) {
      if (LocationUtils.equals(foundTree, location)) {
        foundTress.remove(foundTree);
        return;
      }
    }
  }

  //So that no two instances chop the same tree
  private static boolean isTreeFound(Location location) {
    for (Location foundTree : foundTress) {
      if (LocationUtils.equals(foundTree, location)) {
        return true;
      }
      BlockFace[] faces = {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
      for (BlockFace face : faces) {
        if (LocationUtils.equals(foundTree, location.getBlock().getRelative(face).getLocation())) {
          return true;
        }
      }
    }
    return false;
  }

  public static MachineInstance fromJson(JsonObject jsonObject) {
    BaseMachineType type = MachineManager.getInstance()
        .getMachineType(jsonObject.getString("type"));
    UUID owner = UUID.fromString(jsonObject.getString("owner"));
    return new MachineInstance(type, owner, jsonObject.getInt("location_x"),
        jsonObject.getInt("location_y"), jsonObject.getInt("location_z"),
        jsonObject.getString("world"));
  }

  public boolean equals(Location location) {
    return Objects.equals(location.getWorld(), this.getWorld())
        && location.getBlockX() == this.getBlockX()
        && location.getBlockY() == this.getBlockY()
        && location.getBlockZ() == this.getBlockZ();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MachineInstance)) {
      throw new IllegalArgumentException(
          "Tried to compare MachineInstance with non MachineInstance: "
              + obj.getClass().getName());
    }
    MachineInstance objInst = (MachineInstance) obj;
    return (objInst.getWorld() != null && this.getWorld() != null)
        && objInst.getWorld().equals(this.getWorld())
        && objInst.getBlockX() == this.getBlockX()
        && objInst.getBlockY() == this.getBlockY()
        && objInst.getBlockZ() == this.getBlockZ();
  }

  public void destroy() {
    if (hologram != null) {
      hologram.destroy();
      hologram = null;
    }
    ChoppingMachine.getInstance().getLogger()
        .info("Destroying machine with uid: " + getId());
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
      this.status = MachineStatus.CHEST_FULL;
      notifyOwner("§cChest full at " + getBlockX() + ", " + getBlockY() + ", "
              + getBlockZ() + "in world " + getWorld().getName(),
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

  private boolean isInLoadedChunk(int x, int y) {
    return getWorld().isChunkLoaded(x >> 4, y >> 4);
  }

  public void onTick() {
    //check if owner is online
    if (Bukkit.getPlayer(owner) == null) {
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

    //check for boosters or destructive blocks around the machine
    if (checkDestructiveNearby()) {
      return;
    }

    //chop if current tree logs is not empty
    if (!currentTreeLogs.isEmpty()) {
      this.status = MachineStatus.CHOPPING;
      chopLogs();
    } else if (foundTree == null) {
      searchForTree();
    } else {
      this.status = MachineStatus.MOVING_TO_TREE;
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
    currentTreeLogs.stream().findFirst().ifPresent(block -> {
      if (isInLoadedChunk(block.getX() >> 4, block.getZ() >> 4)) {
        type.attemptChop(this, block);
      }
      choppedBlock.set(block);
      this.lastChoppedLog = block.getType();
    });
    if (choppedBlock.get() != null) {
      currentTreeLogs.remove(choppedBlock.get());
    }
  }

  /**
   * Searches for a tree in a radius around the machine
   */
  private void searchForTree() {
    if (searchingTree) {
      return;
    }
    this.status = MachineStatus.SEARCHING_TREE;
    lastChoppedLog = null;
    searchingTree = true;
    searchTreeTask = Bukkit.getScheduler()
        .runTaskAsynchronously(ChoppingMachine.getInstance(), () -> {
          int radius = type.getSearchRadius();
          Location centerLocation = this.clone();

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
                        && block.getRelative(BlockFace.DOWN).getType() == Material.DIRT
                        && !isTreeFound(block.getLocation())) {
                      foundTress.add(block.getLocation());
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
        this,
        location, 10);
  }

  private void unsetPath() {
    if (foundTree != null) {
      removeFoundTree(foundTree.getTargetLog());
      foundTree = null;
    }
  }

  private void handleMoveToTree() {
    Location origin = foundTree.getOrigin().clone();
    if (LocationUtils.xzDistance(origin, foundTree.getTargetLog()) <= 3) {
      ArrayList<Block> treeLogs = new ArrayList<>();
      Block targetLog = foundTree.getTargetLog().getBlock();
      getTreeLogs(targetLog, targetLog, treeLogs, null);
      this.currentTreeLogs.addAll(treeLogs);
      unsetPath();
      return;
    }
    if (!foundTree.shouldTryAgain()) {
      unsetPath();
      return;
    }
    Location nextPos = foundTree.getNextPos();
    if (nextPos == null) {
      unsetPath();
      return;
    }
    updatePos(nextPos);
  }

  private void updatePos(Location newPos) {
    //move blocks
    getBlock().setType(Material.AIR);
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

  /**
   * Recursive method to get all connected logs to a log
   * <p>
   * exploring in all directions to find connected logs
   */
  private void getTreeLogs(Block primaryBlock, Block currentBlock, List<Block> treeLogs,
      List<Block> treeLeaves) {
    if (treeLeaves == null) {
      treeLeaves = new ArrayList<>();
    }
    if (!treeLogs.contains(currentBlock) && !treeLeaves.contains(currentBlock)) {
      boolean callRecursive = false;
      if (type.isWoodenLog(currentBlock.getType())) {
        callRecursive = true;
        treeLogs.add(currentBlock);
      } else if (isLeaf(currentBlock.getType())) {
        callRecursive = true;
        treeLeaves.add(currentBlock);
      }
      if (callRecursive) {
        for (BlockFace face : BlockFace.values()) {
          Block relative = currentBlock.getRelative(face);
          if (LocationUtils.xzDistance(primaryBlock.getLocation(), relative.getLocation()) > 5) {
            continue;
          }
          getTreeLogs(primaryBlock, relative, treeLogs, treeLeaves);
        }
      }
    }
  }

  private boolean checkCounter() {
    if (tickCounter == -1) {
      tickCounter = type.getTickInterval();
    }
    return tickCounter-- == 0;
  }

  public boolean verify(boolean setBlock) {
    boolean verified = getBlock().getType() == type.getMachineBlockMaterial();
    if (!verified && setBlock) {
      getBlock().setType(type.getMachineBlockMaterial());
      return true;
    }
    return verified;
  }

  public String getStatus() {
    if (chest == null) {
      return MachineStatus.CHEST_MISSING.getStatus();
    }
    return status.getStatus();
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
      Block block = getBlock().getRelative(direction);
      for (Material booster : boosters) {
        if (block.getType() == booster) {
          block.setType(Material.AIR);
          currentTreeLogs.forEach(log -> {
            this.type.attemptChop(this, log);
          });
          currentTreeLogs.clear();
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public World getWorld() {
    if (super.getWorld() == null) {
      super.setWorld(Bukkit.getWorld(worldName));
    }
    return super.getWorld();
  }

  private boolean checkDestructiveNearby() {
    Material[] destructiveBlocks = {Material.RED_MUSHROOM, Material.BROWN_MUSHROOM,
        Material.POPPY, Material.DANDELION, Material.SUNFLOWER, Material.LILAC, Material.PEONY,
        Material.CHORUS_FLOWER};

    //faces
    BlockFace[] directions = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST,
        BlockFace.SOUTH, BlockFace.WEST};

    for (BlockFace direction : directions) {
      Block block = getBlock().getRelative(direction);
      for (Material destructiveBlock : destructiveBlocks) {
        if (block.getType() == destructiveBlock) {
          ChoppingMachine.getInstance().getServer().getScheduler()
              .runTask(ChoppingMachine.getInstance(), () -> {
                Objects.requireNonNull(getWorld())
                    .dropItemNaturally(this, type.getMachineItem());
                //destroy machine
                MachineManager.getInstance().destroyMachine(this);
                //smoke
                ChoppingMachine.getInstance().getParticleApi().LIST_1_8.SMOKE_LARGE
                    .packetMotion(true, locObj().add(0.5, 0.5, 0.5).toVector(), 0D, 0.1D,
                        0D)
                    .sendTo(Utils.getNearbyPlayers(locObj(), getType().getSearchRadius()));
                //sound
                Objects.requireNonNull(getWorld())
                    .playSound(this, Sound.ENTITY_ITEM_BREAK, 1, 1);
                //set air
                getBlock().setType(Material.AIR);

                //check if chest is placed
                if (chest != null) {
                  //drop chest items
                  for (ItemStack itemStack : chest.getInventory().getContents()) {
                    if (itemStack != null) {
                      Objects.requireNonNull(getWorld())
                          .dropItemNaturally(this, itemStack);
                    }
                  }
                  //set chest to air
                  chest.getBlock().setType(Material.AIR);
                }
                //notify owner
                notifyOwner("§cMachine destroyed due to destructive block nearby at "
                        + getBlockX() + ", " + getBlockY() + ", "
                        + getBlockZ() + "in world " + getWorld().getName(),
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

  public Location locObj() {
    return new Location(this.getWorld(), this.getBlockX(), this.getBlockY(), this.getBlockZ());
  }

  public void save(JsonObjectBuilder object) {
    JsonObjectBuilder instObjBuilder = Json.createObjectBuilder();
    instObjBuilder.add("type", type.getMachineIdentifier());
    instObjBuilder.add("owner", owner.toString());
    instObjBuilder.add("world", worldName);
    instObjBuilder.add("location_x", getX());
    instObjBuilder.add("location_y", getY());
    instObjBuilder.add("location_z", getZ());
    JsonObject instObj = instObjBuilder.build();
    object.add(locId(), instObj);
  }

  public String locId() {
    return worldName + "_" + getBlockX() + "_" + getBlockY() + "_" + getBlockZ();
  }

  public void onDisable() {
    if (searchTreeTask != null) {
      searchTreeTask.cancel();
    }
  }

  private boolean isLeaf(Material type) {
    return type == Material.ACACIA_LEAVES
        || type == Material.BIRCH_LEAVES
        || type == Material.DARK_OAK_LEAVES
        || type == Material.JUNGLE_LEAVES
        || type == Material.OAK_LEAVES
        || type == Material.SPRUCE_LEAVES;
  }

}
