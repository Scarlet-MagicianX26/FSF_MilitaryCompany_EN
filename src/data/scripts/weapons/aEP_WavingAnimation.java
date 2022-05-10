package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static combat.impl.VEs.AEP_RepairSparkKt.spawnRepairingSpark;

public class aEP_WavingAnimation implements EveryFrameWeaponEffectPlugin
{


  private static final float MAX_SPEED = 60f;
  private static final float ACC = 15f;
  private static final float WAVING_ANGLE = 45f;
  private static final float WAVING_INTERVAL = 3.5f;
  private static final float WAVING_SPEED = 10f;
  private ShipAPI ship;
  private WeaponAPI w;
  private float amount;
  private CombatEngineAPI engine;
  private float toAngle;
  private float arcAngle;
  private final waveControlLer waveControlLer = new waveControlLer();
  private float turnRate = 0f;//turn rate now

  public static Vector2f getExtendedLocationFromPoint(Vector2f point, float facing, float dist) {
    float xAxis = (float) Math.cos(Math.toRadians(facing)) * dist;
    float yAxis = (float) Math.sin(Math.toRadians(facing)) * dist;
    return new Vector2f(point.getX() + xAxis, point.getY() + yAxis);
  }

  @Override
  public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
    this.amount = amount;
    this.w = weapon;
    this.ship = weapon.getShip();
    this.engine = engine;

    if (engine.isPaused() || weapon.getSlot().isHidden() || !ship.isAlive()) {
      return;
    }


    float shipVelocityAngle = aEP_Tool.angleAdd(aEP_Tool.Velocity2Speed(ship.getVelocity()).x, 180f);
    arcAngle = aEP_Tool.angleAdd(w.getArcFacing(), ship.getFacing());
    float currAngle = weapon.getCurrAngle();
    toAngle = shipVelocityAngle;
    //zero speed check
    if (aEP_Tool.Velocity2Speed(ship.getVelocity()).y <= 0) {
      toAngle = arcAngle;
    }


    //clamp
    // getShortest > 0 == toAngle is at right side of arcAngle
    if (MathUtils.getShortestRotation(arcAngle, toAngle) > weapon.getArc() / 2f) {
      toAngle = aEP_Tool.angleAdd(arcAngle, weapon.getArc() / 2f);
    }

    if (MathUtils.getShortestRotation(arcAngle, toAngle) < -weapon.getArc() / 2f) {
      toAngle = aEP_Tool.angleAdd(arcAngle, -weapon.getArc() / 2f);
    }

    //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),toAngle + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);


    //should wave check
    if (Math.abs(MathUtils.getShortestRotation(currAngle, toAngle)) <= WAVING_ANGLE / 2f + 2f) {

      //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),toAngle + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);
      waveControlLer.wave(amount);
      return;
      //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),toAngle + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);

    }
    else {
      waveControlLer.clear(amount);
    }


    moveToAngle(toAngle);
    //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),angleAdd(arcAngle,- weapon.getArc()/2f) + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);
    //float angle = VectorUtils.getAngle(ship.getLocation(),ship.getMouseTarget());
    //moveToAngle(angle);


    //we turn weapon here;
    w.setCurrAngle(weapon.getCurrAngle() + turnRate * amount);

    //Global.getCombatEngine().addFloatingText(getExtendedLocationFromPoint(ship.getLocation(), ship.getFacing(), 50f),  toAngle+ "", 20f ,new Color (0, 100, 200, 240),ship, 0.25f, 120f);


  }

  public void moveToAngle(float toAngle) {
    float angleDist = 0f;
    float angleNow = w.getCurrAngle();
    float turnRateNow = turnRate;// minus = turning to right
    float maxTurnRate = MAX_SPEED * amount;
    float accTurnRate = ACC * amount;
    float decTurnRate = ACC * amount;
    float trueMult = 1 / amount;


    //get which side to turn and how much to turn, minus = you are in the left side
    if (w.getArc() >= 360) {
      angleDist = MathUtils.getShortestRotation(angleNow, toAngle);
    }
    else {
      // getShortest > 0 == toAngle is at right side of arcAngle
      float toAngleDist = MathUtils.getShortestRotation(arcAngle, toAngle);
      float nowAngleDist = MathUtils.getShortestRotation(arcAngle, w.getCurrAngle());
      angleDist = toAngleDist - nowAngleDist;
      //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),angleDist + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);


    }


    //Global.getCombatEngine().addFloatingText(getExtendedLocationFromPoint(ship.getLocation(), ship.getFacing(), 50f), turnRateNow + "turnRate", 20f ,new Color (0, 100, 200, 240),ship, 0.25f, 120f);

    boolean turnRight = false;//true == should turn right, false == should turn left
    turnRight = angleDist < 0;

    //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(), turnRight + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);


    float angleDistBeforeStop = (turnRateNow / 2) * (turnRateNow / (accTurnRate * trueMult)) - 1;
    if (turnRight) {
      if (turnRateNow > 0)//want to turn right but is turning to left, turnRateNow > 0
      {
        if (turnRateNow >= decTurnRate)//stop turning left, till turnRateNow is 0
        {
          turnRate = (turnRateNow - decTurnRate);
        }
        else {
          turnRate = 0f;
        }

        //((ShipAPI)ship).giveCommand(ShipCommand.TURN_RIGHT,null,0);
      }
      else//want to turn right and is turning right, turnRateNow < 0
      {

        if (Math.abs(angleDist) >= angleDistBeforeStop)//accelerate till maxTurnRate
        {
          //((ShipAPI)ship).giveCommand(ShipCommand.TURN_RIGHT,null,0);

          if (Math.abs(turnRateNow) <= (maxTurnRate * trueMult)) {
            turnRate = (turnRateNow - accTurnRate);
          }
          else {
            turnRate = (-maxTurnRate * trueMult);
          }

        }
        else {
          //((ShipAPI)ship).giveCommand(ShipCommand.TURN_LEFT,null,0);

          if (Math.abs(turnRateNow) >= decTurnRate)//decelerate till 0
          {
            turnRate = (turnRateNow + decTurnRate);
          }
          else {
            turnRate = (0);
          }

        }

      }
    }
    else//to turn left
    {
      if (turnRateNow < 0)//want to turn left but is turning to right, turnRateNow < 0
      {
        //((ShipAPI)ship).giveCommand(ShipCommand.TURN_LEFT,null,0);


        if (Math.abs(turnRateNow) >= decTurnRate)//stop turning right, till turnRateNow is 0
        {
          turnRate = (turnRateNow + decTurnRate);
        }
        else {
          turnRate = (0);
        }


      }
      else//want to turn left and is turning left, turnTateNow > 0
      {

        if (Math.abs(angleDist) > angleDistBeforeStop)//accelerate till maxTurnRate
        {
          //((ShipAPI)ship).giveCommand(ShipCommand.TURN_LEFT,null,0);
          if (turnRateNow <= maxTurnRate * trueMult) {
            turnRate = (turnRateNow + accTurnRate);
          }
          else {
            turnRate = (maxTurnRate * trueMult);
          }


        }
        else {
          //((ShipAPI)ship).giveCommand(ShipCommand.TURN_RIGHT,null,0);
          if (turnRateNow >= decTurnRate)//decelerate till 0
          {
            turnRate = (turnRateNow - decTurnRate);
          }
          else {
            turnRate = (0);
          }

        }
      }
    }


  }

  private class waveControlLer
  {
    boolean waveToLeft = true;
    float interval;
    float timer = 0f;

    waveControlLer() {
      interval = MathUtils.getRandomNumberInRange(0, WAVING_INTERVAL);

    }


    private void wave(float amount) {
      timer = timer + amount;
      float angle;
      if (timer > (WAVING_INTERVAL * 3 + interval * 2) / 4f) {
        timer = 0f;
        waveToLeft = !waveToLeft;

        for (Vector2f loc : aEP_Tool.Util.getWeaponOffsetInAbsoluteCoo(w)) {
          //spawnRepairingSpark(loc,new Color(240,210,50,100));
        }
      }
      if (waveToLeft) {
        angle = aEP_Tool.angleAdd(toAngle, WAVING_ANGLE / 2f);
      }
      else {
        angle = aEP_Tool.angleAdd(toAngle, -WAVING_ANGLE / 2f);
      }


      if (MathUtils.getShortestRotation(angle, arcAngle) >= w.getArc() / 2f) {
        angle = aEP_Tool.angleAdd(arcAngle, -w.getArc() / 2f);
      }
      if (MathUtils.getShortestRotation(angle, arcAngle) <= -w.getArc() / 2f) {
        angle = aEP_Tool.angleAdd(arcAngle, w.getArc() / 2f);
      }


      moveToAngle(angle);
      //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),arcAngle + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);


      //float angle = VectorUtils.getAngle(ship.getLocation(),ship.getMouseTarget());
      //moveToAngle(angle);


      //we turn weapon here;
      w.setCurrAngle(w.getCurrAngle() + turnRate * amount);
    }

    private void clear(float amount) {
      timer = timer + amount;
      if (timer > 0.5) {
        interval = MathUtils.getRandomNumberInRange(0, WAVING_INTERVAL);
        timer = 0;
      }

      waveToLeft = !(MathUtils.getShortestRotation(w.getCurrAngle(), toAngle) > 0);
    }

  }


}

        	         	       	       	 
