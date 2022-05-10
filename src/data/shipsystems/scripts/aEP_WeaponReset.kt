package data.shipsystems.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipSystemAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import combat.impl.VEs.aEP_MovingSmoke
import combat.plugin.aEP_CombatEffectPlugin.Mod.addVE
import combat.util.aEP_Tool
import combat.util.aEP_Tool.Util.Speed2Velocity
import combat.util.aEP_Tool.Util.getAmount
import combat.util.aEP_Tool.Util.getExtendedLocationFromPoint
import combat.util.aEP_Tool.Util.getTargetWidthAngleInDistance
import combat.util.aEP_Tool.Util.getWeaponOffsetInAbsoluteCoo
import combat.util.aEP_Tool.Util.isNormalWeaponSlotType
import org.lazywizard.lazylib.CollisionUtils
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.round

class aEP_WeaponReset: BaseShipSystemScript() {

  val FLUX_REDUCTION = 0f //by percent
  private val ROF_BONUS = 100f //extra percent

  //about visual effect
  private val EMP_ARC_CHANCE_PER_SECOND = 15f
  private val ARC_FRINGE_COLOR = Color(240, 100, 100, 240)
  private val ARC_CORE_COLOR = Color(200, 200, 200, 120)
  private val JITTER_COLOR = Color(240, 50, 50, 125)

  private val spreadRange: MutableMap<WeaponSize, Float> = HashMap()
  private val timeRange: MutableMap<WeaponSize, Float> = HashMap()
  private val FLUX_DECREASE_PERCENT: MutableMap<String, Float> = HashMap()
  private val FLUX_RETURN_SPEED: MutableMap<String, Float> = HashMap()
  init {
    spreadRange[WeaponSize.LARGE] = 10f
    spreadRange[WeaponSize.MEDIUM] = 7.5f
    spreadRange[WeaponSize.SMALL] = 5f

    timeRange[WeaponSize.LARGE] = 0.5f
    timeRange[WeaponSize.MEDIUM] = 0.4f
    timeRange[WeaponSize.SMALL] = 0.3f

    FLUX_DECREASE_PERCENT["aEP_XiLiu"] = 0.75f
    FLUX_DECREASE_PERCENT["aEP_CengLiu"] = 0.5f

    FLUX_RETURN_SPEED["aEP_XiLiu"] = 1f
    FLUX_RETURN_SPEED["aEP_CengLiu"] = 2f
  }
  var didActive = false
  private var storedHardFlux = 0f
  private var storedSoftFlux = 0f

  override fun apply(stats: MutableShipStatsAPI, id: String?, state: ShipSystemStatsScript.State?, effectLevel: Float) {
    val ship = stats.entity as ShipAPI
    if (!ship.isAlive) {
      return
    }

    //激活中
    if(state == ShipSystemStatsScript.State.IN || state == ShipSystemStatsScript.State.ACTIVE || state == ShipSystemStatsScript.State.OUT){

      //执行一次
      if(!didActive){
        didActive = true
      }

      //吸收幅能
      if(state == ShipSystemStatsScript.State.ACTIVE || state == ShipSystemStatsScript.State.OUT){
        val hard = (ship.fluxTracker.hardFlux)
        val soft = (ship.fluxTracker.currFlux - hard)
        val speedPercent = FLUX_DECREASE_PERCENT[ship.hullSpec.hullId]?:0.5f
        //吸收幅能，速度为当前幅能的百分比
        var toReturnThisFrame = speedPercent * ship.currFlux * getAmount(ship) / (ship.system.chargeActiveDur + ship.system.chargeDownDur)
        if(soft > 0){
          var toAdd = toReturnThisFrame.coerceAtMost(soft)
          ship.fluxTracker.increaseFlux(-toAdd,false)
          toReturnThisFrame +=toAdd
          storedSoftFlux +=toAdd
        }
        if(hard > 0){
          var toAdd = toReturnThisFrame.coerceAtMost(hard)
          ship.fluxTracker.increaseFlux(-toAdd,true)
          toReturnThisFrame +=toAdd
          storedHardFlux += toAdd
        }
      }

      ship.isJitterShields = false
      ship.setJitter(ship, JITTER_COLOR, effectLevel, 1, 5f)
      ship.setJitterUnder(ship, JITTER_COLOR, effectLevel, 8, 20f)
      for (w in ship.allWeapons) {
        if (isNormalWeaponSlotType(w.slot, false) && Math.random() < EMP_ARC_CHANCE_PER_SECOND * getAmount(null)) {
          for (offset in getWeaponOffsetInAbsoluteCoo(w)) {
            val vel = Speed2Velocity(w.currAngle, MathUtils.getRandomNumberInRange(20, 120).toFloat())
            val shipVel = ship.velocity
            Global.getCombatEngine().addSmoothParticle(
              getExtendedLocationFromPoint(offset, w.currAngle + 90f, MathUtils.getRandomNumberInRange(-spreadRange[w.size]!!, spreadRange[w.size]!!)),  //loc
              Vector2f(vel.getX() + shipVel.getX(), vel.getY() + shipVel.getY()),  //vel
              MathUtils.getRandomNumberInRange(spreadRange[w.size]!! / 4f, spreadRange[w.size]!!),  //size
              MathUtils.getRandomNumberInRange(0.7f, 1f),  //brightness
              timeRange[w.size]!!,  //duration
              ARC_FRINGE_COLOR
            )
          }
        }
      }

      //ballistic weapon buff
      stats.ballisticRoFMult.modifyPercent(id, ROF_BONUS * effectLevel)
      stats.ballisticAmmoRegenMult.modifyPercent(id, ROF_BONUS * effectLevel)

      //energy weapon buff
      stats.energyRoFMult.modifyPercent(id, ROF_BONUS * effectLevel)
      stats.energyAmmoRegenMult.modifyPercent(id, ROF_BONUS * effectLevel)

      //non beam PD buff
      //stats.getNonBeamPDWeaponRangeBonus().modifyFlat(id,PD_RANGE_BONUS - RANGE_BONUS);

      //flux consume reduce
      stats.ballisticWeaponFluxCostMod.modifyPercent(id, -FLUX_REDUCTION)
      stats.energyWeaponRangeBonus.modifyPercent(id, -FLUX_REDUCTION)
    } else{
      //激活结束运行一次
      if(didActive){
        didActive = false
        //ballistic weapon buff
        stats.ballisticRoFMult.unmodify(id)
        stats.ballisticAmmoRegenMult.unmodify(id)

        //energy weapon buff
        stats.energyRoFMult.unmodify(id)
        stats.energyAmmoRegenMult.unmodify(id)

        //non beam PD buff
        //stats.getNonBeamPDWeaponRangeBonus().modifyFlat(id,0);

        //flux consume reduce
        stats.ballisticWeaponFluxCostMod.unmodify(id)
        stats.energyWeaponRangeBonus.unmodify(id)

        spawnSmoke(ship, 30)
      }
      //返还幅能
      var toReturnThisFrame = aEP_Tool.getRealDissipation(ship) * getAmount(ship) * (FLUX_RETURN_SPEED[ship.hullSpec.hullId]?:2f)
      if(storedSoftFlux > 0){
        var toAdd = toReturnThisFrame.coerceAtMost(storedSoftFlux)
        toAdd = toAdd.coerceAtMost(ship.maxFlux - ship.currFlux)
        ship.fluxTracker.increaseFlux(toAdd,false)
        toReturnThisFrame -= toAdd
        storedSoftFlux -= toAdd
      }
      if(storedHardFlux > 0){
        var toAdd = toReturnThisFrame.coerceAtMost(storedHardFlux)
        toAdd = toAdd.coerceAtMost(ship.maxFlux - ship.currFlux)
        ship.fluxTracker.increaseFlux(toAdd,true)
        toReturnThisFrame -=toAdd
        storedHardFlux -=toAdd
      }

    }

  }


  override fun unapply(stats: MutableShipStatsAPI, id: String?) {
    val ship = stats.entity as ShipAPI


  }

  override fun getStatusData(index: Int, state: ShipSystemStatsScript.State?, effectLevel: Float): StatusData? {
    if(state == ShipSystemStatsScript.State.IN || state == ShipSystemStatsScript.State.ACTIVE || state == ShipSystemStatsScript.State.OUT) {
      if (index == 0) {
        return  StatusData("Rate of Fire increased" + ": " + (effectLevel * ROF_BONUS).toInt() + "%", false)
      }
    }
    return  null
  }

  override fun getInfoText(system: ShipSystemAPI?, ship: ShipAPI?): String {
    return "Stored: "+ storedSoftFlux.toInt()+" / "+ storedHardFlux.toInt()
  }
  

  fun spawnSmoke(ship: ShipAPI, minSmokeDist: Int) {
    var moveAngle = 0f
    val angleToTurn = getTargetWidthAngleInDistance(ship.location, getExtendedLocationFromPoint(ship.location, 0f, ship.collisionRadius), minSmokeDist.toFloat())
    while (moveAngle < 360f) {
      val outPoint = CollisionUtils.getCollisionPoint(getExtendedLocationFromPoint(ship.location, moveAngle, ship.collisionRadius + 10), ship.location, ship)
      val lifeTime = 2f
      val extendRange = 0.5f
      val speed = Speed2Velocity(VectorUtils.getAngle(ship.location, outPoint), extendRange * ship.collisionRadius)
      val ms = aEP_MovingSmoke(outPoint!!)
      ms.lifeTime = lifeTime
      ms.fadeIn = 0.25f
      ms.fadeOut = 0.5f
      ms.setInitVel(speed)
      ms.size = minSmokeDist * 3f
      ms.sizeChangeSpeed = minSmokeDist * extendRange * 3f / lifeTime
      ms.color = Color(200, 200, 200, 80)
      ms.stopSpeed = 0.75f
      addVE(ms)
      moveAngle = moveAngle + angleToTurn
    }
    moveAngle = 0f
    while (moveAngle < 360f) {
      val outPoint = CollisionUtils.getCollisionPoint(getExtendedLocationFromPoint(ship.location, moveAngle, ship.collisionRadius + 10), ship.location, ship)
      val lifeTime = 2f
      val extendRange = 0.5f
      val speed = Speed2Velocity(VectorUtils.getAngle(ship.location, outPoint), extendRange * ship.collisionRadius + minSmokeDist * 6f)
      val ms = aEP_MovingSmoke(outPoint!!)
      ms.lifeTime = lifeTime
      ms.fadeIn = 0.25f
      ms.fadeOut = 0.5f
      ms.setInitVel(speed)
      ms.size = minSmokeDist * 6f
      ms.sizeChangeSpeed = minSmokeDist * extendRange * 6f / lifeTime
      ms.color = Color(200, 200, 200, 80)
      ms.stopSpeed = 0.75f
      addVE(ms)
      moveAngle = moveAngle + angleToTurn
    }
  }
}
