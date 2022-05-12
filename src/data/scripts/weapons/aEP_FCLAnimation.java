package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import combat.util.aEP_AnimationController;
import combat.util.aEP_DecoGlowController;
import combat.util.aEP_DecoMoveController;
import combat.util.aEP_DecoRevoController;

public class aEP_FCLAnimation implements EveryFrameWeaponEffectPlugin
{

  aEP_DecoMoveController decoMoveController;
  aEP_DecoRevoController decoRevoController;
  aEP_DecoGlowController decoGlowController;
  aEP_AnimationController animeController;

  @Override
  public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    ShipAPI ship = weapon.getShip();
    if (ship == null) return;
    if (animeController == null && weapon.getAnimation() != null)
      animeController = new aEP_AnimationController(weapon, weapon.getAnimation().getFrameRate());
    if (decoMoveController == null) decoMoveController = new aEP_DecoMoveController(weapon);
    if (decoRevoController == null) decoRevoController = new aEP_DecoRevoController(weapon);
    if (decoGlowController == null) decoGlowController = new aEP_DecoGlowController(weapon);

    //control animation
    decoRevoController.advance(amount);
    decoMoveController.advance(amount);
    decoGlowController.advance(amount);
    if (animeController != null) animeController.advance(amount);
  }

  public void setAnimeToLevel(float tolevel) {
    animeController.setToLevel(tolevel);
  }

  public void setMoveToLevel(float toLevel) {
    decoMoveController.setToLevel(toLevel);
  }

  public void setRevoToLevel(float toLevel) {
    decoRevoController.setToLevel(toLevel);
  }

  public void setGlowToLevel(float toLevel) {
    decoGlowController.setToLevel(toLevel);
  }

  public void setGlowEffectiveLevel(float effectiveLevel) {
    decoGlowController.setEffectiveLevel(effectiveLevel);
  }

  public void setFrameRate(float frameRate) {
    animeController.setSpeed(frameRate);
  }

  public aEP_AnimationController getAnimeController() {
    return animeController;
  }

  public aEP_DecoMoveController getDecoMoveController() {
    return decoMoveController;
  }

  public aEP_DecoRevoController getDecoRevoController() {
    return decoRevoController;
  }

  public aEP_DecoGlowController getDecoGlowController() {
    return decoGlowController;
  }
}

        	         	       	       	 