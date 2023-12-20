package net.nasiridrishi.choppingmachine.machine.types;

import com.github.fierioziy.particlenativeapi.api.particle.type.ParticleType;
import net.nasiridrishi.choppingmachine.ChoppingMachine;
import net.nasiridrishi.choppingmachine.machine.MachineConfigs;
import net.nasiridrishi.choppingmachine.machine.MachineManager;
import org.bukkit.Material;

public class DiamondMachine extends BaseMachine {

  @Override
  public String getMachineIdentifier() {
    return MachineConfigs.DIAMOND_MACHINE;
  }

  @Override
  public Material getMachineBlockMaterial() {
    return Material.DIAMOND_BLOCK;
  }

  @Override
  public int getTickInterval() {
    return MachineManager.getInstance().getMachineSettings().getDiamondTickInterval();
  }

  @Override
  public int getSearchRadius() {
    return MachineManager.getInstance().getMachineSettings().getDiamondSearchRadius();
  }

  @Override
  public int getYieldMultiplier() {
    return MachineManager.getInstance().getMachineSettings().getDiamondYieldMultiplier();
  }

  @Override
  public ParticleType getLineParticle() {
    //aqua color
    return ChoppingMachine.getInstance().getParticleApi().LIST_1_13.DUST.color(0, 255, 255, 1);
  }

}
