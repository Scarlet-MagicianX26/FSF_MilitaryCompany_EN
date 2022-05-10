//by a111164
package combat.util

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.loading.WeaponSlotAPI
import combat.impl.VEs.aEP_MovingSmoke
import combat.plugin.aEP_CombatEffectPlugin
import data.scripts.util.MagicRender
import org.lazywizard.lazylib.CollisionUtils
import org.lazywizard.lazylib.FastTrig
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*
import kotlin.math.asin

class aEP_Tool {

  companion object Util {
    //move ship's facing
    //return time need to turn
    fun moveToAngle(ship: ShipAPI, toAngle: Float) {
      var angleDist = 0f
      val angleNow = ship.facing

      //get which side to turn and how much to turn, minus = you are in the left side
      angleDist = MathUtils.getShortestRotation(angleNow, toAngle)

      //Global.getCombatEngine().addFloatingText(ship.getLocation(),  "turnRate", 20f ,new Color (0, 100, 200, 240),ship, 0.25f, 120f);
      var turnRight = false //true == should turn right, false == should turn left
      turnRight = angleDist < 0
      val angleDistBeforeStop = ship.angularVelocity * ship.angularVelocity / (ship.mutableStats.turnAcceleration.modifiedValue * 2 + 1)
      if (turnRight) {
        if (ship.angularVelocity > 0) //want to turn right but is turning to left, turnRateNow > 0
        {
          ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0)
        } else  //want to turn right and is turning right, turnRateNow < 0
        {
          if (Math.abs(angleDist) - 2 >= angleDistBeforeStop) //accelerate till maxTurnRate
          {
            //((ShipAPI)ship).giveCommand(ShipCommand.TURN_RIGHT,null,0);
            ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0)
          } else {
            //((ShipAPI)ship).giveCommand(ShipCommand.TURN_LEFT,null,0);
            ship.giveCommand(ShipCommand.TURN_LEFT, null, 0)
          }
        }
      } else  //to turn left
      {
        if (ship.angularVelocity < 0) //want to turn left but is turning to right, turnRateNow < 0
        {
          ship.giveCommand(ShipCommand.TURN_LEFT, null, 0)
        } else  //want to turn left and is turning left, turnTateNow > 0
        {
          if (Math.abs(angleDist) - 2 > angleDistBeforeStop) //accelerate till maxTurnRate
          {
            ship.giveCommand(ShipCommand.TURN_LEFT, null, 0)
          } else {
            ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0)
          }
        }
      }
    }

    fun moveToAngle(ship: MissileAPI, toAngle: Float) {
      var angleDist = 0f
      val angleNow = ship.facing

      //get which side to turn and how much to turn, minus = you are in the left side
      angleDist = MathUtils.getShortestRotation(angleNow, toAngle)


      //Global.getCombatEngine().addFloatingText(ship.getLocation(),  "turnRate", 20f ,new Color (0, 100, 200, 240),ship, 0.25f, 120f);
      var turnRight = false //true == should turn right, false == should turn left
      turnRight = angleDist < 0
      val angleDistBeforeStop = ship.angularVelocity * ship.angularVelocity / (ship.turnAcceleration * 2 + 1)
      if (turnRight) {
        if (ship.angularVelocity > 0) //want to turn right but is turning to left, turnRateNow > 0
        {
          ship.giveCommand(ShipCommand.TURN_RIGHT)
        } else  //want to turn right and is turning right, turnRateNow < 0
        {
          if (Math.abs(angleDist) - 2 >= angleDistBeforeStop) //accelerate till maxTurnRate
          {
            //((ShipAPI)ship).giveCommand(ShipCommand.TURN_RIGHT,null,0);
            ship.giveCommand(ShipCommand.TURN_RIGHT)
          } else {
            //((ShipAPI)ship).giveCommand(ShipCommand.TURN_LEFT,null,0);
            ship.giveCommand(ShipCommand.TURN_LEFT)
          }
        }
      } else  //to turn left
      {
        if (ship.angularVelocity < 0) //want to turn left but is turning to right, turnRateNow < 0
        {
          ship.giveCommand(ShipCommand.TURN_LEFT)
        } else  //want to turn left and is turning left, turnTateNow > 0
        {
          if (Math.abs(angleDist) - 2 > angleDistBeforeStop) //accelerate till maxTurnRate
          {
            ship.giveCommand(ShipCommand.TURN_LEFT)
          } else {
            ship.giveCommand(ShipCommand.TURN_RIGHT)
          }
        }
      }
    }

    //rotate velocity of a ship but not facing
    fun moveToAngle(ship: CombatEntityAPI, velocity: Vector2f?, toAngle: Float, amount: Float) {
      var angleDist = 0f
      val angleNow = VectorUtils.getFacing(velocity)
      val turnRateNow = 0f
      var maxTurnRate = 0f
      if (ship is ShipAPI) {
        maxTurnRate = ship.maxTurnRate * amount
      }
      if (ship is MissileAPI) {
        maxTurnRate = ship.maxTurnRate * amount
      }
      var turnRight = false //true == should turn right, false == should turn left
      //judge how much to turn;
      angleDist = if (Math.abs(angleNow - toAngle) < 360f - Math.abs(angleNow - toAngle)) {
        Math.abs(angleNow - toAngle)
      } else {
        360f - Math.abs(angleNow - toAngle)
      }
      //judge which side to turn
      turnRight = if (toAngle < 180f) {
        angleNow - toAngle < 180f && angleNow - toAngle > 0f
      } else {
        angleNow - toAngle >= 0f || angleNow <= toAngle - 180f
      }

      //to turn the ship
      if (angleDist > maxTurnRate) {
        if (turnRight) {
          VectorUtils.rotate(ship.velocity, -maxTurnRate)
        } else {
          VectorUtils.rotate(ship.velocity, maxTurnRate)
        }
      }
    }

    //turn weapon to angle with accelerate
    fun moveToAngle(toAngle: Float, MAX_SPEED: Float, ACC: Float, w: WeaponAPI, turnRate: Float, amount: Float): Float {
      var turnRate = turnRate
      var angleDist = 0f
      val angleNow = w.currAngle
      val turnRateNow = turnRate // minus = turning to right
      val maxTurnRate = MAX_SPEED * amount
      val accTurnRate = ACC * amount
      val decTurnRate = ACC * amount
      val trueMult = 1 / amount
      val arcAngle = w.arcFacing


      //get which side to turn and how much to turn, minus = you are in the left side
      angleDist = if (w.arc >= 360) {
        MathUtils.getShortestRotation(angleNow, toAngle)
      } else {
        // getShortest > 0 == toAngle is at right side of arcAngle
        val toAngleDist = MathUtils.getShortestRotation(arcAngle, toAngle)
        val nowAngleDist = MathUtils.getShortestRotation(arcAngle, w.currAngle)
        toAngleDist - nowAngleDist
        //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),angleDist + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);
      }

      //stable ship's direction if it is nearly there
      if (Math.abs(angleDist) <= maxTurnRate && Math.abs(turnRateNow) <= decTurnRate) {
        w.currAngle = toAngle
        return 0f
      }
      //Global.getCombatEngine().addFloatingText(getExtendedLocationFromPoint(ship.getLocation(), ship.getFacing(), 50f), turnRateNow + "turnRate", 20f ,new Color (0, 100, 200, 240),ship, 0.25f, 120f);
      var turnRight = false //true == should turn right, false == should turn left
      turnRight = angleDist < 0

      //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(), turnRight + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,2f);
      val angleDistBeforeStop = turnRateNow / 2 * (turnRateNow / (accTurnRate * trueMult))
      turnRate = if (turnRight) {
        if (turnRateNow > 0) //want to turn right but is turning to left, turnRateNow > 0
        {
          if (turnRateNow >= decTurnRate) //stop turning left, till turnRateNow is 0
          {
            turnRateNow - decTurnRate
          } else {
            0f
          }

          //((ShipAPI)ship).giveCommand(ShipCommand.TURN_RIGHT,null,0);
        } else  //want to turn right and is turning right, turnRateNow < 0
        {
          if (Math.abs(angleDist) >= angleDistBeforeStop) //accelerate till maxTurnRate
          {
            //((ShipAPI)ship).giveCommand(ShipCommand.TURN_RIGHT,null,0);
            if (Math.abs(turnRateNow) <= maxTurnRate * trueMult) {
              turnRateNow - accTurnRate
            } else {
              -maxTurnRate * trueMult
            }
          } else {
            //((ShipAPI)ship).giveCommand(ShipCommand.TURN_LEFT,null,0);
            if (Math.abs(turnRateNow) >= decTurnRate) //decelerate till 0
            {
              turnRateNow + decTurnRate
            } else {
              0.toFloat()
            }
          }
        }
      } else  //to turn left
      {
        if (turnRateNow < 0) //want to turn left but is turning to right, turnRateNow < 0
        {
          //((ShipAPI)ship).giveCommand(ShipCommand.TURN_LEFT,null,0);
          if (Math.abs(turnRateNow) >= decTurnRate) //stop turning right, till turnRateNow is 0
          {
            turnRateNow + decTurnRate
          } else {
            0.toFloat()
          }
        } else  //want to turn left and is turning left, turnTateNow > 0
        {
          if (Math.abs(angleDist) > angleDistBeforeStop) //accelerate till maxTurnRate
          {
            //((ShipAPI)ship).giveCommand(ShipCommand.TURN_LEFT,null,0);
            if (turnRateNow <= maxTurnRate * trueMult) {
              turnRateNow + accTurnRate
            } else {
              maxTurnRate * trueMult
            }
          } else {
            //((ShipAPI)ship).giveCommand(ShipCommand.TURN_RIGHT,null,0);
            if (turnRateNow >= decTurnRate) //decelerate till 0
            {
              turnRateNow - decTurnRate
            } else {
              0.toFloat()
            }
          }
        }
      }
      return turnRate
    }

    //+ = turnLeft, - + turn right
    @JvmStatic
    fun angleAdd(originAngle: Float, addAngle: Float): Float {
      var addAngle = addAngle
      val finalAngle = originAngle + addAngle
      while (addAngle > 360) addAngle = addAngle - 360
      while (addAngle < 0) addAngle = addAngle + 360
      return finalAngle
    }

    @JvmStatic
    fun getTimeNeedToTurn(angle: Float, toAngle: Float, turnRate: Float, acc: Float, dec: Float, maxTurnRate: Float): Float {
      var turnRate = turnRate
      var time = 0f
      var dist = 0f


      //no acc is invalid
      if (acc <= 0f) {
        return 999f
      }


      //get which side to turn and how much to turn, minus = you are in the left side
      //turn rate > = 0 means turing to left
      dist = MathUtils.getShortestRotation(angle, toAngle)
      var sameSide = dist <= 0 || turnRate >= 0
      if (dist < 0 && turnRate > 0) {
        sameSide = false
      }
      while (Math.abs(dist) > 1f && turnRate > 1f) {

        //Global.getCombatEngine().addFloatingText(Global.getCombatEngine().getPlayerShip().getMouseTarget(),time + "", 20f ,new Color(100,100,100,100),Global.getCombatEngine().getPlayerShip(), 0.25f, 120f);

        //stop turning to the other side first
        if (!sameSide) {
          time = time + Math.abs(turnRate / dec)
          turnRate = 0f
          dist = dist - turnRate / 2 * Math.abs(turnRate / dec)
        } else {
          //if we are here, we stopped turning opposite
          //if we already turning to fast and should slow down from now, do this method again when we reach target angle
          if (Math.abs(dist) <= turnRate / 2 * Math.abs(turnRate / dec)) {
            val delta = Math.sqrt((turnRate * turnRate - 2 * acc * dist).toDouble()).toFloat()
            val resolve1 = (-turnRate + delta) / -acc
            val resolve2 = (-turnRate - delta) / -acc
            return if (resolve1 > resolve2) {
              time + resolve2
            } else time + resolve1
          }


          //if we can acc to max speed before dec
          val distToSlow = maxTurnRate / 2 * (maxTurnRate / acc)
          if (distToSlow <= Math.abs(dist)) {
            time = time + 2 * (maxTurnRate / acc)
            dist = Math.abs(dist) - distToSlow
            return time + dist / maxTurnRate
          }


          //now the only situation is we acc first and dec before reach max turn speed
          dist = Math.abs(dist)
          return time + (2 * dist - turnRate * turnRate) / turnRate + turnRate / acc
        }
      }
      return time
    }

    @JvmStatic
    fun moveToPosition(entity: ShipAPI, toPosition: Vector2f?) {
      val directionVec = VectorUtils.getDirectionalVector(entity.location, toPosition)
      val directionAngle = VectorUtils.getFacing(directionVec)
      val distSq = MathUtils.getDistanceSquared(entity.location, toPosition)
      val angleAndSpeed = Velocity2Speed(entity.velocity)
      val timeToSlowDown = angleAndSpeed.y / entity.deceleration //time to slow down to zero speed(by seconds,squared)				s
      val aimAngle = MathUtils.getShortestRotation(entity.facing, directionAngle)
      var speedAngle = 0f
      //当舰船速度不为0时，计算实际速度角度与舰船朝向的角度的差值
      if(entity.velocity.x != 0f && entity.velocity.y != 0f){
        speedAngle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getFacing(entity.velocity),entity.facing))
      }
      val maxStopAngle = entity.maxTurnRate / entity.turnAcceleration * (entity.maxTurnRate / 2)
      //当角度偏差超过转向能力时，全速反向转向
      if (aimAngle > maxStopAngle) {
        entity.giveCommand(ShipCommand.TURN_LEFT, null, 0)
      } else if (aimAngle < -maxStopAngle) {
        entity.giveCommand(ShipCommand.TURN_RIGHT, null, 0)
      }else{//能够正常停下时，缓速转向
        moveToAngle(entity, directionAngle)
      }

      //实际速度方向与船头朝向差别过大，减速
      val threshold = 31.4f * (entity.acceleration/entity.maxTurnRate)/(entity.turnAcceleration/entity.maxTurnRate) *
          entity.maxTurnRate / (entity.maxSpeed)
      if(speedAngle > threshold){
        entity.giveCommand(ShipCommand.DECELERATE, null, 0)
      }else{
        if (distSq <= (angleAndSpeed.y * timeToSlowDown + 10) * (angleAndSpeed.y * timeToSlowDown + 10)) {
          entity.giveCommand(ShipCommand.DECELERATE, null, 0)
        }else{
          entity.giveCommand(ShipCommand.ACCELERATE, null, 0)
        }
      }

    }

    //不会在靠近目标时开始减速处停下
    fun flyToPosition(entity: MissileAPI, toPosition: Vector2f?) {
      val directionVec = VectorUtils.getDirectionalVector(entity.location, toPosition)
      val directionAngle = VectorUtils.getFacing(directionVec)
      val aimAngle = MathUtils.getShortestRotation(entity.facing, directionAngle)
      val maxStopAngle = entity.maxTurnRate / entity.turnAcceleration * (entity.maxTurnRate / 2)
      var speedAngle = 0f
      //当舰船速度不为0时，计算实际速度角度与舰船朝向的角度的差值
      if(entity.velocity.x != 0f && entity.velocity.y != 0f){
        speedAngle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getFacing(entity.velocity),entity.facing))
      }
      if (aimAngle > maxStopAngle) {
        entity.giveCommand(ShipCommand.TURN_LEFT)
      } else if (aimAngle < -maxStopAngle) {
        entity.giveCommand(ShipCommand.TURN_RIGHT)
      } else {
        moveToAngle(entity, directionAngle)
      }
      val threshold = 31.4f * (entity.acceleration/entity.maxTurnRate)/(entity.turnAcceleration/entity.maxTurnRate) *
          entity.maxTurnRate / (entity.maxSpeed)
      if(speedAngle > threshold) {
        entity.giveCommand(ShipCommand.DECELERATE)
      }else{
        entity.giveCommand(ShipCommand.ACCELERATE)
      }
    }

    fun flyToPosition(entity: ShipAPI, toPosition: Vector2f?) {
      val directionVec = VectorUtils.getDirectionalVector(entity.location, toPosition)
      val directionAngle = VectorUtils.getFacing(directionVec)
      val aimAngle = MathUtils.getShortestRotation(entity.facing, directionAngle)
      val maxStopAngle = entity.maxTurnRate / entity.turnAcceleration * (entity.maxTurnRate / 2)
      var speedAngle = 0f
      //当舰船速度不为0时，计算实际速度角度与舰船朝向的角度的差值
      if(entity.velocity.x != 0f && entity.velocity.y != 0f){
        speedAngle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getFacing(entity.velocity),entity.facing))
      }
      //aim angle > 0 = need to turn left,you are on the right side
      if (aimAngle > maxStopAngle) {
        entity.giveCommand(ShipCommand.TURN_LEFT,null,0)
      } else if (aimAngle < -maxStopAngle) {
        entity.giveCommand(ShipCommand.TURN_RIGHT,null,0)
      } else {
        moveToAngle(entity, directionAngle)
      }
      val threshold = 31.4f * (entity.acceleration/entity.maxTurnRate)/(entity.turnAcceleration/entity.maxTurnRate) *
          entity.maxTurnRate / (entity.maxSpeed)
      if(speedAngle > threshold) {
        entity.giveCommand(ShipCommand.DECELERATE, null, 0)
      }else{
        entity.giveCommand(ShipCommand.ACCELERATE, null, 0)
      }
    }


    // move to position but not change facing
    fun setToPosition(ship: ShipAPI, toPosition: Vector2f?, amount: Float) {
      val directionVec = VectorUtils.getDirectionalVector(ship.location, toPosition)
      val directionAngle = VectorUtils.getFacing(directionVec)
      val acceleration = ship.acceleration * amount
      val xAxis = FastTrig.cos(Math.toRadians(directionAngle.toDouble())).toFloat() * acceleration
      val yAxis = FastTrig.sin(Math.toRadians(directionAngle.toDouble())).toFloat() * acceleration
      val dist = MathUtils.getDistance(ship.location, toPosition)
      val timeToSlowDown = Velocity2Speed(ship.velocity).y / ship.deceleration
      val speedNow = Velocity2Speed(ship.velocity).y
      if (speedNow > ship.maxSpeed || dist <= Velocity2Speed(ship.velocity).y * timeToSlowDown + 2) {
        ship.giveCommand(ShipCommand.DECELERATE, null, 0)
      } else {
        //ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
        moveToAngle(ship, ship.velocity, directionAngle, amount)
        val X = ship.velocity.getX()
        val Y = ship.velocity.getY()
        ship.velocity.setX(X + xAxis)
        ship.velocity.setY(Y + yAxis)
        for (engine in ship.engineController.shipEngines) {
          ship.engineController.setFlameLevel(engine.engineSlot, 0.5f)
        }
      }
    }

    // move to position but not change facing with target
    fun setToPosition(ship: ShipAPI, toPosition: Vector2f?, amount: Float, target: CombatEntityAPI) {
      val directionVec = VectorUtils.getDirectionalVector(ship.location, toPosition)
      val directionAngle = VectorUtils.getFacing(directionVec)
      val acceleration = ship.acceleration * amount
      val xAxis = FastTrig.cos(Math.toRadians(directionAngle.toDouble())).toFloat() * acceleration
      val yAxis = FastTrig.sin(Math.toRadians(directionAngle.toDouble())).toFloat() * acceleration
      val dist = MathUtils.getDistance(ship.location, toPosition)
      val relativeVel = Vector2f(ship.velocity.x - target.velocity.x, ship.velocity.y - target.velocity.y)
      val timeToSlowDown = Velocity2Speed(relativeVel).y / ship.deceleration
      val speedNow = Velocity2Speed(ship.velocity).y
      if (speedNow > ship.maxSpeed || dist <= Velocity2Speed(ship.velocity).y * timeToSlowDown + 2) {
        ship.giveCommand(ShipCommand.DECELERATE, null, 0)
      } else {
        //ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
        moveToAngle(ship, ship.velocity, directionAngle, amount)
        val X = ship.velocity.getX()
        val Y = ship.velocity.getY()
        ship.velocity.setX(X + xAxis)
        ship.velocity.setY(Y + yAxis)
        for (engine in ship.engineController.shipEngines) {
          ship.engineController.setFlameLevel(engine.engineSlot, 0.5f)
        }
      }
    }

    // move to position at a speed , will not slow down and stop at that position
    @JvmStatic
    fun forceSetToPosition(ship: ShipAPI, toPosition: Vector2f?, speed: Float, deceleration: Float, amount: Float) {
      val directionVec = VectorUtils.getDirectionalVector(ship.location, toPosition)
      val directionAngle = VectorUtils.getFacing(directionVec)
      val acceleration = speed * amount
      val dec = deceleration * amount
      val xAxis = FastTrig.cos(Math.toRadians(directionAngle.toDouble())).toFloat()
      val yAxis = FastTrig.sin(Math.toRadians(directionAngle.toDouble())).toFloat()
      val speedNow = Velocity2Speed(ship.velocity).y
      val X = ship.velocity.getX()
      val Y = ship.velocity.getY()
      if (speedNow > speed) {
        moveToAngle(ship, ship.velocity, directionAngle, amount)
        ship.velocity.setX(X - xAxis * dec)
        ship.velocity.setY(Y - yAxis * dec)
      } else {
        moveToAngle(ship, ship.velocity, directionAngle, amount)
        ship.velocity.setX(X + xAxis * acceleration)
        ship.velocity.setY(Y + yAxis * acceleration)
      }
    }

    //vector2f(angle, speed)
    @JvmStatic
    fun Velocity2Speed(velocity: Vector2f): Vector2f {
      val x = VectorUtils.getFacing(velocity)
      val y = Math.sqrt((velocity.getX() * velocity.getX() + velocity.getY() * velocity.getY()).toDouble()).toFloat()
      return Vector2f(x, y)
    }

    fun Speed2Velocity(angle: Float, speed: Float): Vector2f {
      val xAxis = FastTrig.cos(Math.toRadians(angle.toDouble())).toFloat() * speed
      val yAxis = FastTrig.sin(Math.toRadians(angle.toDouble())).toFloat() * speed
      return Vector2f(xAxis, yAxis)
    }

    fun Speed2Velocity(angleAndSpeed: Vector2f): Vector2f {
      val angle = angleAndSpeed.x
      val speed = angleAndSpeed.y
      val xAxis = FastTrig.cos(Math.toRadians(angle.toDouble())).toFloat() * speed
      val yAxis = FastTrig.sin(Math.toRadians(angle.toDouble())).toFloat() * speed
      return Vector2f(xAxis, yAxis)
    }

    fun Facing2Vector(facing: Float): Vector2f {
      val xAxis = FastTrig.cos(Math.toRadians(facing.toDouble())).toFloat()
      val yAxis = FastTrig.sin(Math.toRadians(facing.toDouble())).toFloat()
      return Vector2f(xAxis, yAxis)
    }

    @JvmStatic
    fun getDistVector(from: Vector2f, to: Vector2f): Vector2f {
      val X = to.getX() - from.getX()
      val Y = to.getY() - from.getY()
      return Vector2f(X, Y)
    }

    @JvmStatic
    fun getExtendedLocationFromPoint(point: Vector2f, facing: Float, dist: Float): Vector2f {
      val xAxis = FastTrig.cos(Math.toRadians(facing.toDouble())).toFloat() * dist
      val yAxis = FastTrig.sin(Math.toRadians(facing.toDouble())).toFloat() * dist
      return Vector2f(point.getX() + xAxis, point.getY() + yAxis)
    }

    fun getLocationTurnedRightByDegrees(originPosition: Vector2f?, turnAroundWhere: Vector2f, degrees: Float): Vector2f {
      var angle = VectorUtils.getAngle(turnAroundWhere, originPosition)
      val dist = MathUtils.getDistance(turnAroundWhere, originPosition)
      angle = angle + degrees
      if (angle > 365) {
        angle = angle - 365f
      }
      return getExtendedLocationFromPoint(turnAroundWhere, angle, dist)
    }

    fun getDegreesTurnedRightByLocation(originPosition: Vector2f?, turnAroundWhere: Vector2f, angleToTurn: Float): Vector2f {
      val angle = VectorUtils.getAngle(turnAroundWhere, originPosition)
      val dist = MathUtils.getDistance(turnAroundWhere, originPosition)
      return getExtendedLocationFromPoint(turnAroundWhere, angleAdd(angle, angleToTurn), dist)
    }

    fun getTargetWidthAngleInDistance(from: Vector2f?, target: CombatEntityAPI): Float {
      return (2 * 57.3f //57.3 is to convert from rad to degrees
          * Math.asin(
        (target.collisionRadius
            / MathUtils.getDistance(from, target.location)).toDouble()
      ).toFloat()) //drone width by degrees
    }

    @JvmStatic
    fun getTargetWidthAngleInDistance(from: Vector2f?, targetLocation: Vector2f?, targetRadius: Float): Float {
      return if (targetRadius >= MathUtils.getDistance(from, targetLocation)) {
        0f
      //57.3 is to convert from rad to degrees
      } else 2 * 57.3f * asin(
        (targetRadius
            / MathUtils.getDistance(from, targetLocation)).toDouble()
      ).toFloat()
      //drone width by degrees
    }

    @JvmStatic
    fun isFriendlyInLine(from: WeaponAPI): ShipAPI? {
      val weaponRange = from.range
      val weaponFacing = from.currAngle
      val allShipsInArc: MutableList<CombatEntityAPI> = ArrayList()
      val allShips = CombatUtils.getEntitiesWithinRange(from.slot.computePosition(from.ship), weaponRange)
        ?: return null
      //return false if there is no ship around
      for (s in allShips) {
        val targetFacing = VectorUtils.getAngle(from.slot.computePosition(from.ship), s.location)
        val targetWidth = getTargetWidthAngleInDistance(from.slot.computePosition(from.ship), s)
        if (Math.abs(MathUtils.getShortestRotation(weaponFacing, targetFacing)) < targetWidth / 2 && s is ShipAPI && !s.isFighter) {
          //Global.getCombatEngine().addFloatingText(s.getLocation(),MathUtils.getShortestRotation(weaponFacing , targetFacing) + "", 20f ,new Color(100,100,100,100),s, 0.25f, 120f);
          allShipsInArc.add(s)
        }
      }
      var closestDist = 4000f
      var closestShip: CombatEntityAPI? = null
      for (c in allShipsInArc) {
        if (MathUtils.getDistance(c, from.ship) < closestDist) {
          closestDist = MathUtils.getDistance(c, from.ship)
          closestShip = c
        }
      }
      if (closestShip == null) {
        return null
      }
      val ship = closestShip as ShipAPI
      return if (ship.isAlly || ship.owner == 0) {
        ship
      } else {
        null
      }
    }

    fun isEnemyInRange(from: WeaponAPI): ShipAPI? {
      val weaponRange = from.range
      val weaponFacing = from.currAngle
      val allShipsInArc: MutableList<CombatEntityAPI> = ArrayList()
      val allShips = CombatUtils.getEntitiesWithinRange(from.slot.computePosition(from.ship), weaponRange)
        ?: return null
      //return false if there is no ship around
      for (s in allShips) {
        val targetFacing = VectorUtils.getAngle(from.slot.computePosition(from.ship), s.location)
        val targetWidth = getTargetWidthAngleInDistance(from.slot.computePosition(from.ship), s)
        if (Math.abs(MathUtils.getShortestRotation(weaponFacing, targetFacing)) < targetWidth / 2 && s is ShipAPI && !s.isFighter) {
          //Global.getCombatEngine().addFloatingText(s.getLocation(),MathUtils.getShortestRotation(weaponFacing , targetFacing) + "", 20f ,new Color(100,100,100,100),s, 0.25f, 120f);
          allShipsInArc.add(s)
        }
      }
      var closestDist = 4000f
      var closestShip: CombatEntityAPI? = null
      for (c in allShipsInArc) {
        if (MathUtils.getDistance(c, from.ship) < closestDist) {
          closestDist = MathUtils.getDistance(c, from.ship)
          closestShip = c
        }
      }
      if (closestShip == null) {
        return null
      }
      val ship = closestShip as ShipAPI
      return if (!ship.isAlly && ship.owner != 0) {
        ship
      } else {
        null
      }
    }

    @JvmStatic
    fun aimToPoint(weapon: WeaponAPI, toTargetPo: Vector2f?) {
      val ship = weapon.ship
      val angle = VectorUtils.getDirectionalVector(weapon.location, toTargetPo)
      val angleFacing = VectorUtils.getFacing(angle)
      val maxTurnRate = weapon.turnRate / 60f
      val angleDist = MathUtils.getShortestRotation(weapon.currAngle, angleFacing)
      if (Math.abs(MathUtils.getShortestRotation(weapon.slot.angle + ship.facing, angleFacing)) > weapon.slot.arc / 2) {
      } else {
        if (angleDist >= 0) {
          if (angleDist > maxTurnRate) {
            weapon.currAngle = weapon.currAngle + maxTurnRate
          } else {
            weapon.currAngle = angleFacing
          }
        } else {
          if (angleDist < -maxTurnRate) {
            weapon.currAngle = weapon.currAngle - maxTurnRate
          } else {
            weapon.currAngle = angleFacing
          }
        }
      }
    }

    //by seconds
    @JvmStatic
    fun projTimeToHitShip(proj: CombatEntityAPI, ship: CombatEntityAPI): Float {
      val projFlyRange = 1600f
      var projEnd = proj.location
      projEnd = if (proj is MissileAPI && proj.isGuided) {
        ship.location
      } else {
        getExtendedLocationFromPoint(proj.location, VectorUtils.getFacing(proj.velocity), projFlyRange)
      }
      val hitPoint = CollisionUtils.getCollisionPoint(proj.location, projEnd, ship)
      return if (hitPoint == null) {
        1000f
      } else {
        MathUtils.getDistance(proj.location, hitPoint) / Velocity2Speed(proj.velocity).y
      }
    }

    @JvmStatic
    fun returnToParent(ship: ShipAPI, parentShip: ShipAPI, amount: Float) {
      ship.giveCommand(ShipCommand.HOLD_FIRE, null, 0)
      val id = "aEP_ReturnToParent"
      if (parentShip.launchBaysCopy.size <= 0) {
        val callBackColor = if (ship.shield != null) ship.shield.innerColor else Color(100, 100, 200, 100)
        Global.getCombatEngine().spawnExplosion(
          ship.location,  //loc
          Vector2f(0f, 0f),  //velocity
          callBackColor,  //color
          ship.collisionRadius * 3f,  //range
          0.5f
        ) //duration
        Global.getCombatEngine().removeEntity(ship)
      }

      //landing check
      val landingStarted = ship.isLanding
      val toTargetPo = ship.wing.source.getLandingLocation(ship)
      val dist = MathUtils.getDistance(ship.location, toTargetPo)
      if (dist > 100f) {
        moveToPosition(ship, toTargetPo)
        if (landingStarted) {
          ship.abortLanding()
        }
      } else {
        ship.mutableStats.maxSpeed.modifyFlat(id, parentShip.maxSpeed + ship.maxSpeed)
        moveToAngle(ship, parentShip.facing)
        setToPosition(ship, toTargetPo, amount, parentShip)
      }
      if (dist <= 50f) {
        if (!landingStarted) {
          ship.beginLandingAnimation(parentShip)
          ship.mutableStats.maxSpeed.modifyFlat(id, parentShip.maxSpeed + ship.maxSpeed)
        }
      }
      if (ship.isFinishedLanding) {
        ship.wing.source.land(ship)
      }
    }

    fun getSpriteRelPoint(spriteCenter: Vector2f, spriteSize: Vector2f, facing: Float, boundPoint: Vector2f?): Vector2f {
      val dist = MathUtils.getDistance(spriteCenter, boundPoint)
      val originFacing = VectorUtils.getAngle(spriteCenter, boundPoint)
      val boundPointInStandardCoord = getExtendedLocationFromPoint(spriteCenter, originFacing - facing + 90f, dist)
      val lowerLeftInStandardCoord = Vector2f(spriteCenter.x - spriteSize.x / 2f, spriteCenter.y - spriteSize.y / 2f)
      return Vector2f((boundPointInStandardCoord.x - lowerLeftInStandardCoord.x) / spriteSize.x, (boundPointInStandardCoord.y - lowerLeftInStandardCoord.y) / spriteSize.y)
    }

    //x is angle,y is dist
    fun getRelativeLocationData(hitPoint: Vector2f?, target: CombatEntityAPI, relativeToShield: Boolean): Vector2f {
      val absoluteAngle = VectorUtils.getAngle(target.location, hitPoint)
      var angle = 0f
      angle = if (relativeToShield) {
        angleAdd(absoluteAngle, -target.shield.facing)
      } else {
        angleAdd(absoluteAngle, -target.facing)
      }
      val dist = MathUtils.getDistance(target.location, hitPoint)
      return Vector2f(angle, dist)
    }

    fun getAbsoluteLocation(angle: Float, dist: Float, target: CombatEntityAPI, relativeToShield: Boolean): Vector2f {
      return if (relativeToShield) {
        val absoluteAngle = angleAdd(angle, target.shield.facing)
        getExtendedLocationFromPoint(target.location, absoluteAngle, dist)
      } else {
        val absoluteAngle = angleAdd(angle, target.facing)
        getExtendedLocationFromPoint(target.location, absoluteAngle, dist)
      }
    }

    fun getAbsoluteLocation(relativeData: Vector2f, target: CombatEntityAPI, relativeToShield: Boolean): Vector2f {
      val angle = relativeData.x
      val dist = relativeData.y
      return if (relativeToShield) {
        val absoluteAngle = angleAdd(angle, target.shield.facing)
        getExtendedLocationFromPoint(target.location, absoluteAngle, dist)
      } else {
        val absoluteAngle = angleAdd(angle, target.facing)
        getExtendedLocationFromPoint(target.location, absoluteAngle, dist)
      }
    }

    fun getWeaponOffsetInAbsoluteCoo(w: WeaponAPI): List<Vector2f> {
      val absCoo: MutableList<Vector2f> = ArrayList()
      if (w.slot.isTurret) {
        for (offset in w.spec.turretFireOffsets) {
          var absOffset = getExtendedLocationFromPoint(w.location, w.currAngle, offset.x)
          absOffset = getExtendedLocationFromPoint(absOffset, angleAdd(w.currAngle, 90f), offset.y)
          absCoo.add(absOffset)
        }
      } else {
        for (offset in w.spec.hardpointFireOffsets) {
          var absOffset = getExtendedLocationFromPoint(w.location, w.currAngle, offset.x)
          absOffset = getExtendedLocationFromPoint(absOffset, angleAdd(w.currAngle, 90f), offset.y)
          absCoo.add(absOffset)
        }
      }
      return absCoo
    }

    // +x = to top, -y = to right
    fun getWeaponOffsetInAbsoluteCoo(relativeCoo: Vector2f, w: WeaponAPI): Vector2f {
      var absOffset = getExtendedLocationFromPoint(w.location, w.currAngle, relativeCoo.x)
      absOffset = getExtendedLocationFromPoint(absOffset, angleAdd(w.currAngle, 90f), relativeCoo.y)
      return absOffset
    }

    fun getDistForProjToHitShield(proj: CombatEntityAPI, ship: ShipAPI): Float {
      if (ship.shield == null || ship.getShield().getType() == ShieldAPI.ShieldType.NONE) {
        return 9999f
      }
      val inAngle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.location, proj.location), ship.shield.facing)) < ship.shield.activeArc / 2
      return if (inAngle) {
        MathUtils.getDistance(proj.location, ship.location) - ship.shield.radius
      } else {
        9999f
      }
    }

    fun limitToTop(input: Float, limit: Float): Float {
      return if (input > limit) {
        limit
      } else input
    }

    @JvmStatic
    fun limitToTop(input: Float, limitMax: Float, limitMin: Float): Float {
      if (input > limitMax) {
        return limitMax
      }
      return if (input < limitMin) {
        limitMin
      } else input
    }

    fun getFirstDecimal(input: Float): Float {
      val input2string = java.lang.Float.toString(input)
      val allString = input2string.split("\\.".toRegex()).toTypedArray()
      return allString[1].toFloat()
    }


    fun getPlayerCargo():CargoAPI?{
      if(Global.getSector()?.playerFleet?.cargo != null){
        return Global.getSector().playerFleet.cargo
      }
      return null
    }

    @JvmStatic
    fun checkCargoAvailable(engine: CombatEngineAPI?, ship: ShipAPI?): Boolean {
      if (engine == null) {
        if (ship != null && ship.owner == 0) {
          return true
        }
        if (ship == null) {
          return Global.getSector().playerFleet != null
        }
      } else {
        return engine.isInCampaign && !engine.isInCampaignSim && ship != null && ship.owner == 0
      }
      return false
    }

    @JvmStatic
    fun killMissile(missile: CombatEntityAPI, engine: CombatEngineAPI) {
      engine.applyDamage(
        missile,  //target
        missile.location,  //point
        missile.hitpoints * 10,  //damage
        DamageType.ENERGY,
        0f,
        true,  //deal softflux
        true,  //is bypass shield
        missile
      ) //damage source
    }

    @JvmStatic
    fun getNearestFriendCombatShip(e: CombatEntityAPI?): ShipAPI? {
      var distMost = 1000000f
      var returnShip: ShipAPI? = null
      for (s in AIUtils.getAlliesOnMap(e)) {
        if (s.isFrigate || s.isDestroyer || s.isCruiser || s.isCapital) {
          val dist = MathUtils.getDistance(e, s)
          if (dist < distMost) {
            returnShip = s
            distMost = dist
          }
        }
      }
      return returnShip
    }

    @JvmStatic
    fun isNormalWeaponSlotType(slot: WeaponSlotAPI, containMissile: Boolean): Boolean {
      return if (slot.weaponType != WeaponAPI.WeaponType.DECORATIVE && slot.weaponType != WeaponAPI.WeaponType.BUILT_IN && slot.weaponType != WeaponAPI.WeaponType.LAUNCH_BAY && slot.weaponType != WeaponAPI.WeaponType.SYSTEM && slot.weaponType != WeaponAPI.WeaponType.STATION_MODULE) {
        if (containMissile) {
          true
        } else {
          slot.weaponType != WeaponAPI.WeaponType.MISSILE
        }
      } else false
    }

    @JvmStatic
    fun isNormalWeaponType(w: WeaponAPI, containMissile: Boolean): Boolean {
      return if (w.type != WeaponAPI.WeaponType.DECORATIVE && w.type != WeaponAPI.WeaponType.BUILT_IN && w.type != WeaponAPI.WeaponType.LAUNCH_BAY && w.type != WeaponAPI.WeaponType.SYSTEM && w.type != WeaponAPI.WeaponType.STATION_MODULE) {
        if (containMissile) {
          true
        } else {
          w.type != WeaponAPI.WeaponType.MISSILE
        }
      } else false
    }

    @JvmStatic
    fun getRandomPointAround(center: Vector2f, range: Float): Vector2f {
      val angle = MathUtils.getRandomNumberInRange(0, 360).toFloat()
      val dist = MathUtils.getRandomNumberInRange(0f, range)
      return getExtendedLocationFromPoint(center, angle, dist)
    }

    fun getOutOfRangeWeaponCostPercent(group: WeaponGroupAPI?, toTargetPo: Vector2f?, rangeFix: Float): Float {
      if (group == null) {
        return 1f
      }
      var allCost = 0.01f
      var ableToFireCost = 0.01f
      for (w in group.weaponsCopy) {
        if (w.distanceFromArc(toTargetPo) == 0f && isNormalWeaponSlotType(w.slot, false)) {
          allCost = allCost + w.spec.getOrdnancePointCost(null, null)
          if (MathUtils.getDistance(w.location, toTargetPo) > w.range - rangeFix) {
            ableToFireCost = ableToFireCost + w.spec.getOrdnancePointCost(null, null)
          }
        }
      }
      return limitToTop(ableToFireCost / allCost, 1f, 0f)
    }

    @JvmStatic
    fun toggleSystemControl(systemShip: ShipAPI, shouldUse: Boolean) {
      if (shouldUse && !systemShip.system.isActive) {
        systemShip.useSystem()
      }
      if (!shouldUse && (systemShip.system.state == ShipSystemAPI.SystemState.IN || systemShip.system.state == ShipSystemAPI.SystemState.ACTIVE)) {
        systemShip.useSystem()
      }
    }

    fun applyImpulse(entity: CombatEntityAPI, applyPoint: Vector2f?, applyAngle: Float, impulse: Float) {
      var entity = entity
      if (entity is ShipAPI && entity.isStationModule) entity = entity.parentStation
      val radius = Math.max(1f, getShipLength(entity))
      val distToApplyPoint = Math.min(MathUtils.getDistance(entity.location, applyPoint), radius)
      val angleToApplyPoint = MathUtils.getShortestRotation(VectorUtils.getAngle(entity.location, applyPoint), applyAngle)
      val mass = Math.max(1f, entity.mass)
      //addDebugText(""+radius);
      val angularMomentum = impulse * distToApplyPoint * FastTrig.sin(Math.toRadians(angleToApplyPoint.toDouble())).toFloat()
      val angularSpeedChange = angularMomentum / (1f / 2f * mass * radius + 1f)
      entity.angularVelocity = limitToTop(entity.angularVelocity + angularSpeedChange, 720f, -720f)
      val addVel = Speed2Velocity(applyAngle, Math.max(1f, radius - distToApplyPoint) * impulse / (mass * radius + 1f))
      var newVel = Vector2f(entity.velocity.x + addVel.x, entity.velocity.y + addVel.y)
      newVel = Velocity2Speed(newVel)
      newVel.setY(Math.min(1000f, newVel.y))
      newVel = Speed2Velocity(newVel)
      entity.velocity.set(newVel)
    }

    fun getShipLength(s: CombatEntityAPI): Float {
      if (s is ShipAPI && s.getExactBounds() != null) {
        var toReturn = 1f
        for (seg in s.getExactBounds().segments) {
          val range = MathUtils.getDistance(seg.p2, s.getLocation())
          if (toReturn < range) toReturn = range
        }
        return toReturn
      }
      return s.collisionRadius + 1f
    }

    @JvmStatic
    fun applyImpulse(entity: CombatEntityAPI, applyAngle: Float, impulse: Float) {
      val mass = Math.max(1f, entity.mass)
      val addVel = Speed2Velocity(applyAngle, impulse / mass)
      var newVel = Vector2f(entity.velocity.x + addVel.x, entity.velocity.y + addVel.y)
      newVel = Velocity2Speed(newVel)
      newVel.setY(Math.min(1000f, newVel.y))
      newVel = Speed2Velocity(newVel)
      entity.velocity.set(newVel)
    }

    fun getEveryFrameScriptWithClass(clazz: Class<*>?): EveryFrameScript? {
      for (EFS in Global.getSector().scripts) {
        if (EFS.javaClass.isInstance(clazz)) {
          return EFS
        }
      }
      return null
    }

    @JvmStatic
    fun isWithinArc(target: ShipAPI?, weapon: WeaponAPI): Boolean {
      return CollisionUtils.getCollisionPoint(weapon.location, getExtendedLocationFromPoint(weapon.location, weapon.currAngle, weapon.range), target) != null
    }

    fun getLongestNormalWeapon(ship: ShipAPI): WeaponAPI? {
      var longest = 0f
      var toReturn: WeaponAPI? = null
      for (w in ship.allWeapons) {
        if (isNormalWeaponType(w, false)) {
          if (w.range > longest) {
            longest = w.range
            toReturn = w
          }
        }
      }
      return toReturn
    }

    fun addDebugText(input: String?) {
      Global.getCombatEngine().addFloatingText(Global.getCombatEngine().playerShip.location, input, 20f, Color(100, 100, 100, 100), Global.getCombatEngine().playerShip, 1f, 1f)
    }

    fun addDebugLog(input: String?) {
      Global.getLogger(this.javaClass).info(input)
    }

    fun addDebugText(input: String?, loc: Vector2f?) {
      Global.getCombatEngine().addFloatingText(loc, input, 20f, Color(100, 100, 100, 100), null, 1f, 1f)
    }

    fun addDebugPoint(loc: Vector2f?){
      MagicRender.battlespace(Global.getSettings().getSprite("graphics/fx/hit_glow.png"),
        loc,
        Vector2f(0f, 0f),
        Vector2f(24f, 24f),
        Vector2f(0f, 0f),
        0f,
        0f,
        Color(100, 250, 100, 120),
        true, 0f, 0f, 0.1f
      )
    }

    fun getColorWithAlphaChange(ori: Color, alphaLevel: Float): Color {
      return Color(ori.red, ori.green, ori.blue, (ori.alpha * alphaLevel).toInt().coerceAtMost(255))
    }

    fun getColorWithAlpha(ori: Color, alphaLevel: Float): Color {
      return Color(ori.red, ori.green, ori.blue, MathUtils.clamp((255f * alphaLevel).toInt(),0,255))
    }

    fun getColorWithChange(ori: Color, alphaLevel: Float): Color {
      return Color((ori.blue * alphaLevel).toInt(), (ori.blue * alphaLevel).toInt(), (ori.blue * alphaLevel).toInt(), (ori.alpha * alphaLevel).toInt())
    }

    fun isHitMissileAndShip(toCheck: CollisionClass): Boolean {
      return EnumSet.of(CollisionClass.MISSILE_FF, CollisionClass.MISSILE_NO_FF, CollisionClass.SHIP, CollisionClass.FIGHTER, CollisionClass.ASTEROID).contains(toCheck)
    }

    fun exponentialIncreaseSmooth(`in`: Float): Float {
      return `in` * `in`
    }

    fun exponentialDecreaseSmooth(`in`: Float): Float {
      return 1 - (1 - `in`) * (1 - `in`)
    }

    fun getPointRotateVector(point: Vector2f?, center: Vector2f, angle: Float): Vector2f {
      val dist = MathUtils.getDistance(point, center)
      var angleNow = VectorUtils.getAngle(center, point)
      angleNow = angleAdd(angleNow, angle)
      return getExtendedLocationFromPoint(center, angleNow, dist)
    }

    fun getPointRotateVector(point: Vector2f?, dist: Float, center: Vector2f, angle: Float): Vector2f {
      var angleNow = VectorUtils.getAngle(center, point)
      angleNow = angleAdd(angleNow, angle)
      return getExtendedLocationFromPoint(center, angleNow, dist)
    }

    fun getHitPoint(t: CombatEntityAPI, range: Float, facing: Float, point: Vector2f): Vector2f? {
      var toReturn: Vector2f? = null
      val checkPoint = getExtendedLocationFromPoint(point, facing, range)
      if (t is ShipAPI) {
        if (t.getShield() != null && t.getShield().isOn && t.getShield().isWithinArc(checkPoint) && MathUtils.getDistance(checkPoint, t.getLocation()) < t.getShield().radius) return checkPoint
      }
      toReturn = CollisionUtils.getCollisionPoint(point, checkPoint, t)
      return toReturn
    }

    @JvmStatic
    fun getFighterReplaceRate(reduceAmount: Float, ship: ShipAPI): Float {
      var totalFFR = 0f
      var num = 0
      for (bay in ship.launchBaysCopy) {
        num += 1
        val rate = Math.max(0.35f, bay.currRate - reduceAmount)
        bay.currRate = rate
        totalFFR += bay.currRate
      }
      return totalFFR / num
    }

    @JvmStatic
    fun getAmount(ship: ShipAPI?): Float {
      if (Global.getCombatEngine().isPaused) return 0f
      val engine = Global.getCombatEngine()
      return if (ship == null) engine.elapsedInLastFrame * engine.timeMult.modifiedValue else engine.elapsedInLastFrame * engine.timeMult.modifiedValue * ship.mutableStats.timeMult.modifiedValue
    }

    fun rotateVector(`in`: Vector2f?, toAngle: Float, speed: Float, amount: Float): Vector2f {
      val angleNow = VectorUtils.getFacing(`in`)
      var turnAngle = MathUtils.getShortestRotation(angleNow, toAngle)
      val turnAngleABS = Math.abs(turnAngle)
      turnAngle = turnAngle / turnAngleABS * Math.min(turnAngleABS, speed * amount)
      return VectorUtils.rotate(`in`, turnAngle)
    }

    //exclude fighters
    fun findNearestEnemyShip(entity: CombatEntityAPI): ShipAPI? {
      var closest: ShipAPI? = null
      var distance: Float
      var closestDistance = Float.MAX_VALUE
      for (tmp in AIUtils.getEnemiesOnMap(entity)) {
        if (tmp.isFighter || tmp.owner == entity.owner) continue
        distance = MathUtils.getDistance(tmp, entity.location)
        if (distance < closestDistance) {
          closest = tmp
          closestDistance = distance
        }
      }
      return closest
    }

    fun findNearestEnemyFighter(entity: CombatEntityAPI): ShipAPI? {
      var closest: ShipAPI? = null
      var distance: Float
      var closestDistance = Float.MAX_VALUE
      for (tmp in AIUtils.getEnemiesOnMap(entity)) {
        if (!tmp.isFighter || tmp.owner == entity.owner) continue
        distance = MathUtils.getDistance(tmp, entity.location)
        if (distance < closestDistance) {
          closest = tmp
          closestDistance = distance
        }
      }
      return closest
    }

    fun isEntityHit(entity: CombatEntityAPI): Boolean {
      var distance: Float
      for (tmp in Global.getCombatEngine().ships) {
        if (tmp.isFighter) continue
        if (tmp === entity || tmp.isHulk || tmp.isShuttlePod || !tmp.isAlive || !Global.getCombatEngine().isEntityInPlay(tmp)) continue
        if (tmp.collisionClass != CollisionClass.SHIP) continue
        distance = MathUtils.getDistance(tmp, entity.location)
        if (distance > tmp.collisionRadius) continue
        if (CollisionUtils.isPointWithinBounds(entity.location, tmp)) {
          return true
        }
      }
      return false
    }

    //angle, length
    fun getAngleAndDist(point: Vector2f, center: Vector2f, centerFacing: Float): Vector2f {
      val x = -VectorUtils.getAngle(center, point) - 90 - centerFacing
      val dist = Vector2f(point.x - center.x, point.y - center.y)
      val y = Math.sqrt((dist.getX() * dist.getX() + dist.getY() * dist.getY()).toDouble()).toFloat()
      return Vector2f(x, y)
    }

    fun getAbsPos(angleAndLength: Vector2f, basePoint: Vector2f): Vector2f {
      val angle = angleAndLength.x
      val length = angleAndLength.y
      val xAxis = FastTrig.cos(Math.toRadians(angle.toDouble())).toFloat() * length
      val yAxis = FastTrig.sin(Math.toRadians(angle.toDouble())).toFloat() * length
      return Vector2f(xAxis + basePoint.x, yAxis + basePoint.y)
    }

    fun firingSmoke(loc: Vector2f, facing: Float, param:FiringSmokeParam, ship: ShipAPI?) {
      val engine = Global.getCombatEngine()

      val smokeSize = param.smokeSize
      val smokeRange = param.smokeSizeRange
      val smokeEndSizeMult = param.smokeEndSizeMult

      val smokeSpread = param.smokeSpread
      val maxSpreadRange = param.maxSpreadRange

      val smokeTime = param.smokeTime

      val smokeColor = param.smokeColor
      val smokeAlphaMult = param.smokeAlpha
      val num: Int = param.smokeNum.toInt()

      val smokeStopSpeed = param.smokeStopSpeed
      val smokeSpeed = param.smokeInitSpeed

      //add cloud
      var i = 0
      while (i < num) {
        val size = smokeSize + MathUtils.getRandomNumberInRange(-smokeRange, smokeRange)
        val angleRandom = MathUtils.getRandomNumberInRange(-smokeSpread / 2f, smokeSpread / 2f)
        val angleMult = (smokeSpread / 2f - Math.abs(angleRandom)) / (1 + smokeSpread / 2f)

        val spawnDist = MathUtils.getRandomNumberInRange(0f, maxSpreadRange)
        val ms = aEP_MovingSmoke(getExtendedLocationFromPoint(loc, facing + angleRandom, spawnDist))
        ms.velocity = Speed2Velocity(facing+angleRandom, smokeSpeed * angleMult * MathUtils.getRandom().nextFloat())
        ms.angleSpeed = MathUtils.getRandomNumberInRange(-15f,15f)
        ms.lifeTime = smokeTime
        ms.fadeIn = 0f
        ms.fadeOut = 0.8f
        ms.size = size
        ms.sizeChangeSpeed = (smokeEndSizeMult-1) * size / smokeTime
        ms.color = Color(smokeColor.red, smokeColor.green,smokeColor.blue,(smokeColor.alpha * smokeAlphaMult).toInt())
        ms.setInitVel(ship?.velocity?:Vector2f(0f,0f))
        ms.stopSpeed = smokeStopSpeed
        aEP_CombatEffectPlugin.addVE(ms)
        i ++
      }
    }

    fun isShipInPlayerFleet(ship:ShipAPI,alwaysTrueIfNotCampaign:Boolean): Boolean{
      if(Global.getCombatEngine().isInCampaign && Global.getSector().playerFleet.fleetData.membersListCopy.contains(ship.fleetMember)){
        return true
      }
      if(!Global.getCombatEngine().isInCampaign && alwaysTrueIfNotCampaign){
        return true
      }
      return false
    }

    fun getShipSpecName(spec:String):String{
      return Global.getSettings().getHullSpec(spec).hullName
    }

    fun getHullModName(spec: String):String{
      return Global.getSettings().getHullModSpec(spec).displayName
    }

    fun checkTargetWithinSystemRange(ship: ShipAPI?, baseRange: Float): Boolean{
      val range = ship?.mutableStats?.systemRangeBonus?.computeEffective(baseRange) ?: -9999999999f
      if(ship?.shipTarget != null) {
        if(MathUtils.getDistance(ship.shipTarget.location,ship.location) - ship.collisionRadius< range){
          return true
        }
      }
      return false
    }

    fun txtOfTargetWithinSystemRange(ship: ShipAPI?, baseRange: Float): String{
      val range = ship?.mutableStats?.systemRangeBonus?.computeEffective(baseRange) ?: -9999999999f
      if(ship?.shipTarget != null) {
        if(MathUtils.getDistance(ship.shipTarget.location,ship.location) - ship.collisionRadius< range){
          return ""
        }
      }
      return "Out of Range"
    }

    fun checkMouseWithinSystemRange(ship: ShipAPI?, baseRange: Float): Boolean{
      val range = ship?.mutableStats?.systemRangeBonus?.computeEffective(baseRange) ?: -9999999999f
      if(ship?.mouseTarget != null) {
        if(MathUtils.getDistance(ship.mouseTarget,ship.location) - ship.collisionRadius< range){
          return true
        }
      }
      return false
    }

    fun txtOfMouseWithinSystemRange(ship: ShipAPI?, baseRange: Float): String{
      val range = ship?.mutableStats?.systemRangeBonus?.computeEffective(baseRange) ?: -9999999999f
      if(ship?.mouseTarget != null) {
        if(MathUtils.getDistance(ship.mouseTarget,ship.location) - ship.collisionRadius< range){
          return ""
        }
      }
      return "Out of Range"
    }

    fun getSystemRange(ship: ShipAPI?, baseRange: Float):Float{
      val range = ship?.mutableStats?.systemRangeBonus?.computeEffective(baseRange) ?: baseRange
      return range
    }

    fun getRealDissipation(ship: ShipAPI?):Float{
      ship?:return 0f
      return ship.mutableStats.fluxDissipation.modifiedValue ?:0f
    }

    fun spawnCompositeSmoke(loc: Vector2f, radius: Float, lifeTime:Float, color:Color?){
      val c = color?: Color(240,240,240)
      Global.getCombatEngine().addNebulaSmokeParticle(loc,
        Vector2f(0f,0f),
        radius,
        1f,
        0f,
        0f,
        lifeTime,
        getColorWithAlpha(c, 0.2f *(c.alpha/255f))
      )


      Global.getCombatEngine().addNebulaSmokeParticle(loc,
        Vector2f(0f,0f),
        radius,
        1f,
        0f,
        0f,
        lifeTime,
        getColorWithAlpha(c, 0.2f *(c.alpha/255f)))

      val smoke = aEP_MovingSmoke(loc)
      smoke.size = radius * 3f/4f
      smoke.fadeIn = 0.1f
      smoke.fadeOut = 0.8f
      smoke.lifeTime = lifeTime
      smoke.sizeChangeSpeed = 0f
      smoke.color = getColorWithAlpha(c, 0.4f *(c.alpha/255f))
      aEP_CombatEffectPlugin.addVE(smoke)

      //生成烟雾
      //生成雾气
      var i = 0
      while (i < 360) {
        val p = getExtendedLocationFromPoint(loc, i.toFloat(), radius/2f)
        val smoke = aEP_MovingSmoke(p)
        smoke.size = radius/2f
        smoke.fadeIn = 0.1f
        smoke.fadeOut = 0.8f
        smoke.lifeTime = lifeTime
        smoke.sizeChangeSpeed = -(smoke.size/4f)/lifeTime
        smoke.setInitVel(Speed2Velocity(i.toFloat(),smoke.sizeChangeSpeed/2f))
        smoke.color = getColorWithAlpha(c, 0.2f *(c.alpha/255f))
        aEP_CombatEffectPlugin.addVE(smoke)
        i += 30
      }
    }

    fun spawnSingleCompositeSmoke(loc: Vector2f, radius: Float, lifeTime:Float, color:Color?){
      val c = color?: Color(240,240,240)
      Global.getCombatEngine().addNebulaSmokeParticle(loc,
        Vector2f(0f,0f),
        radius,
        1.2f,
        0f,
        0f,
        lifeTime,
        getColorWithAlpha(c, 0.35f *(c.alpha/255f)))
      val smoke = aEP_MovingSmoke(loc)
      smoke.size = radius/2f
      smoke.fadeIn = 0.1f
      smoke.fadeOut = 0.8f
      smoke.lifeTime = lifeTime
      smoke.sizeChangeSpeed = (smoke.size/4f)/lifeTime
      smoke.color = getColorWithAlpha(c, 0.65f *(c.alpha/255f))
      aEP_CombatEffectPlugin.addVE(smoke)

    }
  }
  class FiringSmokeParam{
    var smokeSize = 0f
    var smokeEndSizeMult = 1f
    var smokeSizeRange = 0f
    //角度散步，不能为0，否则烟雾不动
    var smokeSpread = 1f
    var smokeTime = 0f
    //出生距离，会在0到这个范围内刷出烟
    var maxSpreadRange = 0f
    var smokeColor = Color(230,230,230)
    var smokeAlpha = 1f
    var smokeNum = 0

    var smokeInitSpeed = 100f
    //每0.1秒速度乘一次这个系数
    var smokeStopSpeed = 0.9f
  }
}