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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.types.BaseMachineType;
import net.nasiridrishi.choppingmachine.machine.types.DiamondMachine;
import net.nasiridrishi.choppingmachine.machine.types.EmeraldMachine;
import net.nasiridrishi.choppingmachine.machine.types.GoldMachine;
import net.nasiridrishi.choppingmachine.utils.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.patheloper.api.pathing.Pathfinder;
import org.patheloper.api.pathing.rules.PathingRuleSet;
import org.patheloper.mapping.PatheticMapper;

public class MachineManager implements Listener {

  @Getter
  private static MachineManager instance;

  private final ChoppingMachine plugin;

  private final List<BaseMachineType> machineTypes = new ArrayList<>();

  @Getter
  private final MachineList machineList;

  @Getter
  private final MachineConfigs machineSettings;

  @Getter
  private final Pathfinder pathfinder;

  private final int tickTask;
  private final int saveTask;

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

    this.machineList = new MachineList(plugin);

    //register manager as listener
    this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);

    this.pathfinder = PatheticMapper.newPathfinder(PathingRuleSet.createAsyncRuleSet()
        .withAllowingFallback(true)
        .withLoadingChunks(true)
        .withMaxIterations(2000));
    registerCommand();

    //start ticking
    tickTask = this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
      //iterate through all machines that are in loaded chunks
      Iterator<MachineInstance> iterator = machineList.runnable().iterator();
      while (iterator.hasNext()) {
        iterator.next().onTick();
      }
    }, 20L, 1L);

    saveTask = this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
      //save machines data if dirty
      if (machineList.isDirty() && !machineList.isSaving()) {
        machineList.setSaving(true);
        machineList.save();
        machineList.setDirty(false);
      }
    }, 20L, 100L);
  }

  public void registerInstance(MachineInstance machine) {
    if (machineList.containsKey(machine.getId())) {
      throw new IllegalStateException("Machine instance already registered");
    }
    machineList.put(machine.getId(), machine);
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
          BaseMachineType machineType = (BaseMachineType) args.get("machineType");
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

  public Argument<BaseMachineType> machineArgument(String nodeName) {
    return new CustomArgument<>(new StringArgument(nodeName), info -> {
      BaseMachineType machineType = getMachineType(info.input());

      if (machineType == null) {
        throw CustomArgumentException.fromMessageBuilder(
            new MessageBuilder("Unknown world: ").appendArgInput());
      } else {
        return machineType;
      }
    }).replaceSuggestions(ArgumentSuggestions.strings(info ->

        machineTypes.stream().map(BaseMachineType::getMachineIdentifier).toArray(String[]::new)
    ));
  }

  public BaseMachineType getMachineType(String identifier) {
    for (BaseMachineType machineType : machineTypes) {
      if (machineType.getMachineIdentifier().equalsIgnoreCase(identifier)) {
        return machineType;
      }
    }
    return null;
  }

  public void destroyMachine(MachineInstance machineInstance) {
    machineInstance.destroy();
    machineList.remove(machineInstance.getId());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    machineList.forEach((location, machineInstance) -> {
      if (machineInstance.equals(event.getBlock().getLocation())) {
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
      machineList.forEach((location, machineInstance) -> {
        Location mLocation = machineInstance.locObj();
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
    for (BaseMachineType machineType : machineTypes) {
      if (machineType.isMachineItem(itemStack)) {
        Player player = event.getPlayer();
        if (player.hasPermission(Permissions.MACHINE_PLACE)) {
          //check if there is a machine instance in the same chunk
          //if found, prevent another machine from being placed

          Location blLc = event.getBlock().getLocation();

          if (!machineList.inChunk(blLc.getBlockX() >> 4, blLc.getBlockZ() >> 4, blLc.getWorld())
              .isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED
                + "There are already one or more machines in this chunk. Please try moving to another location");
            return;
          }

          //instantiate machine
          MachineInstance machineInstance = new MachineInstance(machineType,
              player.getUniqueId(), blLc.getBlockX(), blLc.getBlockY(), blLc.getBlockZ(),
              blLc.getWorld());

          registerInstance(machineInstance);
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
      machineList.forEach((location, machineInstance) -> {
        Location mLocation = machineInstance.locObj();
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


  @EventHandler(priority = EventPriority.HIGHEST)
  public void onMachineItemSpawn(ItemSpawnEvent event) {
    ItemStack itemStack = event.getEntity().getItemStack();
    for (BaseMachineType machineType : machineTypes) {
      if (machineType.isMachineItem(itemStack)) {
        event.getEntity()
            .setCustomName(Objects.requireNonNull(itemStack.getItemMeta()).getDisplayName());
        event.getEntity().setCustomNameVisible(true);
      }
    }
  }

  public void updateMachineLocation(MachineInstance instance, Location newPos) {
    if (!machineList.containsKey(instance.getId())) {
      throw new IllegalStateException("Machine instance not registered");
    }
    instance.setX(newPos.getBlockX());
    instance.setY(newPos.getBlockY());
    instance.setZ(newPos.getBlockZ());

    machineList.setDirty(true);
  }

  public void onDisable() {
    plugin.getServer().getScheduler().cancelTask(tickTask);
    plugin.getServer().getScheduler().cancelTask(saveTask);
    machineList.forEach((location, machineInstance) -> {
      machineInstance.onDisable();
    });
    machineList.save();
  }
}

