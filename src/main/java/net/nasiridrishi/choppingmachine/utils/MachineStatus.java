package net.nasiridrishi.choppingmachine.utils;

import lombok.Getter;

@Getter
public enum MachineStatus {

  IDLE("§7Idle"),
  CHOPPING("§6Chopping"),
  ERROR("§cError"),
  CHEST_FULL("§cChest Full"),
  CHEST_MISSING("§cChest Missing"),
  SEARCHING_TREE("§eSearching Tree"),
  CHOPPING_TREE("§6Chopping Tree"),
  MOVING_TO_TREE("§aMoving to Tree");
  

  private final String status;

  MachineStatus(String status) {
    this.status = status;
  }

  public static MachineStatus fromString(String status) {
    for (MachineStatus machineStatus : MachineStatus.values()) {
      if (machineStatus.getStatus().equalsIgnoreCase(status)) {
        return machineStatus;
      }
    }
    return null;
  }

}
