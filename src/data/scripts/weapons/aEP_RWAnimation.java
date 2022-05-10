package data.scripts.weapons;


import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;


public class aEP_RWAnimation implements EveryFrameWeaponEffectPlugin
{
  @Override
  public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    ShipAPI ship = weapon.getShip();
    if (weapon.getSlot().isHidden() || !ship.isAlive()) {
      return;
    }
  }


}
