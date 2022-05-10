package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

public class aEP_DroneDashAI implements ShipSystemAIScript
{
  CombatEngineAPI engine;
  ShipSystemAPI system;
  ShipAPI ship;
  ShipwideAIFlags flags;
  IntervalUtil think = new IntervalUtil(0.5f, 0.5f);
  float willing = 0;

  @Override
  public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
    this.ship = ship;
    this.system = system;
    this.engine = engine;
    this.flags = flags;
  }


  @Override
  public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
    if (engine.isPaused()) {
      return;
    }

    think.advance(amount);
    willing = 0;
    if (!think.intervalElapsed()) return;
    Vector2f mousePoint = ship.getMouseTarget();
    if (ship.getShipTarget() != null) mousePoint = ship.getShipTarget().getLocation();
    if (ship.getWing() != null && ship.getWing().getSourceShip() != null)
      if (ship.getWing().getSourceShip().isPullBackFighters())
        mousePoint = ship.getWing().getSourceShip().getLocation();

    if (mousePoint == null) return;
    if (MathUtils.getDistance(mousePoint, ship.getLocation()) > 75 + ship.getCollisionRadius()) willing += 50;
    if (Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), mousePoint))) < 10f)
      willing += 35;
    if (ship.getEngineController().isAccelerating()) willing += 25;
    //aEP_Tool.addDebugText(willing+"");
    willing += MathUtils.getRandomNumberInRange(0f, 50f);
    if (willing >= 100f)
      ship.useSystem();

  }
}
