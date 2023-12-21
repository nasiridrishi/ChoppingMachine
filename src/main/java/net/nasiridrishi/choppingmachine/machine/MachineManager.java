package net.nasiridrishi.choppingmachine.machine;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException;
import dev.jorel.commandapi.arguments.CustomArgument.MessageBuilder;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.types.BaseMachine;
import net.nasiridrishi.choppingmachine.machine.types.DiamondMachine;
import net.nasiridrishi.choppingmachine.machine.types.EmeraldMachine;
import net.nasiridrishi.choppingmachine.machine.types.GoldMachine;
import net.nasiridrishi.choppingmachine.storage.MachineData;
import net.nasiridrishi.choppingmachine.storage.MachineStorage;
import net.nasiridrishi.choppingmachine.utils.LocationUtils;
import net.nasiridrishi.choppingmachine.utils.Permissions;
import net.nasiridrishi.choppingmachine.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.patheloper.api.pathing.Pathfinder;
import org.patheloper.api.pathing.rules.PathingRuleSet;
import org.patheloper.mapping.PatheticMapper;

public class MachineManager implements Listener {

  @Getter
  private static MachineManager instance;

  private final ChoppingMachine plugin;

  private final List<BaseMachine> machineTypes = new ArrayList<>();

  @Getter
  private final MachineStorage machineStorage;

  @Getter
  private final MachineConfigs machineSettings;

  @Getter
  private final Pathfinder pathfinder;

  /**
   * List of all placed machines<br> Key: Location of machine as string<br> Value:
   */
  @Getter
  private final Map<String, MachineInstance> machineInstances = new HashMap<>();


  public MachineManager(ChoppingMachine plugin) {
    if (instance != null) {
      throw new IllegalStateException("MachineManager already initialized");
    }
    instance = this;
    this.plugin = plugin;

    //register machine types
    this.machineTypes.add(new DiamondMachine());
    this.machineTypes.add(new EmeraldMachine());
    this.machineTypes.add(new GoldMachine());

    this.machineSettings = new MachineConfigs(plugin);

    this.machineStorage = new MachineStorage(plugin);

    initialLoad();

    //register manager as listener
    this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);

    this.pathfinder = PatheticMapper.newPathfinder(PathingRuleSet.createAsyncRuleSet()
        .withAllowingFailFast(true)
        .withAllowingFallback(true)
        .withLoadingChunks(true)
        .withMaxLength(200)
        .withMaxIterations(50000));

    registerCommand();

    //start ticking
    this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
      //iterator
      Iterator<MachineInstance> iterator = machineInstances.values().iterator();
      while (iterator.hasNext()) {
        iterator.next().onTick();
      }
    }, 20L, 1L);

  }

  private void initialLoad() {
    List<MachineInstance> deleteInstanceList = new ArrayList<>();
    List<MachineData> machineDataList = new ArrayList<>(machineStorage.getMachinesData().values());

    machineDataList.forEach(machineData -> {
      Location mcLc = machineData.getLocationObj();
      if (mcLc == null) {
        return;
      }
      if (Objects.requireNonNull(mcLc.getWorld())
          .isChunkLoaded(mcLc.getBlockX() >> 4, mcLc.getBlockZ() >> 4)) {
        MachineInstance machineInstance = machineData.instantiate();
        if (!machineInstance.verify(false)) {
          plugin.getLogger().warning(
              "Found an invalid stored machine which is not valid anymore due to machine's block type not matching the available block at the location. Deleting it...");
          deleteInstanceList.add(machineInstance);
        } else {
          registerInstance(machineInstance);
        }
      } else {
        plugin.getLogger().warning("Chunk is not loaded for a machine");
      }
    });

    if (!deleteInstanceList.isEmpty()) {
      deleteInstanceList.forEach(machineStorage::destroy);
      machineStorage.setDirty(true);
    }

    if (!machineInstances.isEmpty()) {
      plugin.getLogger().info("Loaded " + machineInstances.size() + " machines on initial load");
    }
  }

  public void registerInstance(MachineInstance machineInstance) {
    if (machineInstances.containsKey(machineInstance.getUid())) {
      throw new IllegalStateException("Machine instance already registered");
    }
    machineInstances.put(machineInstance.getUid(), machineInstance);
    plugin.getLogger().info("Registered machine instance " + machineInstance.getUid());
  }


  /**
   * Register give machine commands
   */
  private void registerCommand() {
    plugin.getMainCommand().withSubcommand(new CommandAPICommand("give")
        .withPermission(Permissions.MACHINE_GIVE)
        .withArguments(new PlayerArgument("player"))
        .withArguments(machineArgument("machineType"))
        .executesPlayer((sender, args) -> {
          Player player = (Player) args.get("player");
          BaseMachine machineType = (BaseMachine) args.get("machineType");
          assert machineType != null;
          ItemStack machineItem = machineType.getMachineItem();
          assert player != null;
          player.getInventory().addItem(machineItem);
          sender.sendMessage(ChatColor.GREEN + "Gave " + player.getName() + " a "
              + machineType.getMachineIdentifier() + " machine");
          player.sendMessage(ChatColor.GREEN + "You have been given a "
              + machineType.getMachineIdentifier() + " machine");
        }));
  }

  public Argument<BaseMachine> machineArgument(String nodeName) {
    return new CustomArgument<>(new StringArgument(nodeName), info -> {
      BaseMachine machineType = getMachineType(info.input());

      if (machineType == null) {
        throw CustomArgumentException.fromMessageBuilder(
            new MessageBuilder("Unknown world: ").appendArgInput());
      } else {
        return machineType;
      }
    }).replaceSuggestions(ArgumentSuggestions.strings(info ->

        machineTypes.stream().map(BaseMachine::getMachineIdentifier).toArray(String[]::new)
    ));
  }

  public BaseMachine getMachineType(String identifier) {
    for (BaseMachine machineType : machineTypes) {
      if (machineType.getMachineIdentifier().equalsIgnoreCase(identifier)) {
        return machineType;
      }
    }
    return null;
  }

  public void destroyMachine(MachineInstance machineInstance) {
    //despawn holograms etc
    machineInstance.destroy();
    //remove from instances
    machineInstances.remove(machineInstance.getUid());
    //delete machine data
    machineStorage.destroy(machineInstance);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    machineInstances.forEach((location, machineInstance) -> {
      if (machineInstance.getLocation().equals(event.getBlock().getLocation())) {
        if (player.hasPermission(Permissions.MACHINE_BREAK)) {
          event.setDropItems(false);
          plugin.getServer().getScheduler().runTask(plugin, () -> {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
                machineInstance.getType().getMachineItem());
            destroyMachine(machineInstance);
            player.sendMessage(ChatColor.GREEN + "You have successfully broken a " + machineInstance
                .getType().getMachineIdentifier() + " machine");
          });
        } else {
          event.setCancelled(true);
          player.sendMessage(ChatColor.RED + "You don't have permission to break this machine");
        }
      }
    });
    Block block = event.getBlock();
    if (block.getType() == Material.CHEST) {
      Chest chest = (Chest) block.getState();
      machineInstances.forEach((location, machineInstance) -> {
        Location mLocation = machineInstance.getLocation().clone();
        Location chestLocation = chest.getBlock().getLocation().clone();
        if (mLocation.getBlockX() == chestLocation.getBlockX()
            && mLocation.getBlockY() == chestLocation.getBlockY() - 1
            && mLocation.getBlockZ() == chestLocation.getBlockZ()) {
          if (machineInstance.getOwner().equals(event.getPlayer().getUniqueId())
              || event.getPlayer().hasPermission(Permissions.MACHINE_OVERRIDE)) {
            machineInstance.onChestRemoved();
            machineInstance.updateHologram();
            player.sendMessage(ChatColor.YELLOW + "You have removed a chest from "
                + machineInstance.getType().getMachineIdentifier()
                + " machine. This machine is now deactivated");
          } else {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED
                + "You must be the owner of chopping machine to remove a chest from it");
          }
        }
      });
    }
  }


  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBlockPlaceEvent(BlockPlaceEvent event) {
    ItemStack itemStack = event.getItemInHand();
    for (BaseMachine machineType : machineTypes) {
      if (machineType.isMachineItem(itemStack)) {
        Player player = event.getPlayer();
        if (player.hasPermission(Permissions.MACHINE_PLACE)) {
          //check if there is a machine instance in the same chunk
          //if found, prevent another machine from being placed

          Location blLc = event.getBlock().getLocation();

          if (getChunkMachine(blLc.getBlockX() >> 4, blLc.getBlockZ() >> 4, blLc.getWorld())
              != null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED
                + "There is already a machine in this chunk. Please try moving to another location");
            return;
          }

          //instantiate machine
          MachineInstance machineInstance = new MachineInstance(machineType,
              event.getBlock().getLocation(), player.getUniqueId());

          registerInstance(machineInstance);
          machineStorage.add(machineInstance);
          player.sendMessage(ChatColor.GREEN + "You have successfully placed a "
              + machineInstance.getType().getMachineIdentifier() + " machine");
          player.sendMessage(ChatColor.GREEN
              + "You can now place a chest on top of it to activate machine and start "
              + "chopping logs");
        } else {
          event.setCancelled(true);
          player.sendMessage(ChatColor.RED + "You don't have permission to place this machine");
        }
      }
    }
    Block block = event.getBlock();
    if (block.getType() == Material.CHEST) {
      Chest chest = (Chest) block.getState();
      machineInstances.forEach((location, machineInstance) -> {
        Location mLocation = machineInstance.getLocation().clone();
        Location chestLocation = chest.getBlock().getLocation().clone();
        if (mLocation.getBlockX() == chestLocation.getBlockX()
            && mLocation.getBlockY() == chestLocation.getBlockY() - 1
            && mLocation.getBlockZ() == chestLocation.getBlockZ()) {
          if (machineInstance.getOwner().equals(event.getPlayer().getUniqueId())) {
            machineInstance.onChestPlaced(chest);
            machineInstance.updateHologram();
          } else {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED
                + "You must be the owner of chopping machine to place a chest on it");
          }
        }
      });
    }
  }

  private MachineData getChunkMachine(int x, int y, World world) {
    //check for machines in the same chunk
    for (MachineData machineData : machineStorage.getMachinesData().values()) {
      Location mcLc;
      try {
        mcLc = LocationUtils.fromString(machineData.getLocation());
      } catch (IllegalArgumentException e) {
        continue;
      }
      if (x == mcLc.getBlockX() >> 4
          && y == mcLc.getBlockZ() >> 4
          && mcLc.getWorld() == world) {
        return machineData;
      }
    }
    return null;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onChunkLoad(ChunkLoadEvent event) {
    Chunk loadedChunk = event.getChunk();
    for (MachineData machineData : machineStorage.getMachinesData().values()) {
      if (isInstantiated(machineData)) {
        continue;
      }
      Location mcLc;
      try {
        mcLc = LocationUtils.fromString(machineData.getLocation());
      } catch (IllegalArgumentException e) {
        continue;
      }
      if (loadedChunk.getX() == mcLc.getBlockX() >> 4
          && loadedChunk.getZ() == mcLc.getBlockZ() >> 4
          && mcLc.getWorld() == loadedChunk.getWorld()) {

        MachineInstance newMachineInstance;
        try {
          newMachineInstance = machineData.instantiate();
        } catch (IllegalArgumentException e) {
          continue;
        }
        if (!newMachineInstance.verify(false)) {
          plugin.getLogger().warning(
              "Found a invalid stored machine which is not valid anymore due to machines block type is not same as available block at location. Deleting it...");
          //delete from storage
          machineStorage.destroy(newMachineInstance);
          continue;
        }
        registerInstance(newMachineInstance);
      }
    }
  }

  private boolean isInstantiated(MachineData machineData) {
    return machineData.getLocationObj() != null && machineInstances.containsKey(
        Utils.formulateMachineUid(machineData));
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onMachineItemSpawn(ItemSpawnEvent event) {
    ItemStack itemStack = event.getEntity().getItemStack();
    for (BaseMachine machineType : machineTypes) {
      if (machineType.isMachineItem(itemStack)) {
        event.getEntity()
            .setCustomName(Objects.requireNonNull(itemStack.getItemMeta()).getDisplayName());
        event.getEntity().setCustomNameVisible(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onChunkUnload(ChunkLoadEvent event) {
    Chunk unloadedChunk = event.getChunk();
    List<MachineInstance> instancesToRemove = new ArrayList<>();

    machineInstances.forEach((location, machineInstance) -> {
      Location mcLc = machineInstance.getLocation();
      if (unloadedChunk.getX() == mcLc.getBlockX() >> 4
          && unloadedChunk.getZ() == mcLc.getBlockZ() >> 4) {
        instancesToRemove.add(machineInstance);
      }
    });

    for (MachineInstance instance : instancesToRemove) {
      machineInstances.remove(instance.getUid());
    }
  }


  public void updateMachineLocation(MachineInstance instance, Location newPos) {
    if (!machineInstances.containsKey(instance.getUid())) {
      throw new IllegalStateException("Machine instance not registered");
    }
    Location currentPos = instance.getLocation().clone();
    machineInstances.remove(instance.getUid());
    instance.setLocation(newPos);
    machineInstances.put(instance.getUid(), instance);
    machineStorage.updateLocation(currentPos, newPos);
  }
}

