package data.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import combat.util.aEP_DataTool
import combat.util.aEP_Tool
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.round

class aEP_RapidDissipate internal constructor() : aEP_BaseHullMod() {

  companion object {
    const val REVERSE_PERCENT = 0.5f
    const val MAX_PERCENT = 0.9f
    const val CONVERT_SPEED_PER_CAP = 8f
    const val HARD_DISS_PER_CAP = 8f
    const val DAMAGE_CONVERTED = 0.75f
    var id = "aEP_RapidDissipate"
  }

  override fun applyEffectsAfterShipCreationImpl(ship: ShipAPI?, id: String?) {
    ship ?: return
    if(!ship.hasListenerOfClass(DamageTaken::class.java)){
      ship.addListener(DamageTaken(ship))
    }
  }

  override fun advanceInCombat(ship: ShipAPI, amount: Float) {
    val bufferLevel = aEP_MarkerDissipation.getBufferLevel(ship)
    if (ship.variant.numFluxCapacitors <= 0) {
      return
    }
    val numOfCap = ship.variant.numFluxCapacitors
    val fluxVent = numOfCap * CONVERT_SPEED_PER_CAP
    //val soft = ship.fluxTracker.maxFlux - ship.fluxTracker.hardFlux
    val hard = ship.fluxTracker.hardFlux
    //val hardPercent = MathUtils.clamp(hard / (ship.fluxTracker.maxFlux * MAX_PERCENT), 0f, 1f)
    //ship.getFluxTracker().setHardFlux(Math.max(hard-bufferLevel*fluxVent*amount,0));
    if(ship.hasListenerOfClass(DamageTaken::class.java)){
      val listener = ship.getListeners(DamageTaken::class.java)[0]
      listener.convertPercnet = 0f
      if (bufferLevel > 0.99f) {
        listener.convertPercnet = DAMAGE_CONVERTED
      }
    }


    //aEP_Tool.addDebugText(bufferLevel+"");
    var addOrReduce = "不变"
    var isDebuff = false
    var useLevel = 0f
    //先取消加成，用于计算修改前剩余的幅散，防止减到负数
    ship.mutableStats.fluxDissipation.modifyFlat(id, 0f)
    //过载不计入
    if (ship.fluxTracker.isOverloadedOrVenting) {
      return
    }
    if (ship.fluxLevel <= REVERSE_PERCENT) {
      useLevel = 1 - ship.fluxLevel * (1f / REVERSE_PERCENT)
      ship.mutableStats.fluxDissipation.modifyFlat(id, -Math.min(useLevel * fluxVent, ship.mutableStats.fluxDissipation.modifiedValue))
      isDebuff = true
      addOrReduce = aEP_DataTool.txt("reduce")
    } else {
      useLevel = (ship.fluxLevel - REVERSE_PERCENT) * (1f / REVERSE_PERCENT)
      ship.mutableStats.fluxDissipation.modifyFlat(id, 0f)
      ship.mutableStats.fluxDissipation.modifyFlat(id, useLevel * fluxVent)
      isDebuff = false
      addOrReduce = aEP_DataTool.txt("add")
    }
    if (Global.getCombatEngine().playerShip === ship) {
      Global.getCombatEngine().maintainStatusForPlayerShip(
        this.javaClass.simpleName,  //key
        Global.getSettings().getHullModSpec(id).spriteName,  //sprite name,full, must be registed in setting first
        Global.getSettings().getHullModSpec("aEP_RapidDissipate").displayName,  //title
        aEP_DataTool.txt("flux_diss") + addOrReduce + ": " + (useLevel * fluxVent).toInt(),  //data
        isDebuff
      ) //is debuff
    }
  }

  override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
    return if (index == 0) CONVERT_SPEED_PER_CAP.toInt().toString() + "" else null
  }

  override fun addPostDescriptionSection(tooltip: TooltipMakerAPI?, hullSize: HullSize?, ship: ShipAPI?, width: Float, isForModSpec: Boolean) {
    tooltip?: return
    ship?: return
    tooltip.addSectionHeading(aEP_DataTool.txt("when_soft_up"), Alignment.MID, 5f)
    val image = tooltip.beginImageWithText(Global.getSettings().getHullModSpec("aEP_RapidDissipate").spriteName, 48f)
    image.addPara("- " + aEP_DataTool.txt("aEP_RapidDissipate01") , 5f, Color.white, Color.green, round(DAMAGE_CONVERTED*100).toString()+"%")
    tooltip.addImageWithText(5f)
  }

  class DamageTaken(val ship: ShipAPI) : DamageTakenModifier{
    var convertPercnet = 0f

    override fun modifyDamageTaken(param: Any?, target: CombatEntityAPI?, damage: DamageAPI?, point: Vector2f?, shieldHit: Boolean): String? {
      damage ?: return null
      if(shieldHit) return null
      var d = (damage.modifier?.modifiedValue?:1f) * damage.damage
      if(damage.type == DamageType.FRAGMENTATION){
        d /= 4f
      } else if(damage.type == DamageType.HIGH_EXPLOSIVE){
        d *= 2f
      } else if(damage.type == DamageType.KINETIC){
        d /= 2f
      }
      ship.fluxTracker.decreaseFlux(d*convertPercnet)
      //Global.getLogger(this.javaClass).info(d)
      return  null
    }
  }

  init {
    notCompatibleList.add("aEP_SoftfluxDissipate")
    haveToBeWithMod.add("aEP_MarkerDissipation")
  }
}