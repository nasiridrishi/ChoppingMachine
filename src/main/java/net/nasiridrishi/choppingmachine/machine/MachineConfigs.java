package net.nasiridrishi.choppingmachine.machine;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.utils.Permissions;
import org.bukkit.configuration.file.FileConfiguration;

public class MachineConfigs {

  public static final String DIAMOND_MACHINE = "Diamond";
  public static final String GOLD_MACHINE = "Gold";
  public static final String EMERALD_MACHINE = "Emerald";

  private static final String TICK_INTERVAL = "tick_interval";
  private static final String SEARCH_RADIUS = "search_radius";

  private static final String YIELD_MULTIPLIER = "yield_multiplier";

  private final ChoppingMachine plugin;
  private FileConfiguration config;

  public MachineConfigs(ChoppingMachine plugin) {
    this.plugin = plugin;
    plugin.saveDefaultConfig();
    config = plugin.getConfig();

    plugin.getMainCommand().withSubcommand(new CommandAPICommand("settings")
        .withPermission(Permissions.MACHINE_ADMIN)
        .withSubcommand(settingsCommand(DIAMOND_MACHINE))
        .withSubcommand(settingsCommand(GOLD_MACHINE))
        .withSubcommand(settingsCommand(EMERALD_MACHINE)
        ));
  }

  private CommandAPICommand settingsCommand(String machineType) {
    return new CommandAPICommand(machineType)
        .withSubcommand(new CommandAPICommand("tick_interval")
            .withArguments(new IntegerArgument("tick_interval"))
            .executesPlayer((player, args) -> {
              int tickInterval = (int) args.get("tick_interval");
              config.set("machines." + machineType + "." + TICK_INTERVAL,
                  tickInterval);
              syncConfig();
              player.sendMessage(machineType + " tick interval set to " + tickInterval);
            })
        )
        .withSubcommand(new CommandAPICommand("search_radius")
            .withArguments(new IntegerArgument("search_radius"))
            .executesPlayer((player, args) -> {
              int searchRadius = (int) args.get("search_radius");
              config.set("machines." + machineType + "." + SEARCH_RADIUS,
                  searchRadius);
              syncConfig();
              player.sendMessage(machineType + " search radius set to " + searchRadius);
            })

        ).withSubcommand(new CommandAPICommand("yield_multiplier")
            .withArguments(new IntegerArgument("yield_multiplier"))
            .executesPlayer((player, args) -> {
              int yieldMultiplier = (int) args.get("yield_multiplier");
              config.set("machines." + machineType + "." + YIELD_MULTIPLIER,
                  yieldMultiplier);
              syncConfig();
              player.sendMessage(machineType + " yield multiplier set to " + yieldMultiplier);
            })
        );
  }


  private void syncConfig() {
    plugin.saveConfig();
    plugin.reloadConfig();
    config = plugin.getConfig();
  }

  public int getDiamondTickInterval() {
    return config.getInt("machines." + DIAMOND_MACHINE + "." + TICK_INTERVAL);
  }

  public int getDiamondSearchRadius() {
    return config.getInt("machines." + DIAMOND_MACHINE + "." + SEARCH_RADIUS);
  }

  public int getGoldTickInterval() {
    return config.getInt("machines." + GOLD_MACHINE + "." + TICK_INTERVAL);
  }

  public int getGoldSearchRadius() {
    return config.getInt("machines." + GOLD_MACHINE + "." + SEARCH_RADIUS);
  }

  public int getEmeraldTickInterval() {
    return config.getInt("machines." + EMERALD_MACHINE + "." + TICK_INTERVAL);
  }

  public int getEmeraldSearchRadius() {
    return config.getInt("machines." + EMERALD_MACHINE + "." + SEARCH_RADIUS);
  }

  public int getDiamondYieldMultiplier() {
    return config.getInt("machines." + DIAMOND_MACHINE + "." + YIELD_MULTIPLIER);
  }

  public int getGoldYieldMultiplier() {
    return config.getInt("machines." + GOLD_MACHINE + "." + YIELD_MULTIPLIER);
  }

  public int getEmeraldYieldMultiplier() {
    return config.getInt("machines." + EMERALD_MACHINE + "." + YIELD_MULTIPLIER);
  }


}
