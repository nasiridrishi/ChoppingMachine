package net.nasiridrishi.choppingmachine.machine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import lombok.Getter;
import lombok.Setter;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class MachineList extends HashMap<Integer, MachineInstance> {

  private final Object lock = new Object();

  private final File machineDataFile;

  private final ChoppingMachine plugin;
  @Getter

  @Setter
  private boolean isDirty = false;

  @Setter
  @Getter
  private boolean saving = false;

  private boolean loaded = false;

  public MachineList(ChoppingMachine plugin) {
    this.plugin = plugin;
    this.machineDataFile = new File(plugin.getDataFolder() + "/machines.json");
    this.load();
  }

  public void save() {
    synchronized (lock) {
      JsonObjectBuilder jsonObjBuilder = Json.createObjectBuilder(); // Use JsonObjectBuilder
      if (!isEmpty()) {
        forEach((id, machineInstance) -> machineInstance.save(jsonObjBuilder)); // Pass builder
      }
      try (FileWriter fileWriter = new FileWriter(machineDataFile)) {
        fileWriter.write(jsonObjBuilder.build().toString()); // Build JSON object
        plugin.getLogger().log(Level.INFO, "Machines data saved successfully.");
      } catch (IOException e) {
        plugin.getLogger().log(Level.WARNING, "Error writing machines data file", e);
      }
    }
  }

  @Override
  public MachineInstance put(Integer key, MachineInstance value) {
    if (at(value.locObj()) != null) {
      Bukkit.getLogger()
          .warning("A Machine already exists at location: " + "x: " + value.getBlockX()
              + " y: " + value.getBlockY() + " z: " + value.getBlockZ() + " in world: "
              + value.getWorldName());
      return null;
    }
    super.put(key, value);
    setDirty(true);
    return value;
  }

  @Override
  public MachineInstance remove(Object key) {
    MachineInstance machineInstance = super.remove(key);
    setDirty(true);
    return machineInstance;
  }

  public void load() {
    if (loaded) {
      plugin.getLogger().warning("Tried to load machines twice. Ignoring to prevent duplicates...");
      return;
    }

    plugin.getLogger().info("Loading machines data...");

    // File existence and creation
    if (!machineDataFile.exists()) {
      plugin.getLogger()
          .info("Machines data file not found. Creating a new one with default structure...");
      try {
        // Create a default JSON structure in the file
        Files.writeString(machineDataFile.toPath(), "{}");
      } catch (IOException e) {
        plugin.getLogger().log(Level.SEVERE, "Error creating machines data file", e);
        return;
      }
    }

    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(machineDataFile))) {
      JsonReader jsonReader = Json.createReader(bufferedReader);

      try {
        JsonObject jsonObject = jsonReader.readObject();
        loadFromJson(jsonObject);
        plugin.getLogger().info("Loaded " + size() + " machines");
        loaded = true;
      } catch (JsonParsingException e) {
        plugin.getLogger().log(Level.WARNING, "Error parsing machines data file", e);
      } catch (Exception e) {
        plugin.getLogger().log(Level.WARNING, "Error parsing machine data", e);
      }
    } catch (IOException e) {
      plugin.getLogger().log(Level.WARNING, "Error reading machines data file", e);
    }
  }


  private void loadFromJson(JsonObject jsonObject) {
    for (String key : jsonObject.keySet()) {
      JsonObject machineJson = jsonObject.getJsonObject(key);
      try {
        MachineInstance machine = MachineInstance.fromJson(machineJson);
        super.put(machine.getId(), machine);
      } catch (Exception e) {
        plugin.getLogger().log(Level.WARNING, "Error parsing machine data", e);
      }
    }
  }

  @Override
  public void clear() {
    super.clear();
    setDirty(true);
  }


  //List of machines that are in loaded chunks and have an owner that is online
  //So they can tick and chop logs
  public List<MachineInstance> runnable() {
    return loadedChunks().stream()
        .filter(mch -> Bukkit.getPlayer(mch.getOwner()) != null)
        .collect(Collectors.toList());
  }

  public List<MachineInstance> inChunk(int x, int y, World world) {
    return values().stream()
        .filter(mch -> mch.getWorld() != null)
        .filter(mch -> mch.getWorld().equals(world))
        .filter(mch -> mch.getBlockX() >> 4 == x)
        .filter(mch -> mch.getBlockZ() >> 4 == y)
        .collect(Collectors.toList());
  }

  public List<MachineInstance> loadedChunks() {
    return values().stream()
        .filter(mch -> mch.getWorld() != null)
        .filter(mch -> mch.getWorld().isChunkLoaded(mch.getBlockX() >> 4, mch.getBlockZ() >> 4))
        .collect(Collectors.toList());
  }

  public List<MachineInstance> loadedWorlds() {
    return values().stream()
        .filter(mch -> mch.getWorld() != null)
        .filter(mch -> mch.getWorld().isChunkLoaded(mch.getBlockX() >> 4, mch.getBlockZ() >> 4))
        .collect(Collectors.toList());
  }

  public MachineInstance at(Location location) {
    return values().stream()
        .filter(mch -> mch.getWorld() != null)
        .filter(mch -> mch.getWorld().equals(location.getWorld()))
        .filter(mch -> mch.getBlockX() == location.getBlockX())
        .filter(mch -> mch.getBlockY() == location.getBlockY())
        .filter(mch -> mch.getBlockZ() == location.getBlockZ())
        .findFirst().orElse(null);
  }

}
