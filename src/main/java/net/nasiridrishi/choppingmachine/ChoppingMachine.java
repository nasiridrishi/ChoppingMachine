package net.nasiridrishi.choppingmachine;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.core.ParticleNativeCore;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import lombok.Getter;
import net.nasiridrishi.choppingmachine.machine.MachineManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.patheloper.mapping.PatheticMapper;

public final class ChoppingMachine extends JavaPlugin {

  @Getter
  private static ChoppingMachine instance;

  @Getter
  ParticleNativeAPI particleApi;

  @Getter
  private MachineManager machineManager;

  @Getter
  private boolean hologramsEnabled = false;

  private CommandAPICommand mainCommand;

  @Override
  public void onLoad() {
    instance = this;
    CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(true));

    if (getServer().getPluginManager().getPlugin("DecentHolograms") != null) {
      hologramsEnabled = true;
      getLogger().info("DecentHolograms found. Enabling holograms...");
    } else {
      getLogger().warning("DecentHolograms not found. Holograms will be disabled.");
      getLogger().warning("DecentHolograms is required for machines to work properly.");
      getLogger().warning("Download it here: https://www.spigotmc.org/resources/96927/");
    }
  }

  @Override
  public void onEnable() {
    CommandAPI.onEnable();
    PatheticMapper.initialize(this);
    machineManager = new MachineManager(this);
    particleApi = ParticleNativeCore.loadAPI(this);

    getMainCommand().register();

  }

  public CommandAPICommand getMainCommand() {
    if (mainCommand == null) {
      mainCommand = new CommandAPICommand("chopper")
          .withAliases("cmachine", "choppingmachine");
    }
    return mainCommand;
  }

  @Override
  public void onDisable() {
    CommandAPI.onDisable();
    machineManager.getMachineStorage().onDisable();
  }
}
