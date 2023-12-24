package net.nasiridrishi.choppingmachine.machine.types;

import com.github.fierioziy.particlenativeapi.api.particle.type.ParticleType;
import de.tr7zw.changeme.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.MachineInstance;
import net.nasiridrishi.choppingmachine.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

abstract public class BaseMachineType {

  public abstract String getMachineIdentifier();

  public abstract Material getMachineBlockMaterial();

  //determines the speed of chopping machine
  public abstract int getTickInterval();

  public abstract int getSearchRadius();

  public abstract int getYieldMultiplier();

  public abstract ParticleType getLineParticle();

  /**
   * Optional method to perform machine-specific features
   */
  protected void onPostChop(MachineInstance instance, Location choppedLog,
      List<Player> nearbyPlayers) {
    //NOOP
  }


  /**
   * Returns a machine item with the machine identifier set
   */
  public ItemStack getMachineItem() {
    ItemStack itemStack = new ItemStack(getMachineBlockMaterial());
    NBTItem nbtItem = new NBTItem(itemStack);
    nbtItem.setString("machine", getMachineIdentifier());
    itemStack = nbtItem.getItem();
    ItemMeta itemMeta = itemStack.getItemMeta();
    //set colored name
    itemMeta.setDisplayName("§6" + getMachineIdentifier() + " Machine");
    itemMeta.setLore(new ArrayList<String>() {{
      add("§7");
      add("§7Chops logs in a radius of " + getSearchRadius() + " blocks");
      add("§7Yields " + getYieldMultiplier() + "x logs");
      add("§7Place a chest above the machine to start chopping logs");
      add("§7To pickup machine, simply break placed machine with appropriate tool");
    }});
    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  /**
   * Returns true if the item is a machine item
   */
  public boolean isMachineItem(ItemStack item) {
    NBTItem nbtItem = new NBTItem(item);
    return nbtItem.hasTag("machine") && nbtItem.getString("machine").equals(getMachineIdentifier());
  }


  public void attemptChop(MachineInstance machineInstance, Block block) {
    Block blockToChop = block.getWorld().getBlockAt(block.getLocation());
    if (blockToChop.getType() != block.getType()) {
      return;
    }
    Collection<ItemStack> drops = blockToChop.getDrops();
    for (ItemStack drop : drops) {
      if (isWoodenLog(block.getType())) {
        drop.setAmount(drop.getAmount() * getYieldMultiplier());
        machineInstance.getChoppedLogs().add(drop);
      }
    }
    blockToChop.setType(Material.AIR);

    //at broken log location
    blockToChop.getWorld().playSound(blockToChop.getLocation(),
        blockToChop.getType().createBlockData().getSoundGroup().getBreakSound(), 1, 1);

    List<Player> nearbyPlayers = Utils.getNearbyPlayers(machineInstance,
        getSearchRadius());

    //smoke at broken log location
    ChoppingMachine.getInstance().getParticleApi().LIST_1_8.SMOKE_LARGE
        .packetMotion(true, blockToChop.getLocation().add(0.5, 0.5, 0.5).toVector(), 0D, 0.1D, 0D)
        .sendTo(nearbyPlayers);

    //at machine
    blockToChop.getWorld().playSound(machineInstance.getChest().getLocation(),
        Sound.ENTITY_ITEM_PICKUP, 1, 1);

    //draw green line particle from machine to chopped log
    if (nearbyPlayers.isEmpty()) {
      return;
    }

    List<Vector> lineVectors = Utils.getLine(
        machineInstance.locObj().add(0.5, 0.5, 0.5).toVector(),
        blockToChop.getLocation().add(0.5, 0, 0.5).toVector(), 0.1);

    for (Player player : nearbyPlayers) {
      for (Vector vector : lineVectors) {
        getLineParticle().
            packet(true, vector)
            .sendTo(player);
      }
    }

    onPostChop(machineInstance, blockToChop.getLocation(), nearbyPlayers);
  }


  public boolean isWoodenLog(Material type) {
    return type == Material.ACACIA_LOG
        || type == Material.BIRCH_LOG
        || type == Material.DARK_OAK_LOG
        || type == Material.JUNGLE_LOG
        || type == Material.OAK_LOG
        || type == Material.SPRUCE_LOG;
  }

}
