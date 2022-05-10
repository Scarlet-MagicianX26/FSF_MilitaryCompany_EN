//by111164
package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class aEP_FighterCollectorScript extends BaseShipSystemScript
{
  public static final float maxDragNum = 10f;
  public static final float maxRange = 2000f;
  public static final float minRange = 500f;
  public static final float maxDragSpeed = 700f;
  private static final Color dragColour = new Color(60, 60, 90, 60);
  CombatEngineAPI engine;
  ShipAPI ship;
  float amount;
  float allCost = 0;
  List<ShipAPI> fighters = new ArrayList();

  @Override
  public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
    engine = Global.getCombatEngine();
    ship = (ShipAPI) stats.getEntity();
    if (engine.isPaused() || ship == null || !ship.isAlive()) {
      return;
    }

    if (effectLevel > 0.1f) {

      fighters.clear();
      for (ShipAPI f : AIUtils.getNearbyEnemies(ship, maxRange)) {
        if (MathUtils.getDistance(ship.getLocation(), f.getLocation()) > minRange && f.isFighter()) {
          fighters.add(f);
        }
      }


      for (ShipAPI f : fighters) {

        Vector2f vel = aEP_Tool.getDistVector(f.getLocation(), ship.getLocation());
        float velX = vel.getX() * 0.5f;
        float velY = vel.getY() * 0.5f;
        Global.getCombatEngine().addSmoothParticle(f.getLocation(), new Vector2f(velX, velY), f.getCollisionRadius() + 30f, 1f * effectLevel, 0.35f * effectLevel, dragColour);
        float dragSpeed = maxDragSpeed * (1 - MathUtils.getDistance(f.getLocation(), ship.getLocation()) / (2 * maxRange));
        //Global.getCombatEngine().addSmoothParticle(f.getLocation(), new Vector2f(0, 0), f.getCollisionRadius() + 45f, 0.5f * effectLevel, 0.01f * effectLevel, dragColour);
        aEP_Tool.forceSetToPosition(f, ship.getLocation(), dragSpeed, 200f, amount);

      }


      Global.getCombatEngine().addSmoothParticle(ship.getLocation(), new Vector2f(0f, 0f), 2000f, 0.01f * effectLevel, 0.05f * effectLevel, dragColour);//size,brightness,duration

    }


  }


  public void unapply(MutableShipStatsAPI stats, String id) {
    allCost = 0f;
    fighters.clear();

  }


}
  
  
 
  

  

   

