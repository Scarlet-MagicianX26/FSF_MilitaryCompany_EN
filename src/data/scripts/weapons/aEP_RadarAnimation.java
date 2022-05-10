package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class aEP_RadarAnimation implements EveryFrameWeaponEffectPlugin
{
  static final float ROTATE_RATE = 5f;

  @Override
  public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    if (weapon.getShip() == null) return;
    float toAngle = VectorUtils.getAngle(weapon.getLocation(), weapon.getShip().getMouseTarget());
    float angleDist = MathUtils.getShortestRotation(weapon.getCurrAngle(), toAngle);
    if (angleDist > 0) {
      weapon.setCurrAngle(weapon.getCurrAngle() + Math.min(angleDist, ROTATE_RATE * amount));
    }
    else {
      weapon.setCurrAngle(weapon.getCurrAngle() - Math.min(-angleDist, ROTATE_RATE * amount));
    }
  }


}
