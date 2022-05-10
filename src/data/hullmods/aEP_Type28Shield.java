package data.hullmods;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class aEP_Type28Shield extends BaseHullMod
{

  private static final int COLOR_SHIFT_VALUE = 200;
  private static final float MIN_EMP_ARC_INTERVAL_PER_SEC = 0.25f;
  private static String id = "aEP_Type28Shield";

  @Override
  public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
    aEP_Type28Shield.id = id;
    stats.getHardFluxDissipationFraction().modifyPercent(id, 20f);

  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    float fluxLevel = ship.getFluxLevel();
    if (ship.getShield() == null || ship.getShield().getType() == ShieldAPI.ShieldType.NONE) {
      return;
    }

    Color shieldColor = new Color((int) (COLOR_SHIFT_VALUE * fluxLevel) + 250 - COLOR_SHIFT_VALUE, 50, (int) (COLOR_SHIFT_VALUE * (1f - fluxLevel)) + 250 - COLOR_SHIFT_VALUE, 120);
    ship.getShield().setInnerColor(shieldColor);

    if (fluxLevel > 0.5f) {
      ship.setCircularJitter(true);
      ship.setJitterShields(true);
      ship.setJitter(ship,
        shieldColor,
        20f,//range
        3,//copies
        1f * (fluxLevel - 0.5f) * 2f);

      if (MathUtils.getRandomNumberInRange(0, 100) < (fluxLevel - 0.5f) * 2f * 100f * amount * MIN_EMP_ARC_INTERVAL_PER_SEC) {
        Vector2f from = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getShield().getRadius() / 2f);
        Vector2f to = aEP_Tool.getExtendedLocationFromPoint(ship.getLocation(), MathUtils.getRandomNumberInRange(0, 360), ship.getShield().getRadius());
        Global.getCombatEngine().spawnEmpArcVisual(from,
          ship,
          to,
          null,
          MathUtils.getRandomNumberInRange(2f, 6f),
          new Color(100, 100, 200, 80),
          new Color(200, 200, 250, 200));
      }

    }

    if (ship.getParentStation() == null) {
      return;
    }


    for (ShipAPI modular : ship.getParentStation().getChildModulesCopy()) {

      if (ship.getParentStation().getEngineController().isAccelerating()) {
        modular.giveCommand(ShipCommand.ACCELERATE, null, 0);
      }
      else if (ship.getParentStation().getEngineController().isTurningRight()) {
        if (modular.getHullSpec().getHullId().equals("aEP_typeL28")) {
          modular.giveCommand(ShipCommand.ACCELERATE, null, 0);
        }
      }
      else if (ship.getParentStation().getEngineController().isTurningLeft()) {
        if (modular.getHullSpec().getHullId().equals("aEP_typeR28")) {
          modular.giveCommand(ShipCommand.ACCELERATE, null, 0);
        }
      }
    }

  }


  @Override
  public String getDescriptionParam(int index, HullSize hullSize) {
    return null;
  }


  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    return false;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    return "无法安装任何插件";
  }

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    List<String> toRomvelist = new ArrayList<String>();
    for (String hullmodId : ship.getVariant().getHullMods()) {
      if (!ship.getVariant().getPermaMods().contains(hullmodId)) {
        toRomvelist.add(hullmodId);
      }
    }

    for (String toRemove : toRomvelist) {
      ship.getVariant().removeMod(toRemove);
    }
  }

}
