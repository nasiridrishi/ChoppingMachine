package net.nasiridrishi.choppingmachine.utils;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.nasiridrishi.choppingmachine.machine.MachineInstance;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

public class MachineHologram {

  private final MachineInstance machine;
  private final Hologram hologram;


  public MachineHologram(MachineInstance machineInstance) {
    this.machine = machineInstance;
    hologram = DHAPI.createHologram(machine.getUid(),
        machineInstance.getBlockLc().add(0.5, 2, 0.5));

    DHAPI.addHologramLine(hologram, machine.getType().getMachineItem());
    //in colors
    //owner
    DHAPI.addHologramLine(hologram,
        "&6Owner: &e" + Bukkit.getOfflinePlayer(machine.getOwner()).getName());

    Chest chest = machine.getChest();

    //check if chest is null
    if (chest == null) {
      DHAPI.addHologramLine(hologram, "&cChest not found");
    } else if (chest.getInventory().firstEmpty() == -1) {
      DHAPI.addHologramLine(hologram, "&6Chest Inventory: &cFull");
    } else {
      //check available slots/total slots
      DHAPI.addHologramLine(hologram,
          "&6Chest Inventory: &a" + chest.getInventory().firstEmpty() + "&6/&a"
              + chest.getInventory().getSize());
    }
    //status
    DHAPI.addHologramLine(hologram, "&bStatus: &a" + machine.getStatus());

  }

  public void updateHologramLines() {
    Chest chest = machine.getChest();
    if (machine.getLastChoppedLog() != null && machine.getLastChoppedLog() != Material.AIR) {
      DHAPI.setHologramLine(hologram, 0, new ItemStack(machine.getLastChoppedLog()));
    } else {
      DHAPI.setHologramLine(hologram, 0, machine.getType().getMachineItem());
    }
    //update lines
    //owner
    DHAPI.setHologramLine(hologram, 1,
        "&6Owner: &e" + Bukkit.getOfflinePlayer(machine.getOwner()).getName());
    //check if chest is null
    if (chest == null) {
      DHAPI.setHologramLine(hologram, 2, "&cChest not found");
    } else if (chest.getInventory().firstEmpty() == -1) {
      DHAPI.setHologramLine(hologram, 2, "&6Chest Inventory: &cFull");
    } else {
      //check available slots/total slots
      DHAPI.setHologramLine(hologram, 2,
          "&6Chest Inventory: &a" + chest.getInventory().firstEmpty() + "&6/&a"
              + chest.getInventory().getSize());
    }
    //status
    DHAPI.setHologramLine(hologram, 3, "&bStatus: &a" + machine.getStatus());

    //update location
    hologram.setLocation(machine.getBlockLc().add(0.5, 2, 0.5));

    hologram.realignLines();
  }


  public void destroy() {
    hologram.destroy();
  }
}
