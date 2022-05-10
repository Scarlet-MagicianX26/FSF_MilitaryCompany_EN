package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import combat.util.aEP_DataTool;
import data.scripts.weapons.aEP_FCLAnimation;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;


public class aEP_RLDamper extends BaseShipSystemScript
{
  public static final float INCOMING_DAMAGE_CAPITAL = 0.5f;
  private static final float EFFECT_ARMOR_FLAT_BONUS = 800f;
  private static final float EFFECT_ARMOR_PERCENT_BONUS = 0.5f;
  private static final float ARMOR_DAMAGE_REDUCE = 0.75f;//by mult
  private ShipAPI ship;
  private final String id = "aEP_RLDamper";

  @Override
  public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
    ship = (ShipAPI) stats.getEntity();
    float convertedLevel = effectLevel;
    if (state == State.ACTIVE) convertedLevel = 1f;
    for (WeaponAPI w : ship.getAllWeapons()) {
      if (!w.getSlot().getId().contains("RL_DECO")) continue;
      aEP_FCLAnimation anima = (aEP_FCLAnimation) w.getEffectPlugin();
      //extend out
      if (convertedLevel < 0.5f) {
        float level = MathUtils.clamp(convertedLevel * 2f, 0f, 1f);
        if (w.getSpec().getWeaponId().equals("aEP_raoliu_armor") || w.getSpec().getWeaponId().equals("aEP_shangshengliu_armor")) {
          w.getSprite().setColor(new Color(0, 0, 0, 0));
          anima.setMoveToLevel(level);
        }
        if (w.getSpec().getWeaponId().equals("aEP_raoliu_armor_dark") || w.getSpec().getWeaponId().equals("aEP_shangshengliu_armor_dark")) {
          w.getSprite().setColor(new Color(255, 255, 255));
          anima.setMoveToLevel(level);
        }
        if (w.getSpec().getWeaponId().equals("aEP_raoliu_hull")) {
          anima.setMoveToLevel(level);
        }
      }
      //pull back and exchange renderOrder
      else {
        float level = MathUtils.clamp(2f - convertedLevel * 2f, 0f, 1f);
        if (w.getSpec().getWeaponId().equals("aEP_raoliu_armor") || w.getSpec().getWeaponId().equals("aEP_shangshengliu_armor")) {
          int black = (int) (255 * effectLevel);
          w.getSprite().setColor(new Color(black, black, black));
          anima.setMoveToLevel(level);
        }
        if (w.getSpec().getWeaponId().equals("aEP_raoliu_armor_dark") || w.getSpec().getWeaponId().equals("aEP_shangshengliu_armor_dark")) {
          anima.setMoveToLevel(level);
        }
      }
      if (w.getSpec().getWeaponId().equals("aEP_raoliu_bridge")) anima.setMoveToLevel(effectLevel);
    }

    //modify here
    float toAdd = EFFECT_ARMOR_FLAT_BONUS + ship.getHullSpec().getArmorRating() * EFFECT_ARMOR_PERCENT_BONUS;
    stats.getEffectiveArmorBonus().modifyFlat(id, toAdd * effectLevel);
    stats.getArmorDamageTakenMult().modifyMult(id, ARMOR_DAMAGE_REDUCE * effectLevel);
    //ship.getExactBounds().getSegments()
  }

  @Override
  public void unapply(MutableShipStatsAPI stats, String id) {
    ship = (ShipAPI) stats.getEntity();
    stats.getEffectiveArmorBonus().unmodify(id);
    stats.getArmorDamageTakenMult().unmodify(id);

    for (WeaponAPI w : ship.getAllWeapons()) {
      if (!w.getSlot().getId().contains("RL_DECO")) continue;
      aEP_FCLAnimation anima = (aEP_FCLAnimation) w.getEffectPlugin();
      if (w.getSpec().getWeaponId().equals("aEP_raoliu_armor") || w.getSpec().getWeaponId().equals("aEP_shangshengliu_armor")) {
        w.getSprite().setColor(new Color(0, 0, 0, 0));
        anima.setMoveToLevel(0);
      }
      if (w.getSpec().getWeaponId().equals("aEP_raoliu_armor_dark") || w.getSpec().getWeaponId().equals("aEP_shangshengliu_armor_dark")) {
        w.getSprite().setColor(new Color(0, 0, 0, 0));
        anima.setMoveToLevel(0);
      }
      if (w.getSpec().getWeaponId().equals("aEP_raoliu_hull")) {
        anima.setMoveToLevel(0);
      }
      if (w.getSpec().getWeaponId().equals("aEP_raoliu_bridge")) anima.setMoveToLevel(0f);
    }
  }

  @Override
  public StatusData getStatusData(int index, State state, float effectLevel) {
    if (index == 0) {
      float toAdd = EFFECT_ARMOR_FLAT_BONUS + ship.getHullSpec().getArmorRating() * EFFECT_ARMOR_PERCENT_BONUS;
      return new StatusData(aEP_DataTool.txt("aEP_LADamper01") + (int) (toAdd * effectLevel), false);
    }
    return null;
  }


}