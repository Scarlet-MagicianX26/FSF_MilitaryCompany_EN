package data.hullmods

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import combat.util.aEP_DataTool
import java.awt.Color

class aEP_CruiseMissileCarrier : BaseHullMod() {
  companion object{
    const val CR_THRESHOLD = 0.4f
    const val LOAD_SPEED_PER_DAY = 0.1f
    const val id = "\$aEP_CruiseMissileCarrier"

    fun getNumOfMissileInCargo(member: FleetMemberAPI): Float{
      var haveCMInCargo = 0f
      if (member.fleetData != null && member.fleetData.fleet != null) {
        val fleet = member.fleetData.fleet
        if (fleet.cargo != null) {
          var numOfMissile = 0f
          for (stack in fleet.cargo.stacksCopy) {
            if (stack.specialItemSpecIfSpecial != null && stack.specialItemSpecIfSpecial.id == "aEP_CM") {
              numOfMissile += stack.size
            }
          }
          if (numOfMissile >= 1) {
            haveCMInCargo = numOfMissile.toInt().toFloat()
          }
        }
      }
      return haveCMInCargo
    }

    fun getLoadedMap():HashMap<FleetMemberAPI,Float>{
      if(Global.getSector()?.memoryWithoutUpdate?.get(id) == null){
        Global.getSector()?.memoryWithoutUpdate?.set(id,HashMap<FleetMemberAPI,Float>())
      }
      return (Global.getSector()?.memoryWithoutUpdate?.get(id) as HashMap<FleetMemberAPI,Float>?) ?: HashMap<FleetMemberAPI,Float>()
    }

    //跨存档不变的还有member.getId()
    fun putLoadedNum(member: FleetMemberAPI, num: Float){
      val map = getLoadedMap()
      map[member] = num
    }

    fun getLoadedNum(member: FleetMemberAPI): Float{
      val map = getLoadedMap()
      return (map[member])?: 0f
    }
  }

  override fun advanceInCampaign(member: FleetMemberAPI, amount: Float) {
    //每帧在map内找到对应的member进行load的加减操作
    val dayPassed = Global.getSector().clock.convertToDays(amount)
    val missileInCargo = getNumOfMissileInCargo(member)
    val map = getLoadedMap()
    var loaded = (map[member] ?: 0f)
    //Global.getLogger(this.javaClass).info(loaded)
    if(member.repairTracker.cr > CR_THRESHOLD && missileInCargo >= 1){
      loaded = (loaded + LOAD_SPEED_PER_DAY * dayPassed).coerceAtMost(1f)
    } else{
      if(missileInCargo < 1){
        loaded = 0f
      }else{
        loaded = (loaded - LOAD_SPEED_PER_DAY * dayPassed).coerceAtLeast(0f)
      }
    }
    putLoadedNum(member,loaded)

    //同时loop一下map内的所有船，对于不在任何舰队内的统统清理
    val it = map.iterator()
    val toRemoveList = ArrayList<FleetMemberAPI>()
    while (it.hasNext()){
      val m = it.next().key
      if((m.fleetData?.fleet) == null){
        toRemoveList.add(m)
      }
    }
    for(m in toRemoveList){
      map.remove(m)
    }
  }

  override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String) {
    for (w in ship.allWeapons) {
      if (w.spec.weaponId == "aEP_CM_weapon") {
        //处于生涯战斗时，根据有是否装填好的导弹判断
        //如果在模拟战，直接给弹药
        //此时的ship.fleetMember并不是生涯的member，第一个判断没用
        if (Global.getCombatEngine().isInCampaign && ship.fleetMember != null && getLoadedNum(ship.fleetMember)>=1f) {
          w.maxAmmo = 1
          w.ammo = 1
        } else if(Global.getCombatEngine().isInCampaignSim || Global.getCombatEngine().isSimulation){
          w.maxAmmo = 1
          w.ammo = 1
        }
      }
    }
  }

  //在生涯战斗中，若该舰不存在生涯装填数据，把弹药归0，禁止发射
  override fun advanceInCombat(ship: ShipAPI, amount: Float) {
    if (Global.getCombatEngine() == null || Global.getCombatEngine().isMission) {
      return
    }
    for (w in ship.allWeapons) {
      if (w.spec.weaponId == "aEP_CM_weapon") {
        if (Global.getCombatEngine().isInCampaign && ship.fleetMember != null && getLoadedNum(ship.fleetMember) < 1f) {
          w.maxAmmo = 0
          w.ammo = 0
        }
      }
    }
  }

  override fun shouldAddDescriptionToTooltip(hullSize: HullSize, ship: ShipAPI, isForModSpec: Boolean): Boolean {
    return true
  }

  override fun addPostDescriptionSection(tooltip: TooltipMakerAPI, hullSize: HullSize, ship: ShipAPI, width: Float, isForModSpec: Boolean) {
    if (Global.getSector() == null || Global.getCurrentState() != GameState.CAMPAIGN) {
      tooltip.addPara(aEP_DataTool.txt("CMCarrier01"), Color.green, 10f)
      return
    }
    var percent = (getLoadedMap()[ship.fleetMember])?: 0f
    if (ship.currentCR < CR_THRESHOLD) {
      tooltip.addPara(aEP_DataTool.txt("CMCarrier02"), Color.red, 10f)
    }
    if (percent <= 0) {
      tooltip.addPara(aEP_DataTool.txt("CMCarrier03"), Color.red, 10f)
      return
    }
    if (percent < 1) {
      tooltip.addPara(aEP_DataTool.txt("CMCarrier04") + ": {%s}", 10f, Color.white, Color.yellow, String.format("%.1f", percent * 100) + "%")
      return
    }
    if (percent >= 1) {
      tooltip.addPara(aEP_DataTool.txt("CMCarrier04"), Color.green, 10f)
      return
    }
  }

  class LoadingMissile : EveryFrameScript {
    var isEnd = false
    var loadedNum = 0f
    var lifeTime = 10f
    var fleetMember = ""
    override fun isDone(): Boolean {
      return if (lifeTime <= 0) {
        true
      } else isEnd
    }

    override fun runWhilePaused(): Boolean {
      return false
    }

    override fun advance(amount: Float) {
      if (loadedNum > 1) {
        loadedNum = 1f
      }

      //加入的EveryFrame会每帧自减，如果没有每帧在船插里面每帧续上lifeTime，一段时间会自己结束
      if (!Global.getSector().isPaused) lifeTime = lifeTime - amount
    }
  }

}