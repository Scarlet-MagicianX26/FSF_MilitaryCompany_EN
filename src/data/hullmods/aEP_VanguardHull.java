package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import combat.util.aEP_DataTool;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static combat.util.aEP_DataTool.txt;

public class aEP_VanguardHull extends aEP_BaseHullMod
{
  static final float REDUCE_PERCENT = 0.75f;
  static final float FLUX_REDUCE_PER_HIT = 4f;
  static final float BEAM_PER_HIT_REDUCE_COMPROMISE = 0.1f;
  private static final Map<ShipAPI.HullSize, Float> REDUCE_AMOUNT = new HashMap();

  static {
    REDUCE_AMOUNT.put(ShipAPI.HullSize.FIGHTER, 0f);
    REDUCE_AMOUNT.put(ShipAPI.HullSize.FRIGATE, 20f);
    REDUCE_AMOUNT.put(ShipAPI.HullSize.DESTROYER, 20f);
    REDUCE_AMOUNT.put(ShipAPI.HullSize.CRUISER, 20f);
    REDUCE_AMOUNT.put(ShipAPI.HullSize.CAPITAL_SHIP, 30f);
  }

  public aEP_VanguardHull(){
    haveToBeWithMod.add("aEP_MarkerDissipation");
    notCompatibleList.add("reinforcedhull");
  }

  @Override
  public void applyEffectsAfterShipCreationImpl(ShipAPI ship, String id) {
    //remove itself if ship has not compatible hullmod
    if (!isApplicableToShip(ship)) {
      ship.getVariant().removeMod(id);
      return;
    }

    ship.getMutableStats().getDynamic().getMod(Stats.INDIVIDUAL_SHIP_RECOVERY_MOD).modifyFlat(id, 1000f);
    if (!ship.hasListenerOfClass(aEP_VanguardDamageTaken.class)) {
      ship.addListener(new aEP_VanguardDamageTaken(REDUCE_AMOUNT.get(ship.getHullSize()).floatValue(), ship));
    }
  }

  @Override
  public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
    return true;
  }

  @Override
  public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
    tooltip.addSectionHeading(txt("effect"), Alignment.MID, 5f);
    //tooltip.addGrid( 5 * 5f + 10f);
    tooltip.addPara("- " + txt("VA_des01"), 5f, Color.white, Color.green,
      String.format("%.0f", REDUCE_PERCENT * 100f),
      (REDUCE_AMOUNT.get(hullSize)).intValue() + "");
    tooltip.addSectionHeading(aEP_DataTool.txt("when_soft_up"), Alignment.MID, 5f);
    TooltipMakerAPI image = tooltip.beginImageWithText(Global.getSettings().getHullModSpec("aEP_VanguardHull").getSpriteName(), 48f);
    image.addPara("- " + txt("reduce_flux_per_hit"), 5f, Color.white, Color.green, String.format("%.1f", FLUX_REDUCE_PER_HIT));
    tooltip.addImageWithText(5f);

  }


  static class aEP_VanguardDamageTaken implements DamageTakenModifier
  {
    ShipAPI ship;
    String id = "aEP_VanguardDamageTaken";
    float reduceAmount = 0;

    aEP_VanguardDamageTaken(float reduceAmount, ShipAPI ship) {
      this.reduceAmount = reduceAmount;
      this.ship = ship;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
      if (MathUtils.getRandomNumberInRange(0f, 1f) > REDUCE_PERCENT) return null;
      if (shieldHit) return null;
      float damageAmount = damage.getDamage();
      float toReduce = Math.min(reduceAmount, damageAmount - 1);
      if(param instanceof BeamAPI){
        toReduce /= 4f;
      }
      float convertToMult = toReduce / Math.max(damageAmount, 1);
      //这个flat是修改的mult
      Global.getCombatEngine().addFloatingDamageText(point,
        toReduce,
        new Color(100, 200, 150, 150),
        target,
        null);
      damage.getModifier().modifyFlat(id, -convertToMult);
      //aEP_Tool.addDebugText(damage.getDamage()+"_"+toReduce);
      if (aEP_MarkerDissipation.getBufferLevel(ship) >= 1f)

        ship.getFluxTracker().decreaseFlux(getFluxReduce(damage,toReduce/reduceAmount *FLUX_REDUCE_PER_HIT));
      return id;
    }

    float getFluxReduce(DamageAPI damage, Float baseReduce){
      if(damage.getType() == DamageType.FRAGMENTATION){
        return baseReduce/4f;
      }

      return baseReduce;
    }
  }



}
