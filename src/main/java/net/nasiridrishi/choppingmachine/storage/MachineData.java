package net.nasiridrishi.choppingmachine.storage;

import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import net.nasiridrishi.choppingmachine.machine.MachineInstance;
import net.nasiridrishi.choppingmachine.machine.MachineManager;
import net.nasiridrishi.choppingmachine.utils.LocationUtils;
import org.bukkit.Location;
import org.json.JSONObject;

@Getter
public class MachineData {

  private final String type;
  private final UUID owner;
  private String location;

  public MachineData(@NonNull String type, @NonNull UUID $owner, @NonNull String location) {
    this.type = type;
    this.owner = $owner;
    this.location = location;
  }

  public boolean isWorldLoaded() {
    return getLocationObj() != null;
  }

  public Location getLocationObj() {
    try {
      return LocationUtils.fromString(location);
    } catch (Exception e) {
      return null;
    }
  }

  public void setLocation(Location location) {
    this.location = LocationUtils.toString(location);
  }

  public static MachineData fromMachineInstance(MachineInstance machineInstance) {
    return new MachineData(machineInstance.getType().getMachineIdentifier(),
        machineInstance.getOwner(),
        LocationUtils.toString(machineInstance.getLocation()));
  }

  public static MachineData fromJson(JSONObject jsonObject) {
    return new MachineData(jsonObject.getString("type"),
        UUID.fromString(jsonObject.getString("owner")),
        jsonObject.getString("location"));
  }

  public MachineInstance instantiate() {
    return new MachineInstance(MachineManager.getInstance().getMachineType(type),
        LocationUtils.fromString(location), owner);
  }

  public JSONObject toJson() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("type", type);
    jsonObject.put("owner", owner.toString());
    jsonObject.put("location", location);
    return jsonObject;
  }


}
