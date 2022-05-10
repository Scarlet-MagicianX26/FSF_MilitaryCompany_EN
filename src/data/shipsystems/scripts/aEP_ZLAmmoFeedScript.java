package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import combat.impl.VEs.aEP_MovingSmoke;
import combat.plugin.aEP_CombatEffectPlugin;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class aEP_ZLAmmoFeedScript extends BaseShipSystemScript
{

  public static final float FLUX_REDUCTION = 0f;//by percent
  private static final float ROF_BONUS = 200f;//extra percent
  private static final float RANGE_BONUS = 450f;
  private static final float PD_RANGE_BONUS = 0f;
  private static final float PROJ_SPEED_BONUS = 10f;
  private static final float RECOIL_DECAY_BONUS = 25f;
  private static final float MAX_SPREAD_BONUS = 25f;
  //about visual effect
  private static final float EMP_ARC_CHANCE_PER_SECOND = 15f;
  private static final Color ARC_FRINGE_COLOR = new Color(240, 100, 100, 240);
  private static final Color ARC_CORE_COLOR = new Color(200, 200, 200, 120);

  private static final Color JITTER_COLOR = new Color(240, 50, 50, 125);

  private static final Map<WeaponAPI.WeaponSize, Float> spreadRange = new HashMap<>();
  private static final Map<WeaponAPI.WeaponSize, Float> timeRange = new HashMap<>();

  static {
    spreadRange.put(WeaponAPI.WeaponSize.LARGE, 10f);
    spreadRange.put(WeaponAPI.WeaponSize.MEDIUM, 7.5f);
    spreadRange.put(WeaponAPI.WeaponSize.SMALL, 5f);
  }

  static {
    timeRange.put(WeaponAPI.WeaponSize.LARGE, 0.5f);
    timeRange.put(WeaponAPI.WeaponSize.MEDIUM, 0.4f);
    timeRange.put(WeaponAPI.WeaponSize.SMALL, 0.3f);
  }


  boolean didActive = false;

  public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
    ShipAPI ship = (ShipAPI) stats.getEntity();

    if (!ship.isAlive()) {
      return;
    }

    ship.setJitterShields(false);
    ship.setJitter(ship, JITTER_COLOR, effectLevel, 1, 5f);
    ship.setJitterUnder(ship, JITTER_COLOR, effectLevel, 8, 20f);

    for (WeaponAPI w : ship.getAllWeapons()) {
      if (aEP_Tool.isNormalWeaponSlotType(w.getSlot(), false) && Math.random() < EMP_ARC_CHANCE_PER_SECOND * aEP_Tool.getAmount(null)) {
        for (Vector2f offset : aEP_Tool.Util.getWeaponOffsetInAbsoluteCoo(w)) {
          Vector2f vel = aEP_Tool.Util.Speed2Velocity(w.getCurrAngle(), MathUtils.getRandomNumberInRange(20, 120));
          Vector2f shipVel = ship.getVelocity();
          Global.getCombatEngine().addSmoothParticle(aEP_Tool.getExtendedLocationFromPoint(offset, w.getCurrAngle() + 90f, MathUtils.getRandomNumberInRange(-spreadRange.get(w.getSize()), spreadRange.get(w.getSize()))),//loc
            new Vector2f(vel.getX() + shipVel.getX(), vel.getY() + shipVel.getY()),//vel
            MathUtils.getRandomNumberInRange(spreadRange.get(w.getSize()) / 4f, spreadRange.get(w.getSize())),//size
            MathUtils.getRandomNumberInRange(0.7f, 1f),//brightness
            timeRange.get(w.getSize()),//duration
            ARC_FRINGE_COLOR);
        }
      }
    }

    //ballistic weapon buff
    stats.getBallisticRoFMult().modifyPercent(id, ROF_BONUS * effectLevel);
    stats.getBallisticAmmoRegenMult().modifyPercent(id,ROF_BONUS * effectLevel);

    //energy weapon buff
    stats.getEnergyRoFMult().modifyPercent(id, ROF_BONUS * effectLevel);
    stats.getEnergyAmmoRegenMult().modifyPercent(id,ROF_BONUS * effectLevel);

    //non beam PD buff
    //stats.getNonBeamPDWeaponRangeBonus().modifyFlat(id,PD_RANGE_BONUS - RANGE_BONUS);

    //flux consume reduce
    stats.getBallisticWeaponFluxCostMod().modifyPercent(id, -FLUX_REDUCTION);
    stats.getEnergyWeaponRangeBonus().modifyPercent(id, -FLUX_REDUCTION);

    //proj speed buff
    stats.getProjectileSpeedMult().modifyPercent(id, PROJ_SPEED_BONUS);

    //spread buff
    stats.getRecoilDecayMult().modifyPercent(id, RECOIL_DECAY_BONUS);
    stats.getMaxRecoilMult().modifyPercent(id, -MAX_SPREAD_BONUS);

    //VEs.
    didActive = true;

  }


  public void unapply(MutableShipStatsAPI stats, String id) {
    ShipAPI ship = (ShipAPI) stats.getEntity();

    //ballistic weapon buff
    stats.getBallisticRoFMult().unmodify(id);
    stats.getBallisticAmmoRegenMult().unmodify(id);


    //energy weapon buff
    stats.getEnergyRoFMult().unmodify(id);
    stats.getEnergyAmmoRegenMult().unmodify(id);


    //non beam PD buff
    //stats.getNonBeamPDWeaponRangeBonus().modifyFlat(id,0);


    //flux consume reduce
    stats.getBallisticWeaponFluxCostMod().unmodify(id);
    stats.getEnergyWeaponRangeBonus().unmodify(id);

    //proj speed buff
    stats.getProjectileSpeedMult().unmodify(id);

    //spread buff
    stats.getRecoilDecayMult().unmodify(id);
    stats.getMaxRecoilMult().unmodify(id);

    if (didActive) spawnSmoke(ship, 30);
    didActive = false;
  }

  @Override
  public StatusData getStatusData(int index, State state, float effectLevel) {
    if (index == 0) {
      return new StatusData("Rate of Fire increased" + ": " + (int) (effectLevel * ROF_BONUS) + "%", false);
    }

    return null;
  }

  void spawnSmoke(ShipAPI ship, int minSmokeDist) {
    float moveAngle = 0;
    float angleToTurn = aEP_Tool.getTargetWidthAngleInDistance(ship.getLocation(), aEP_Tool.getExtendedLocationFromPoint(ship.getLocation(), 0f, ship.getCollisionRadius()), minSmokeDist);
    while (moveAngle < 360f) {
      Vector2f outPoint = CollisionUtils.getCollisionPoint(aEP_Tool.getExtendedLocationFromPoint(ship.getLocation(), moveAngle, ship.getCollisionRadius() + 10), ship.getLocation(), ship);
      float lifeTime = 2f;
      float extendRange = 0.5f;
      Vector2f speed = aEP_Tool.Util.Speed2Velocity(VectorUtils.getAngle(ship.getLocation(), outPoint), extendRange * ship.getCollisionRadius());
      aEP_MovingSmoke ms = new aEP_MovingSmoke(outPoint);
      ms.setLifeTime(lifeTime);
      ms.setFadeIn(0.25f);
      ms.setFadeOut(0.5f);
      ms.setInitVel(speed);
      ms.setSize(minSmokeDist * 3f);
      ms.setSizeChangeSpeed(minSmokeDist * extendRange * 3f / lifeTime);
      ms.setColor(new Color(200,200,200,80));
      ms.setStopSpeed(0.75f);
      aEP_CombatEffectPlugin.Mod.addVE(ms);
      moveAngle = moveAngle + angleToTurn;
    }
    moveAngle = 0f;
    while (moveAngle < 360f) {
      Vector2f outPoint = CollisionUtils.getCollisionPoint(aEP_Tool.getExtendedLocationFromPoint(ship.getLocation(), moveAngle, ship.getCollisionRadius() + 10), ship.getLocation(), ship);
      float lifeTime = 2f;
      float extendRange = 0.5f;
      Vector2f speed = aEP_Tool.Util.Speed2Velocity(VectorUtils.getAngle(ship.getLocation(), outPoint), extendRange * ship.getCollisionRadius() + minSmokeDist * 6f);

      aEP_MovingSmoke ms = new aEP_MovingSmoke(outPoint);
      ms.setLifeTime(lifeTime);
      ms.setFadeIn(0.25f);
      ms.setFadeOut(0.5f);
      ms.setInitVel(speed);
      ms.setSize(minSmokeDist * 6f);
      ms.setSizeChangeSpeed(minSmokeDist * extendRange * 6f / lifeTime);
      ms.setColor(new Color(200,200,200,80));
      ms.setStopSpeed(0.75f);
      aEP_CombatEffectPlugin.Mod.addVE(ms);

      moveAngle = moveAngle + angleToTurn;
    }

  }
}
