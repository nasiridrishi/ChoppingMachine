package net.nasiridrishi.choppingmachine.machine.types;

import com.github.fierioziy.particlenativeapi.api.particle.type.ParticleType;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.MachineConfigs;
import net.nasiridrishi.choppingmachine.machine.MachineManager;
import org.bukkit.Material;

public class GoldMachine extends BaseMachineType {

  @Override
  public String getMachineIdentifier() {
    return MachineConfigs.GOLD_MACHINE;
  }

  @Override
  public Material getMachineBlockMaterial() {
    return Material.GOLD_BLOCK;
  }

  @Override
  public int getTickInterval() {
    return MachineManager.getInstance().getMachineSettings().getGoldTickInterval();
  }

  @Override
  public int getSearchRadius() {
    return MachineManager.getInstance().getMachineSettings().getGoldSearchRadius();
  }

  @Override
  public int getYieldMultiplier() {
    return MachineManager.getInstance().getMachineSettings().getGoldYieldMultiplier();
  }

  @Override
  public ParticleType getLineParticle() {
    return ChoppingMachine.getInstance().getParticleApi().LIST_1_13.DUST.color(255, 215, 0, 1);
  }
}
