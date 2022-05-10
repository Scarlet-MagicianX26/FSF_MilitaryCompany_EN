//by a111164
package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;


public class aEP_aa_missileAI implements GuidedMissileAI, MissileAIPlugin
{

  private static final float ROUGH_SEARCH_RANGE = 2000f;
  private static final float DEDICATE_AIM_RANGE = 250f;
  private static final float ROTATE_AROUND_DIST = 120f;
  private static final float ENGINE_START_TIME = 0.5f;
  private CombatEntityAPI target;
  private Vector2f targetPo = new Vector2f(0f, 0f);
  private final ShipAPI lunchShip;
  private final MissileAPI missile;
  private float timer = 0f;
  private final float amount = 0f;
  private STATUS status = new SEARCHING();

  public aEP_aa_missileAI(MissileAPI missile, ShipAPI lunchShip) {
    this.missile = missile;
    this.lunchShip = lunchShip;
  }

  @Override
  public CombatEntityAPI getTarget() {
    return target;
  }

  @Override
  public void setTarget(CombatEntityAPI newTarget) {
    this.target = newTarget;
  }

  @Override
  public void advance(float amount) {
    if (missile == null || lunchShip == null) {
      return;
    }

    timer = timer + amount;

    //init no searching time
    if (timer < ENGINE_START_TIME) {
      missile.giveCommand(ShipCommand.ACCELERATE);
      return;
    }

    //ready to end check
    if (timer > missile.getMaxFlightTime() && !(status instanceof READY_TO_END)) {
      //Global.getCombatEngine().addFloatingText(missile.getLocation(), "search", 20f ,new Color (0, 100, 200, 240),missile, 0.25f, 120f);
      status = new READY_TO_END();
      return;
    }

    // null check
    if (target == null && !(status instanceof SEARCHING)) {
      status = new SEARCHING();
    }


    status.advance(amount);

  }

  private CombatEntityAPI findNearestTarget() {
    CombatEntityAPI target = null;


    //find the nearest fighter
    float dist = 0f;
    float minDist = ROUGH_SEARCH_RANGE;
    for (ShipAPI s : AIUtils.getNearbyEnemies(missile, ROUGH_SEARCH_RANGE)) {
      if (s.isFighter()) {
        dist = MathUtils.getDistance(s, missile);
        float turnTime = aEP_Tool.getTimeNeedToTurn(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), s.getLocation()), missile.getAngularVelocity(), missile.getAcceleration(), missile.getAcceleration(), missile.getMaxTurnRate());
        float fiyTime = dist / missile.getMaxSpeed();
        if (turnTime < fiyTime && dist < minDist) {
          minDist = dist;
          target = s;
        }
      }
    }

    return target;
  }

  private interface STATUS
  {
    void advance(float amount);
  }

  private class SEARCHING implements STATUS
  {
    float angle = 360f * (float) Math.random();
    float timer = 0f;

    @Override
    public void advance(float amount) {
      //if we have target change to flying mod
      if (target != null && target != lunchShip) {
        status = new SEARCHING_WHILE_TO_TARGET();
        return;
      }


      //rotate around lunch ship
      float angleDist = (missile.getMaxSpeed() * 1.1f * amount) / (lunchShip.getCollisionRadius() + ROTATE_AROUND_DIST * 2f * 3.14f) * 360f;
      float newAngle = aEP_Tool.angleAdd(angle, angleDist);
      Vector2f targetPo = aEP_Tool.getExtendedLocationFromPoint(lunchShip.getLocation(), newAngle, lunchShip.getCollisionRadius() + ROTATE_AROUND_DIST);
      aEP_Tool.Util.flyToPosition(missile, targetPo);

      //search target per 0.5 second
      timer = timer + amount;
      if (timer > 0.25f) {
        timer = 0f;
        target = findNearestTarget();
      }

    }

  }

  private class SEARCHING_WHILE_TO_TARGET implements STATUS
  {

    float timer = 0f;

    @Override
    public void advance(float amount) {

      //if target is null, return to searching mod
      if (target instanceof MissileAPI) {

        if (target == null || !Global.getCombatEngine().isEntityInPlay(target)) {
          target = null;
          status = new SEARCHING();
          return;
        }

      }

      if (target instanceof ShipAPI) {
        if (!((ShipAPI) target).isAlive() || !Global.getCombatEngine().isEntityInPlay(target)) {
          target = null;
          status = new SEARCHING();
          return;
        }

        //if too close, change to dedicate fly mod
        if (MathUtils.getDistance(missile.getLocation(), target.getLocation()) < DEDICATE_AIM_RANGE) {
          status = new STRAIGHT_TO_TARGET();
          return;
        }
      }


      targetPo = AIUtils.getBestInterceptPoint(missile.getLocation(), missile.getMoveSpeed(), target.getLocation(), target.getVelocity());
      if (targetPo == null) {
        targetPo = target.getLocation();
      }

      if (Math.abs(MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), targetPo))) < 90f) {
        missile.giveCommand(ShipCommand.ACCELERATE);
      }
      else {
        missile.giveCommand(ShipCommand.DECELERATE);
      }
      aEP_Tool.Util.moveToAngle(missile, VectorUtils.getAngle(missile.getLocation(), targetPo));

      //search target per 0.2 second
      timer = timer + amount;
      if (timer > 0.2f) {
        timer = 0f;
        target = findNearestTarget();
      }


    }

  }

  private class STRAIGHT_TO_TARGET implements STATUS
  {

    float timer = 0f;

    @Override
    public void advance(float amount) {

      //if target is null, return to searching mod
      if (target instanceof MissileAPI) {

        if (target == null || ((MissileAPI) target).getFlightTime() > ((MissileAPI) target).getMaxFlightTime() || ((MissileAPI) target).didDamage() || target.getHullLevel() <= 0f) {
          target = null;
          status = new SEARCHING();
          return;
        }

      }

      if (target instanceof ShipAPI) {
        if (!((ShipAPI) target).isAlive()) {
          target = null;
          status = new SEARCHING();
          return;
        }
      }


      targetPo = AIUtils.getBestInterceptPoint(missile.getLocation(), missile.getMoveSpeed(), target.getLocation(), target.getVelocity());
      if (targetPo == null) {
        targetPo = target.getLocation();
      }

      if (Math.abs(MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), targetPo))) < 90f) {
        missile.giveCommand(ShipCommand.ACCELERATE);
      }
      else {
        missile.giveCommand(ShipCommand.DECELERATE);
      }
      aEP_Tool.Util.moveToAngle(missile, VectorUtils.getAngle(missile.getLocation(), targetPo));

    }

  }

  private class READY_TO_END implements STATUS
  {
    @Override
    public void advance(float amount) {

    }
  }
}