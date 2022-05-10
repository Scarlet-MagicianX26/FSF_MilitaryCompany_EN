package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;

public class aEP_BeamCodeDamage implements BeamEffectPlugin
{

  private int timer = 0;


  @Override
  public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
    if (beam.didDamageThisFrame() && beam.getDamageTarget() instanceof ShipAPI) {
      timer = timer + 1;
      ShipAPI ship = (ShipAPI) beam.getDamageTarget();
      // apply the extra damage to the target
      engine.applyDamage(ship, //target
        beam.getTo(), // where to apply damage
        10f, // amount of damage
        DamageType.KINETIC, // damage type
        0f, // amount of EMP damage (none)
        false, // does this bypass shields? (no)
        true, // does this deal soft flux? (no)
        beam.getSource());


      if (timer > 5) {
        engine.applyDamage(ship,
          beam.getTo(), // where to apply damage
          100f, // amount of damage
          DamageType.FRAGMENTATION, // damage type
          0f, // amount of EMP damage (none)
          false, // does this bypass shields? (no)
          true, // does this deal soft flux? (no)
          beam.getSource());
        //engine.addFloatingText(ship.getLocation(),"No_Need_toRepair", 15f ,new Color(200,200,200),ship, 0.25f, 20f);

        timer = 0;
      }


    }
  }

}

