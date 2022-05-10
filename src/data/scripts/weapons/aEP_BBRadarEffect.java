package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;
import combat.impl.aEP_Buff;
import combat.plugin.aEP_BuffEffect;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

import java.util.HashMap;
import java.util.Map;

public class aEP_BBRadarEffect implements EveryFrameWeaponEffectPlugin
{

  public static final String id = "aEP_BBRadarEffect";
  public static final Map<WeaponAPI.WeaponSize, Float> BONUS_PERCENT = new HashMap<>();
  static {
    BONUS_PERCENT.put(WeaponAPI.WeaponSize.LARGE, 0.85f);
    BONUS_PERCENT.put(WeaponAPI.WeaponSize.MEDIUM, 0.85f);
    BONUS_PERCENT.put(WeaponAPI.WeaponSize.SMALL, 0.85f);
  }

  static final float DAMAGE_INCREASE_PERCENT = 10f;

  public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    if(weapon.getShip() != null){
      if(!weapon.getShip().hasListenerOfClass(BB_radar.class) ){
        weapon.getShip().addListener(new BB_radar());
      }
    }
  }

  static class BB_radar implements WeaponRangeModifier {
    CombatEntityAPI hittingTarget;
    WeaponAPI radar;

    @Override
    public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
      return 0f;
    }

    @Override
    public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
      if (weapon.getSpec().getWeaponId().equals("aEP_BB_radar")) {
        radar = weapon;
        hittingTarget = null;
        for (BeamAPI beam : Global.getCombatEngine().getBeams()) {
          if (beam.getWeapon() == weapon) {
            hittingTarget = beam.getDamageTarget();
            if(hittingTarget instanceof ShipAPI){
              aEP_BuffEffect.addThisBuff(hittingTarget,new Buff((ShipAPI) hittingTarget));
            }
          }
        }
      }
      if(radar == null) return 0f;

      //无论雷达是否开火，朝向雷达方向的非PD武器获得射程加成
      if(weapon.hasAIHint(WeaponAPI.AIHints.PD)) return 0f;
      if (aEP_Tool.isNormalWeaponType(weapon, false)) {
        float buffPercent = 0f;
        float angleDist = Math.abs(MathUtils.getShortestRotation(radar.getCurrAngle(), weapon.getCurrAngle()));
        buffPercent = MathUtils.clamp(1 - ((angleDist - 15f) / 90f), 0f, 1f);
        return BONUS_PERCENT.get(weapon.getSize()) * buffPercent;
      }

      return 0f;
    }

    @Override
    public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
      return 1f;
    }
  }

  static class Buff extends aEP_Buff {
    ShipAPI target;
    Buff(ShipAPI target){
      setBuffType("aEP_BBRadarEffect");
      setEntity(target);
      setLifeTime(0.5f);
      setMaxStack(1f);
      setStackNum(1f);
      setRenew(true);
      this.target = target;
    }
    @Override
    public void play() {
      target.getMutableStats().getEnergyDamageTakenMult().modifyPercent(getBuffType(),DAMAGE_INCREASE_PERCENT);
    }

    @Override
    public void readyToEnd() {
      target.getMutableStats().getEnergyDamageTakenMult().unmodify(getBuffType());
    }
  }

}