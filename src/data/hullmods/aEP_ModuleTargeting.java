package data.hullmods;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class aEP_ModuleTargeting extends aEP_BaseHullMod
{


  //百分比加成
  static final Map<String, Float[]> mag = new HashMap<>();
  static {
    mag.put("aEP_HaiLiang2", new Float[]{40f});
    mag.put("aEP_NeiBo_TR", new Float[]{60f});
  }


  public aEP_ModuleTargeting() {
    notCompatibleList.add("safetyoverrides");
    notCompatibleList.add("converted_hangar");
    notCompatibleList.add("dedicated_targeting_core");
    notCompatibleList.add("targetingunit");
  }

  /**
   * 使用这个
   *
   * @param ship
   * @param id
   */
  @Override
  public void applyEffectsAfterShipCreationImpl(ShipAPI ship, String id) {
    ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, mag.get(ship.getHullSpec().getHullId())[0]);
    ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(id, mag.get(ship.getHullSpec().getHullId())[0]);
    ship.getMutableStats().getVentRateMult().modifyMult(id, 0);
  }

  @Override
  public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
    if (index == 0) return String.format("%.0f", mag.get(ship.getHullSpec().getHullId())[0]) + "%";
    return "";
  }


}
