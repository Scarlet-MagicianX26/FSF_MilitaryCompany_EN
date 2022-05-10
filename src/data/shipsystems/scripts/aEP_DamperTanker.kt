package data.shipsystems.scripts

import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import data.scripts.weapons.aEP_FCLAnimation
import org.lazywizard.lazylib.MathUtils
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import java.awt.Color

class aEP_DamperTanker : BaseShipSystemScript() {
  private var ship: ShipAPI? = null
  private val id = "aEP_RLDamper"
  override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
    //复制沾粘这行
    val ship = (stats?.entity?: return)as ShipAPI

    var convertedLevel = effectLevel
    if (state == ShipSystemStatsScript.State.ACTIVE) convertedLevel = 1f
    for (w in ship!!.allWeapons) {
      if (!w.slot.id.startsWith("TOP_LV") && !w.slot.id.startsWith("RL_DECO")) continue
      val anima = w.effectPlugin as aEP_FCLAnimation
      //extend out
      if (convertedLevel < 0.5f) {
        val level = MathUtils.clamp(convertedLevel * 2f, 0f, 1f)
        if (w.spec.weaponId == "aEP_shangshengliu_armor") {
          w.sprite.color = Color(0, 0, 0, 0)
          anima.setMoveToLevel(level)
        }
        if (w.spec.weaponId == "aEP_shangshengliu_armor_dark") {
          w.sprite.color = Color(255, 255, 255)
          anima.setMoveToLevel(level)
        }
      } else {
        val level = MathUtils.clamp(2f - convertedLevel * 2f, 0f, 1f)
        if (w.spec.weaponId == "aEP_shangshengliu_armor") {
          val black = (255 * effectLevel).toInt()
          w.sprite.color = Color(black, black, black)
          anima.setMoveToLevel(level)
        }
        if (w.spec.weaponId == "aEP_shangshengliu_armor_dark") {
          anima.setMoveToLevel(level)
        }
      }
      if (w.slot.id.startsWith("TOP_LV01")) {
        if(state == ShipSystemStatsScript.State.OUT && effectLevel < 0.5f)
          anima.setMoveToLevel(0f)
        else
          anima.setMoveToLevel(1f)
      }
      //开始结束时，首先收起外层
      if (w.slot.id.startsWith("TOP_LV02")) {
        if(state == ShipSystemStatsScript.State.OUT)
          anima.setMoveToLevel(0f)
        else if(effectLevel < 0.5f)
          anima.setMoveToLevel(0.5f)
        else
          anima.setMoveToLevel(1f)
      }
    }

    //modify here
    val toAdd = EFFECT_ARMOR_FLAT_BONUS + ship.hullSpec.armorRating * EFFECT_ARMOR_PERCENT_BONUS
    stats.effectiveArmorBonus.modifyFlat(id, toAdd * effectLevel)
    stats.armorDamageTakenMult.modifyMult(id, (1-ARMOR_DAMAGE_REDUCE * effectLevel))
    stats.hullDamageTakenMult.modifyMult(id, (1-HULL_DAMAGE_REDUCE * effectLevel))
    //ship.getExactBounds().getSegments()
  }

  override fun unapply(stats: MutableShipStatsAPI, id: String) {
    val ship = (stats?.entity?: return)as ShipAPI
    for (w in ship.allWeapons) {
      if (!w.slot.id.startsWith("TOP_LV") && !w.slot.id.startsWith("RL_DECO")) continue
      val anima = w.effectPlugin as aEP_FCLAnimation
      if (w.spec.weaponId == "aEP_shangshengliu_armor") {
        w.sprite.color = Color(0, 0, 0, 0)
        anima.setMoveToLevel(0f)
      }
      if ( w.spec.weaponId == "aEP_shangshengliu_armor_dark") {
        w.sprite.color = Color(0, 0, 0, 0)
        anima.setMoveToLevel(0f)
      }
      if (w.slot.id.startsWith("TOP_LV01")) {
        anima.setMoveToLevel(0f)
      }
      if (w.slot.id.startsWith("TOP_LV02")) {
        anima.setMoveToLevel(0f)
      }

    }

    //modify here
    stats.effectiveArmorBonus.unmodify(id)
    stats.armorDamageTakenMult.unmodify(id)
    stats.hullDamageTakenMult.unmodify(id)
  }

  override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
    ship?: return null
    if (index == 0) {
      val toAdd = EFFECT_ARMOR_FLAT_BONUS + ship!!.hullSpec.armorRating * EFFECT_ARMOR_PERCENT_BONUS
      return StatusData("装甲计算值增加:" + (toAdd * effectLevel).toInt(), false)
    }
    return null
  }

  companion object {
    const val INCOMING_DAMAGE_CAPITAL = 0.5f
    private const val EFFECT_ARMOR_FLAT_BONUS = 800f
    private const val EFFECT_ARMOR_PERCENT_BONUS = 0.5f
    private const val ARMOR_DAMAGE_REDUCE = 0.75f //by mult
    private const val HULL_DAMAGE_REDUCE = 0.75f
  }
}