package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import data.shipsystems.scripts.aEP_FighterCollectorScript;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class aEP_FighterCollectorAI implements ShipSystemAIScript
{


  private CombatEngineAPI engine;
  private ShipSystemAPI system;
  private ShipAPI ship;
  private List WeaponAPI;
  private ShipwideAIFlags flags;


  @Override
  public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {


    if (engine == null || ship == null || target == null || target.getShipAI() == null) {
      return;
    }

    if (engine.isPaused()) {
      return;
    }

    float allCost = 0f;
    for (ShipAPI f : AIUtils.getNearbyEnemies(ship, aEP_FighterCollectorScript.maxRange - 200f)) {
      if (f.isAlive() && f.isFighter()) {
        allCost = allCost + 1;
      }

    }

    if (allCost >= aEP_FighterCollectorScript.maxDragNum / 3f) {
      ship.useSystem();
    }

    return;

  }


  @Override
  public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
    this.ship = ship;
    this.system = system;
    this.engine = engine;
    this.flags = flags;
  }
}