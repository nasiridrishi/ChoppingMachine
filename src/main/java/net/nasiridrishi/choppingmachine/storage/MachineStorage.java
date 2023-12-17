package net.nasiridrishi.choppingmachine.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.MachineInstance;
import net.nasiridrishi.choppingmachine.utils.LocationUtils;
import org.json.JSONArray;

public class MachineStorage {

  private final File machineDataFile;

  private final ChoppingMachine plugin;
  @Getter
  private Map<String, MachineData> machines;

  private boolean saving = false;

  public MachineStorage(ChoppingMachine plugin) {
    this.plugin = plugin;
    this.machineDataFile = new File(plugin.getDataFolder() + "/machines.json");
    this.loadFromFile();
  }

  /**
   * Performs an async save of the machines data
   */
  public void sync() {
    if (saving) {
      plugin.getLogger()
          .warning("Tried to save machines data while it was already saving. Aborting...");
      return;
    }
    saving = true;
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
      JSONArray jsonArray = new JSONArray();
      if (!machines.isEmpty()) {
        for (MachineData machineData : machines.values()) {
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
      saving = false;
    });
  }

  //Not doing async because its only called once on startup
  private void loadFromFile() {
    if (!machineDataFile.exists()) {
      machines = new HashMap<>();
      plugin.getLogger().info("No machines data file found");
      return;
    }
    try (BufferedReader bufferedReader = new BufferedReader(
        new FileReader(machineDataFile))) {
      StringBuilder stringBuilder = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line);
      }
      machines = fromJsonArray(stringBuilder.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param jsonArrayString: MachineData json array string from file
   * @return
   */
  private Map<String, MachineData> fromJsonArray(String jsonArrayString) {
    Map<String, MachineData> machineDataList = new HashMap<>();
    JSONArray jsonArray = new JSONArray(jsonArrayString);
    for (int i = 0; i < jsonArray.length(); i++) {
      MachineData machineData = MachineData.fromJson(jsonArray.getJSONObject(i));
      machineDataList.put(machineData.getLocation(), machineData);
    }
    return machineDataList;
  }

  public void destroy(MachineInstance machineInstance) {
    destroy(machineInstance, true);
  }

  public void destroy(MachineInstance machineInstance, boolean sync) {
    this.machines.remove(LocationUtils.toString(machineInstance.getLocation()));
    if (sync) {
      sync();
    }
  }

  public void add(MachineInstance machineInstance) {
    if (machines.containsKey(LocationUtils.toString(machineInstance.getLocation()))) {
      throw new RuntimeException("A machine already exists at this location");
    }
    machines.put(LocationUtils.toString(machineInstance.getLocation()),
        MachineData.fromMachineInstance(machineInstance));
    sync();
  }
}
