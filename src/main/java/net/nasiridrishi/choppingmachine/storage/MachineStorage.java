package net.nasiridrishi.choppingmachine.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.MachineInstance;
import net.nasiridrishi.choppingmachine.utils.LocationUtils;
import org.bukkit.Location;
import org.json.JSONArray;

public class MachineStorage {

  private final File machineDataFile;

  private final ChoppingMachine plugin;
  @Getter
  private Map<String, MachineData> machinesData = new HashMap<>();

  @Setter
  private boolean isDirty = false;

  public MachineStorage(ChoppingMachine plugin) {
    this.plugin = plugin;
    this.machineDataFile = new File(plugin.getDataFolder() + "/machines.json");
    this.loadFromFile();

    plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      if (isDirty) {
        saveAsync();
        isDirty = false;
      }
    }, 0, 20);
  }

  /**
   * Performs an async save of the machines data
   */
  private void saveAsync() {
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveSync);
  }

  private void saveSync() {
    JSONArray jsonArray = new JSONArray();
    if (!machinesData.isEmpty()) {
      for (MachineData machineData : machinesData.values()) {
        jsonArray.put(machineData.toJson());
      }
    } else {
      //empty json array
      jsonArray.put(new JSONArray());
    }
    try (FileWriter fileWriter = new FileWriter(machineDataFile)) {
      fileWriter.write(jsonArray.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void onDisable() {
    saveSync();
  }

  public MachineData getMachineData(Location location) {
    return machinesData.get(LocationUtils.toString(location));
  }


  //Not doing async because its only called once on startup
  private void loadFromFile() {
    if (!machineDataFile.exists()) {
      plugin.getLogger().info("No machines data file found");
      return;
    }
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(machineDataFile))) {
      StringBuilder stringBuilder = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line);
      }
      machinesData = fromJsonArray(stringBuilder.toString());
    } catch (IOException e) {
      plugin.getLogger().warning("Error reading machines data file");
      e.printStackTrace();
      ;
    }
  }

  /**
   * @param jsonArrayString: MachineData json array string from file
   * @return
   */
  private Map<String, MachineData> fromJsonArray(String jsonArrayString) {
    Map<String, MachineData> machineDataList = new HashMap<>();
    JSONArray jsonArray;
    try {
      jsonArray = new JSONArray(jsonArrayString);
    } catch (Exception e) {
      plugin.getLogger().warning("Error parsing machines data file");
      e.printStackTrace();
      return new HashMap<>();
    }
    for (int i = 0; i < jsonArray.length(); i++) {
      try {
        MachineData machineData = MachineData.fromJson(jsonArray.getJSONObject(i));
        machineDataList.put(machineData.getLocation(), machineData);
      } catch (Exception e) {
        plugin.getLogger().warning("Error parsing machine data at index");
        e.printStackTrace();
      }
    }
    return machineDataList;
  }

  public void destroy(MachineInstance machineInstance) {
    this.machinesData.remove(LocationUtils.toString(machineInstance.getLocation()));
    setDirty(true);
  }

  public void add(MachineInstance machineInstance) {
    if (machinesData.containsKey(LocationUtils.toString(machineInstance.getLocation()))) {
      throw new RuntimeException("A machine already exists at this location");
    }
    machinesData.put(LocationUtils.toString(machineInstance.getLocation()),
        MachineData.fromMachineInstance(machineInstance));
    setDirty(true);
  }


  public void updateLocation(Location currentPos, Location newPos) {
    if (LocationUtils.equals(currentPos, newPos)) {
      ChoppingMachine.getInstance().getLogger()
          .warning("Tried to update machine location to same location");
      return;
    }
    MachineData machineData = machinesData.get(LocationUtils.toString(currentPos));
    if (machineData == null) {
      throw new RuntimeException("No machine found at old location");
    }
    if (machinesData.containsKey(LocationUtils.toString(newPos))) {
      throw new RuntimeException("A machine already exists at new location");
    }
    machineData.setLocation(newPos);
    machinesData.remove(LocationUtils.toString(currentPos));
    machinesData.put(LocationUtils.toString(newPos), machineData);
    setDirty(true);
  }
}
