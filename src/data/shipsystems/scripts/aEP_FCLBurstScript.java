package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import combat.impl.VEs.aEP_MovingSmoke;
import combat.impl.aEP_BaseCombatEffect;
import combat.plugin.aEP_CombatEffectPlugin;
import combat.util.aEP_Tool;
import data.scripts.weapons.aEP_FCLAnimation;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class aEP_FCLBurstScript extends BaseShipSystemScript
{
  public static final float IMPULSE = 25000f;
  public static final float MIN_DAMAGE_PERCENT = 0.1f;
  public static final float MIN_IMPULSE_PERCENT = 0.25f;

  public static final float MAX_GLOW_SIZE = 200f;
  public static final Color GLOW_COLOR = new Color(255, 255, 255, 250);

  public static final float FULL_DAMAGE_RANGE = 250f;
  static final float MAX_SPEED_PERCENT_BONUS = 100f;
  static final float ACC_MULT_PUNISH = 0.35f;
  static final float ON_FIRE_SPEED_MULT_PUNISH = 0.25f;

  IntervalUtil smokeTimer = new IntervalUtil(0.04f, 0.06f);
  private ShipAPI ship;

  public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
    ship = (ShipAPI) stats.getEntity();
    CombatEngineAPI engine = Global.getCombatEngine();
    float amount = Global.getCombatEngine().getElapsedInLastFrame() * stats.getTimeMult().getModifiedValue();

    int weaponNum = 0;
    for (WeaponAPI weapon : ship.getAllWeapons()) {
      if (!weapon.getSlot().getId().contains("FCL_DECO")) continue;
      Vector2f toSpawn = aEP_Tool.getExtendedLocationFromPoint(weapon.getLocation(), weapon.getCurrAngle(), 24f);
      if (weapon.getSpec().getWeaponId().equals("aEP_FCL")) {
        if (ship.getSystem().getEffectLevel() >= 1f) {
          weaponNum += 1;
          CombatEntityAPI pro = engine.spawnProjectile(ship, weapon,
            "aEP_FCL",
            toSpawn,
            weapon.getCurrAngle(),
            ship.getVelocity());
          aEP_CombatEffectPlugin.Mod.addEffect(new Blink((DamagingProjectileAPI) pro));

          engine.addSmoothParticle(toSpawn,//Vector2f loc,
            new Vector2f(0, 0),//Vector2f vel,
            250f,//float size,
            1f,//float brightness,
            0.4f,//float duration,
            new Color(200, 200, 200, 250));//java.awt.Color
          Global.getSoundPlayer().playSound("heavy_mortar_fire",
            1f, 1.2f, // pitch,volume
            ship.getLocation(),//location
            ship.getVelocity());//velocity
        }
      }

      //set other deco to move
      aEP_FCLAnimation anima = (aEP_FCLAnimation) weapon.getEffectPlugin();
      //glow to 1 at instant when fire
      if (weaponNum > 0) {
        if (weapon.getSpec().getWeaponId().equals("aEP_FCL_glow")) anima.setGlowEffectiveLevel(1f);
      }
      //move forward when charging up
      if (state == State.IN) {
        anima.setMoveToLevel(1f);
      }
      //keep still when charging down
      if (state == State.ACTIVE || state == State.OUT) {
        if (effectLevel > 0.5f) {
          smokeTimer.advance(amount);
          if (smokeTimer.intervalElapsed() && weapon.getSpec().getWeaponId().equals("aEP_FCL")) {
            aEP_MovingSmoke ms = new aEP_MovingSmoke(toSpawn);
            ms.setInitVel(aEP_Tool.Util.Speed2Velocity(weapon.getCurrAngle(), 50f));
            ms.setLifeTime(3f);
            ms.setFadeIn(0.17f);
            ms.setFadeOut(0.83f);
            ms.setSize(40);
            ms.setSizeChangeSpeed(10);
            ms.setColor(new Color(120,80,80, MathUtils.getRandomNumberInRange(40, 60)));
            stats.getMaxSpeed().modifyPercent(id, MAX_SPEED_PERCENT_BONUS);
            stats.getAcceleration().modifyMult(id, ACC_MULT_PUNISH);
          }
          if (weapon.getSpec().getWeaponId().equals("aEP_FCL_glow")) anima.setGlowToLevel((effectLevel - 0.5f) * 2f);
        }
        else {
          anima.setMoveToLevel(0f);
          if (weapon.getSpec().getWeaponId().equals("aEP_FCL_glow")) anima.setGlowToLevel(0f);
        }
      }
    }

    //spawn blowback and impulse
    if (weaponNum > 0) {
      ship.getVelocity().set(ship.getVelocity().x * ON_FIRE_SPEED_MULT_PUNISH, ship.getVelocity().y * ON_FIRE_SPEED_MULT_PUNISH);
      aEP_Tool.applyImpulse(ship, ship.getFacing(), -IMPULSE * weaponNum);
    }


    String key = ship.getId() + "_" + id;
    Object test = Global.getCombatEngine().getCustomData().get(key);
    if (state == State.IN) {
      if (test == null && effectLevel > 0.2f) {
        Global.getCombatEngine().getCustomData().put(key, new Object());
        ship.getEngineController().getExtendLengthFraction().advance(1f);
        for (ShipEngineAPI e : ship.getEngineController().getShipEngines()) {
          if (e.isSystemActivated()) {
            ship.getEngineController().setFlameLevel(e.getEngineSlot(), 1f);
          }
        }
      }
    }
    else {
      Global.getCombatEngine().getCustomData().remove(key);
    }
  }

  public void unapply(MutableShipStatsAPI stats, String id) {
    stats.getAcceleration().unmodify(id);
    stats.getMaxSpeed().unmodify(id);
  }

  public StatusData getStatusData(int index, State state, float effectLevel) {

    if (index == 0) {
      return new StatusData("Max Speed Increase" + ": " + MAX_SPEED_PERCENT_BONUS + "%", false);
    }
    return null;
  }

  class Blink extends aEP_BaseCombatEffect
  {
    float maxSize;
    Color glowColor;
    DamagingProjectileAPI s;

    Blink(DamagingProjectileAPI s) {
      this.glowColor = GLOW_COLOR;
      this.maxSize = MAX_GLOW_SIZE;
      this.s = s;
      init(s);
    }

    @Override
    public void advanceImpl(float amount) {
      float effectiveLevel = MathUtils.clamp((MathUtils.getDistance(s.getSpawnLocation(), s.getLocation()) - FULL_DAMAGE_RANGE) / (s.getWeapon().getRange() - FULL_DAMAGE_RANGE), 0f, 1f);
      effectiveLevel = MathUtils.clamp(1f - effectiveLevel, MIN_DAMAGE_PERCENT, 1f);
      float impulseEffectiveLevel = MathUtils.clamp(1f - effectiveLevel, MIN_DAMAGE_PERCENT, 1f);

      s.setDamageAmount(s.getProjectileSpec().getDamage().getDamage() * effectiveLevel);
      if (s.didDamage() && s.getDamageTarget() != null) {
        aEP_Tool.Util.applyImpulse(s.getDamageTarget(), s.getLocation(), s.getFacing(), IMPULSE * impulseEffectiveLevel);
        cleanup();
      }


      Global.getCombatEngine().addSmoothParticle(s.getLocation(),
              new Vector2f(0f, 0f),
              maxSize * effectiveLevel,//size
              1f,//brightness
              amount * 2,//duration
              new Color(GLOW_COLOR.getRed(), GLOW_COLOR.getGreen(), GLOW_COLOR.getBlue(), GLOW_COLOR.getAlpha()));
      //aEP_Tool.addDebugText(effectiveLevel+"");

    }

  }
}
