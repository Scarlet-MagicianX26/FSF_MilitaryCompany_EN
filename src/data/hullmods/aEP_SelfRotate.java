package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import combat.util.aEP_Tool;

import java.util.HashMap;
import java.util.Map;

public class aEP_SelfRotate extends BaseHullMod
{
  static final float SO_ROTATE_SPEED_MULT = 6f;

  //speed, radius, weapon range buff
  static final Map<String, Float[]> mag = new HashMap<>();
  static final String id = "aEP_SelfRotate";

  static {
    mag.put("aEP_HaiLiang2", new Float[]{12f, 90f, 50f});
  }

  @Override
  public void advanceInCombat(ShipAPI ship, float amount) {
    if (!ship.isShipWithModules()) return;
    boolean canZeroBuff = true;
    float rotateSpeedMult = 1;
    //if (ship.getVariant().hasHullMod("safetyoverrides")) rotateSpeedMult = SO_ROTATE_SPEED_MULT;
    float allowLevel = ship.getMutableStats().getZeroFluxMinimumFluxLevel().getModifiedValue();
    if (ship.getMutableStats().getAllowZeroFluxAtAnyLevel().isPositive()) allowLevel = 1f;
    if (!ship.isAlive()) return;
    for (ShipAPI m : ship.getChildModulesCopy()) {
      //死模块过滤
      if (!mag.containsKey(m.getHullSpec().getHullId()) || !m.isAlive() || !Global.getCombatEngine().isEntityInPlay(m))
        continue;
      float speed = mag.get(m.getHullSpec().getHullId())[0];
      float radius = mag.get(m.getHullSpec().getHullId())[1];
      WeaponSlotAPI slot = m.getStationSlot();
      float timePassed = ship.getFullTimeDeployed() * rotateSpeedMult;
      while (timePassed > 360 / speed) {
        timePassed -= 360 / speed;
      }
      //setFacing的前提是槽要有射界
      m.setFacing(m.getFacing() + speed * timePassed);
      m.getLocation().set(aEP_Tool.getExtendedLocationFromPoint(ship.getLocation(), m.getFacing(), radius));
      if (m.getFluxLevel() > allowLevel) canZeroBuff = false;
    }

    ship.getMutableStats().getZeroFluxSpeedBoost().modifyMult("aEP_SelfRotate", 1f);
    if (!canZeroBuff) ship.getMutableStats().getZeroFluxSpeedBoost().modifyMult("aEP_SelfRotate", 0f);
  }

}
