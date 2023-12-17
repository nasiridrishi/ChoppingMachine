package net.nasiridrishi.choppingmachine.machine.types;

import com.github.fierioziy.particlenativeapi.api.particle.type.ParticleType;
import java.util.List;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.MachineConfigs;
import net.nasiridrishi.choppingmachine.machine.MachineInstance;
import net.nasiridrishi.choppingmachine.machine.MachineManager;
import net.nasiridrishi.choppingmachine.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class EmeraldMachine extends BaseMachine {

  @Override
  public String getMachineIdentifier() {
    return MachineConfigs.EMERALD_MACHINE;
  }

  @Override
  protected Material getMachineBlockMaterial() {
    return Material.EMERALD_BLOCK;
  }

  @Override
  public int getTickInterval() {
    return MachineManager.getInstance().getMachineSettings().getEmeraldTickInterval();
  }

  @Override
  public int getSearchRadius() {
    return MachineManager.getInstance().getMachineSettings().getEmeraldSearchRadius();
  }

  @Override
  public int getYieldMultiplier() {
    return MachineManager.getInstance().getMachineSettings().getEmeraldYieldMultiplier();
  }

  @Override
  public ParticleType getLineParticle() {
    return ChoppingMachine.getInstance().getParticleApi().LIST_1_13.DUST.color(0, 128, 0, 1);
  }

  @Override
  protected void onPostChop(MachineInstance instance, Location choppedLog,
      List<Player> nearbyPlayers) {

    for (Player player : nearbyPlayers) {
      //give random 1-3 xp
      player.giveExp(Utils.randomInt(1, 3));
      //play xp sound
      player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    List<Vector> circleVectors = Utils.circularVect(
        instance.getBlockLc().add(0.5, 0, 0.5).toVector(), 1.5);

    for (Vector vector : circleVectors) {
      ChoppingMachine.getInstance().getParticleApi().LIST_1_8.VILLAGER_HAPPY
          .packet(true, vector.toLocation(instance.getBlockLc().getWorld()), 0, 0, 0, 0, 2)
          .sendTo(nearbyPlayers);
    }
  }


}
