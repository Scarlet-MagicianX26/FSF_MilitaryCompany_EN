package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

public class aEP_KBEffect implements BeamEffectPlugin
{
  @Override
  public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
    if (beam.getSource() != null) {
      if (!beam.getSource().hasListenerOfClass(KBListener.class)) {
        beam.getSource().addListener(new KBListener());
      }
    }
  }

  class KBListener implements DamageDealtModifier
  {
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
      if (param instanceof BeamAPI && shieldHit) {
        BeamAPI beam = (BeamAPI) param;
        if (target instanceof ShipAPI) {
          ShipAPI ship = (ShipAPI) target;
          float shieldMult = ship.getShield().getFluxPerPointOfDamage();
          damage.getModifier().modifyMult(this.getClass().getSimpleName(), 1 / (shieldMult + 0.01f));
        }

      }
      return this.getClass().getSimpleName();
    }
  }

}
