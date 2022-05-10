package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class aEP_DLEffect implements EveryFrameWeaponEffectPlugin
{
  //总伤害= 2秒充能 * 0.5f * 0.5f * 20发生成量 * 20次每秒生成 * dph
  static final int MAX_NUM = 21;
  static final float MAX_SPREAD = 2;
  static final float FLUX_PER_SEC_CHARGING = 500f;//充能时每秒涨幅能
  IntervalUtil spawnTimer = new IntervalUtil(0.05f, 0.05f);
  float effectLevelLastFrame = 0;

  @Override
  public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    float effectiveLevel = weapon.getChargeLevel();
    weapon.setMaxAmmo(1);
    if (effectiveLevel <= 0 && effectLevelLastFrame <= 0) {
      return;
    }

    //当充能水平开始降低
    if (effectLevelLastFrame >= effectiveLevel) {
      //在未发射子弹的情况下还是开始降低，子弹消失
      if (weapon.getAmmo() >= 1) {
        weapon.setAmmo(0);
      }
      if (effectiveLevel < 0.5f && weapon.getShip() != null) moveDecoWeapon(weapon.getShip(), 0f);
      effectLevelLastFrame = effectiveLevel;
      return;
    }
    else {
      if (weapon.getShip() != null && !engine.isPaused())
        weapon.getShip().getFluxTracker().increaseFlux(FLUX_PER_SEC_CHARGING * amount, false);
      effectLevelLastFrame = effectiveLevel;
    }
    spawnTimer.advance(amount);
    if (!spawnTimer.intervalElapsed()) return;
    int toSpawn = (int) (MathUtils.getRandomNumberInRange(0f, 1f) * MAX_NUM * effectiveLevel);
    //aEP_Tool.addDebugText(toSpawn+"");
    if (weapon.getShip() != null) moveDecoWeapon(weapon.getShip(), 1f);
    for (int i = 0; i < toSpawn; i++) {
      Vector2f loc = aEP_Tool.getExtendedLocationFromPoint(weapon.getFirePoint(0), weapon.getCurrAngle() - 90f, MathUtils.getRandomNumberInRange(-MAX_SPREAD, MAX_SPREAD));
      DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(weapon.getShip(),
        weapon,//from weapon
        "aEP_KF_shred",//weapon id
        loc,//point
        weapon.getCurrAngle(),//angle
        weapon.getShip().getVelocity());//init ship vel
      float speedRand = MathUtils.getRandomNumberInRange(0.75f, 1.25f);
      Vector2f velBefore = new Vector2f(newProj.getVelocity());
      newProj.setFacing(MathUtils.getRandomNumberInRange(0, 360));
      newProj.getVelocity().setY(velBefore.y * speedRand);
      newProj.getVelocity().setX(velBefore.x * speedRand);
    }
  }

  void moveDecoWeapon(ShipAPI ship, float level) {
    for (WeaponAPI w : ship.getAllWeapons()) {
      if (w.getSlot().getId().contains("WS0039")) {
        ((aEP_FCLAnimation) w.getEffectPlugin()).setMoveToLevel(level);
      }
    }
  }
}


