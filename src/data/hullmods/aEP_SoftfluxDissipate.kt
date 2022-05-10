package data.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import combat.util.aEP_DataTool
import combat.util.aEP_Tool
import java.awt.Color

class aEP_SoftfluxDissipate internal constructor() : aEP_BaseHullMod() {
  companion object const {
    const val REVERSE_PERCENT = 0.5f
    const val MAX_PERCENT = 1f
    const val BASE_BUFF_PER_VENT = 8f

    //幅能耗散降低
    const val DISSI_DECREASE = 0.15f

    //武器消耗减少
    const val CONVERT_PERCENT = 0.15f
    const val id = "aEP_SoftfluxDissipate"
  }

  init {
    notCompatibleList.add("aEP_RapidDissipate")
    haveToBeWithMod.add("aEP_MarkerDissipation")
  }

  override fun applyEffectsAfterShipCreationImpl(ship: ShipAPI, id: String) {
    val fluxVent = ship.variant.numFluxVents * BASE_BUFF_PER_VENT
    ship.mutableStats.fluxDissipation.modifyFlat(const.id, fluxVent)
  }

  override fun advanceInCombat(ship: ShipAPI, amount: Float) {
    var weaponLevel = aEP_MarkerDissipation.getBufferLevel(ship)
    ship.mutableStats.ballisticWeaponFluxCostMod.modifyMult(id, 1f - CONVERT_PERCENT * weaponLevel)
    ship.mutableStats.energyWeaponRangeBonus.modifyMult(id, 1f - CONVERT_PERCENT * weaponLevel)

    if (ship.variant.numFluxVents <= 0) {
      return
    }
    val fluxVent = ship.variant.numFluxVents * BASE_BUFF_PER_VENT
    val hardPercent = aEP_Tool.limitToTop(ship.fluxTracker.hardFlux / ship.fluxTracker.maxFlux * MAX_PERCENT, 1f, 0f)
    var addOrReduce = "不变"
    var isDebuff = false
    //先把加成归零用来防止减到负数
    ship.mutableStats.fluxDissipation.modifyFlat(id, 0f)
    //过载不计入
    if (ship.fluxTracker.isOverloadedOrVenting) {
      return
    }
    if (hardPercent <= REVERSE_PERCENT) {
      ship.mutableStats.fluxDissipation.modifyFlat(id, fluxVent * (REVERSE_PERCENT - hardPercent) / REVERSE_PERCENT)
      isDebuff = false
      addOrReduce = aEP_DataTool.txt("add")
    } else {
      ship.mutableStats.fluxDissipation.modifyFlat(id, -Math.min(fluxVent * (hardPercent - REVERSE_PERCENT) / (1f - REVERSE_PERCENT), ship.mutableStats.fluxDissipation.modifiedValue))

      isDebuff = true
      addOrReduce = aEP_DataTool.txt("reduce")
    }

    //当舰船属于无盾或者系统禁盾时，无效，取消所有修改
    if(ship.shield == null || ship.shield.type == ShieldAPI.ShieldType.NONE ||
      (!ship.system.specAPI.isShieldAllowed && ship.system.effectLevel > 0.1f)){
      ship.mutableStats.ballisticWeaponFluxCostMod.modifyMult(id, 1f)
      ship.mutableStats.energyWeaponRangeBonus.modifyMult(id, 1f)
      ship.mutableStats.fluxDissipation.modifyFlat(id,0f)
    }

    if (Global.getCombatEngine().playerShip === ship) {
      Global.getCombatEngine().maintainStatusForPlayerShip(
        this.javaClass.simpleName,  //key
        Global.getSettings().getHullModSpec(id).spriteName,  //sprite name,full, must be registed in setting first
        Global.getSettings().getHullModSpec("aEP_SoftfluxDissipate").displayName,  //title
        aEP_DataTool.txt("flux_diss") + addOrReduce + ": " + Math.abs(ship.mutableStats.fluxDissipation.getFlatStatMod(id).value).toInt(),  //data
        isDebuff
      ) //is debuff
    }
  }

  override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
    return if (index == 0) BASE_BUFF_PER_VENT.toInt().toString() + "" else null
  }

  override fun addPostDescriptionSection(tooltip: TooltipMakerAPI?, hullSize: HullSize?, ship: ShipAPI?, width: Float, isForModSpec: Boolean) {
    tooltip?: return
    ship?: return
    tooltip.addSectionHeading(aEP_DataTool.txt("when_soft_up"), Alignment.MID, 5f)
    val image = tooltip!!.beginImageWithText(Global.getSettings().getHullModSpec("aEP_SoftfluxDissipate").spriteName, 48f)
    image.addPara("- " + aEP_DataTool.txt("flux_gen_reduce") + "{%s}", 5f, Color.white, Color.green, (CONVERT_PERCENT * 100).toInt().toString() + "%")
    image.addPara("- " + aEP_DataTool.txt("flux_diss") + aEP_DataTool.txt("reduce") + "{%s}", 5f, Color.white, Color.red, (DISSI_DECREASE * 100).toInt().toString() + "%")
    tooltip!!.addImageWithText(5f)
  }

}