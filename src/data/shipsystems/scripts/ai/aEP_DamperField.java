package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class aEP_DamperField implements ShipSystemAIScript
{
  CombatEngineAPI engine;
  ShipSystemAPI system;
  ShipAPI ship;
  ShipwideAIFlags flags;

  IntervalUtil think = new IntervalUtil(0.2f, 0.3f);

  @Override
  public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
    this.ship = ship;
    this.system = system;
    this.engine = engine;
    this.flags = flags;
  }


  @Override
  public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    if(!think.intervalElapsed()) return;
    List<DamagingProjectileAPI> projs = CombatUtils.getProjectilesWithinRange(ship.getLocation(),1600f);
    for(DamagingProjectileAPI proj:projs){

    }
  }
}
