package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import static data.shipsystems.scripts.aEP_FCLBurstScript.FULL_DAMAGE_RANGE;

public class aEP_FCLBurstAI implements ShipSystemAIScript
{
  CombatEngineAPI engine;
  ShipSystemAPI system;
  ShipAPI ship;
  ShipwideAIFlags flags;
  IntervalUtil think = new IntervalUtil(0.2f, 0.3f);
  float willing = 0;

  @Override
  public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
    this.ship = ship;
    this.system = system;
    this.engine = engine;
    this.flags = flags;
  }


  @Override
  public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    if (engine.isPaused() || target == null || target.getAIFlags() == null || target.isFighter()) {
      return;
    }

    think.advance(amount);
    willing = 0;
    if (!think.intervalElapsed()) return;

    float fclGunRange = 0;
    for (WeaponAPI w : ship.getAllWeapons()) {
      if (w.getSpec().getWeaponId().equals("aEP_FCL")) {
        fclGunRange = w.getRange();
        break;
      }
    }


    float maxWillingRange = 1000f;
    float willingRange = maxWillingRange - FULL_DAMAGE_RANGE;
    Vector2f hitPoint = CollisionUtils.getCollisionPoint(ship.getLocation(), aEP_Tool.getExtendedLocationFromPoint(ship.getLocation(), ship.getFacing(), maxWillingRange), target);
    if (hitPoint != null) {
      float dist = MathUtils.getDistance(hitPoint, ship.getLocation());
      willing += 60f * (Math.max(maxWillingRange - (dist - FULL_DAMAGE_RANGE) * (maxWillingRange / willingRange), 1f)) / maxWillingRange;
    }

    if (flags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)) willing += 10f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF)) willing += 35f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE)) willing += 10f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACK_OFF)) willing += 25f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.IN_CRITICAL_DPS_DANGER)) willing += 30f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_AUTOFIRE_NON_ESSENTIAL_GROUPS)) willing += 15f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_FLUX)) willing += 30f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACK_OFF_MIN_RANGE)) willing -= 40f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.MAINTAINING_STRIKE_RANGE)) willing -= 60f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.PURSUING)) willing -= 70f;
    if (flags.hasFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET)) willing -= 25f;
    //aEP_Tool.addDebugText(willing+"");
    willing += MathUtils.getRandomNumberInRange(0f, 50f);
    if (willing >= 100f && hitPoint != null) {
      ship.useSystem();
    }
  }

}