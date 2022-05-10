package data.shipsystems.scripts;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import combat.plugin.aEP_CombatEffectPlugin;

public class aEP_RQReload extends BaseShipSystemScript
{
  boolean didAdd = false;

  @Override
  public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
    ShipAPI ship = (ShipAPI) stats.getEntity();
    if (ship == null) return;


    boolean isAllFull = true;

    for (WeaponAPI w : ship.getAllWeapons()) {
      if (w.getSlot().getWeaponType() != WeaponAPI.WeaponType.BUILT_IN) continue;
      if (!w.getSpec().getWeaponId().equals("aEP_rq_missile")) continue;
      if (w.getAmmo() < w.getSpec().getBurstSize()) isAllFull = false;
      //循环后半段激活当帧走一次
      if (effectLevel < 1f) continue;
      didAdd = true;
      w.beginSelectionFlash();
      w.getAmmoTracker().setAmmo(Math.min(w.getAmmo() + w.getSpec().getBurstSize(), w.getMaxAmmo()));
      aEP_CombatEffectPlugin.Mod.addEffect(new aEP_NCReloadScript.RefresherOrb(ship));
    }

    //满弹失败强制结束，弹药补正1
    if (isAllFull && !didAdd) {
      ship.getSystem().deactivate();
      ship.getSystem().setAmmo(Math.min(ship.getSystem().getAmmo() + 1, ship.getSystem().getMaxAmmo()));
      unapply(stats, id);
    }
  }

  @Override
  public void unapply(MutableShipStatsAPI stats, String id) {
    didAdd = false;
  }
}
