package combat.util

import com.fs.starfarer.api.combat.WeaponAPI
import java.util.HashMap
import combat.util.aEP_DecoMoveController
import org.lwjgl.util.vector.Vector2f
import org.lazywizard.lazylib.FastTrig
import data.scripts.util.MagicAnim

class aEP_DecoMoveController(var weapon: WeaponAPI) {
  companion object {
    //range,speed(by percent)
    val mag: MutableMap<String, Array<Float>> = HashMap()

    init {
      mag["aEP_FCL"] = arrayOf(12f, 2f)
      mag["aEP_FCL_scaffold"] = arrayOf(4f, 2f)
      mag["aEP_FCL_glow"] = arrayOf(12f, 2f)
      mag["aEP_FCL_cover"] = arrayOf(-3f, 2f)
      mag["aEP_raoliu_armor"] = arrayOf(5f, 2f)
      mag["aEP_raoliu_hull"] = arrayOf(0f, 2f)
      mag["aEP_raoliu_armor_dark"] = arrayOf(5f, 2f)
      mag["aEP_raoliu_bridge"] = arrayOf(8f, 2f)
      mag["aEP_duiliu_armor_L"] = arrayOf(20f, 0.5f)
      mag["aEP_duiliu_armor_R"] = arrayOf(20f, 0.5f)
      mag["aEP_duiliu_armor_L3"] = arrayOf(15f, 0.5f)
      mag["aEP_duiliu_armor_R3"] = arrayOf(15f, 0.5f)
      mag["aEP_duiliu_limiter"] = arrayOf(18f, 1f)
      mag["aEP_duiliu_limiter_glow"] = arrayOf(18f, 1f)
      mag["aEP_duiliu_gun_cover"] = arrayOf(-30f, 0.5f)
      mag["aEP_hailiang_holderL"] = arrayOf(6f, 0.5f)
      mag["aEP_hailiang_holderR"] = arrayOf(6f, 0.5f)
      mag["aEP_shangshengliu_armor"] = arrayOf(8f, 2f)
      mag["aEP_shangshengliu_hull"] = arrayOf(0f, 2f)
      mag["aEP_shangshengliu_armor_dark"] = arrayOf(8f, 2f)
      mag["aEP_shangshengliu_top"] = arrayOf(9f, 2f)
      mag["aEP_shangshengliu_bottom"] = arrayOf(21f, 1f)
    }
  }

  var range = 0f
  var speed = 0f
  var effectiveLevel = 0f
  var toLevel = 0f
  var originalX = 0f
  var originalY = 0f
  fun advance(amount: Float) {
    if (weapon.ship == null) return
    val slotRevLocation = weapon.ship.hullSpec.getWeaponSlotAPI(weapon.slot.id).location
    var toMove = 0f
    toMove = if (effectiveLevel > toLevel) {
      -Math.min(effectiveLevel - toLevel, speed * amount)
    } else {
      Math.min(toLevel - effectiveLevel, speed * amount)
    }
    effectiveLevel += toMove
    val angle = weapon.spec.turretAngleOffsets[0]
    weapon.sprite.setCenter(originalX - FastTrig.sin(Math.toRadians(angle.toDouble())).toFloat() * MagicAnim.smooth(effectiveLevel) * range, originalY - FastTrig.cos(Math.toRadians(angle.toDouble())).toFloat() * MagicAnim.smooth(effectiveLevel) * range)
  }


  init {
    originalX = weapon.sprite.centerX
    originalY = weapon.sprite.centerY
    if (mag.containsKey(weapon.spec.weaponId)) {
      range = mag[weapon.spec.weaponId]!![0]
      speed = mag[weapon.spec.weaponId]!![1]
    }
  }
}