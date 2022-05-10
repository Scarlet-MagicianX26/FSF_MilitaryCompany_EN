package data.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BuffManagerAPI.Buff
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import combat.util.aEP_Tool
import java.util.ArrayList
import java.util.HashMap

class aEP_Tugboat: aEP_BaseHullMod() {

  companion object{
    const val ID = "aEP_Tugboat"
  }

  override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
    if(member?.fleetData?.fleet == null) return
    val allMemberList = member.fleetData.combatReadyMembersListCopy
    val allTowerList = ArrayList<FleetMemberAPI>()
    val toRemoveList = ArrayList<FleetMemberAPI>()
    for(m in allMemberList){
      //移除掉本身就有这个船插的船
      if(m.variant.hasHullMod(ID)){
        //封存状态的拖船不能拖
        if(!m.isMothballed){
          allTowerList.add(m)
        }
        toRemoveList.add(m)
      }
    }
    allMemberList.removeAll(toRemoveList)
    //Comparator返回负数即不需要交换，1在2前面。返回整数相反。
    //这里，如果1大于2，返回整数需要交换，把大的放在后面
    val comparator = Comparator<FleetMemberAPI>(function = fun (m1:FleetMemberAPI,m2:FleetMemberAPI) : Int{
      if(m1.stats.maxBurnLevel.modifiedValue > m2.stats.maxBurnLevel.modifiedValue){
        return 1
      }
      return -1
    })
    //排序，速度小到大
    allTowerList.sortWith(comparator)
    allMemberList.sortWith(comparator)

    //船队中没有船就直接return
    if(allTowerList.size <= 0|| allMemberList.size <= 0){
      return
    }

    //用最慢的拖船去拖最慢的成员
    //因为拖船之间不能相互拖，所以最慢的拖船决定了舰队的速度上限
    for (m in allTowerList){
      //val speedNow = member.stats.maxBurnLevel.modifiedValue
      //val slowestSpeed = toRemove.stats.maxBurnLevel.modifiedValue
      //先给最慢的成员加buff，再把已经被拖的成员从list中移除，下一个拖船会碰到第二慢的成员
      //不能重复上buff，会弹出，先检测
      //船队中没有船就直接return
      //不知道为啥上面isEmpty()了下面还是会有问题，总之再判断一次
      if(allMemberList.size >= 1){
        var toRemove: FleetMemberAPI = allMemberList[0]?:return
        if(toRemove.buffManager.getBuff(ID) == null){
          toRemove.buffManager.addBuff(TowCableBuff(ID,m,toRemove))
        }
        allMemberList.removeAt(0)
      }

    }
    //Global.getLogger(this.javaClass).info(test.toString())
  }

  class TowCableBuff(  //public boolean wasApplied = false;
    val buffId: String, val tug: FleetMemberAPI, val slowest: FleetMemberAPI
  ) : Buff {
    private var frames = 0
    override fun isExpired(): Boolean {
      if(frames >= 1){
        slowest.stats.maxBurnLevel.unmodify(buffId)
        return true
      }
      return false
    }

    override fun getId(): String {
      return buffId
    }

    override fun apply(member: FleetMemberAPI) {
      if(tug.stats.maxBurnLevel.modifiedValue > member.stats.maxBurnLevel.modifiedValue)
        member.stats.maxBurnLevel.modifyFlat(buffId, tug.stats.maxBurnLevel.modifiedValue- member.stats.maxBurnLevel.modified)
    }

    override fun advance(days: Float) {
      frames++
    }
  }
}


