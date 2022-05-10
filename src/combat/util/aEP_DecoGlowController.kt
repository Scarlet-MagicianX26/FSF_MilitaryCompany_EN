package combat.util

import com.fs.starfarer.api.combat.WeaponAPI
import combat.util.aEP_DecoGlowController.GlowData
import java.util.HashMap
import combat.util.aEP_DecoGlowController
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import java.awt.Color

class aEP_DecoGlowController(var weapon: WeaponAPI) {
  companion object {
    //range,speed(by percent)
    val mag: MutableMap<String, GlowData> = HashMap()

    init {
      //jitter range, speed, 第四位0代表初始亮度为0
      //在这速度是正的，越大变化越快
      mag["aEP_FCL_glow"] = GlowData(2f, 2f, Color(250, 250, 250, 250), 0f)
      mag["aEP_duiliu_limiter_glow"] = GlowData(4f, 0.5f, Color(250, 250, 250, 250), 0f)
      mag["aEP_duiliu_main_glow"] = GlowData(2f, 1f, Color(250, 250, 250, 250), 0f)
      mag["aEP_maodian_glow"] = GlowData(2f, 4f, Color(250, 20, 250, 250), 0f)
      //mag.put("aEP_raoliu_armor",new GlowData(0f,16f,new Color(255,255,255),0f));
    }
  }

  var test = Color(240, 100, 20)
  var effectiveLevel = 0f
  var toLevel = 0f
  var jitterInterval = IntervalUtil(0.1f, 0.1f)
  fun advance(amount: Float) {
    if (weapon?.ship == null) return
    if (!mag.containsKey(weapon.spec.weaponId)) return
    jitterInterval.advance(amount)
    if (!jitterInterval.intervalElapsed()) return
    weapon.animation.frame = 0


    //aEP_Tool.addDebugText(""+weapon.getAnimation().getFrame());
    val data = mag[weapon.spec.weaponId]
    val range = data!!.range
    val speed = data.speed
    val color = data.color
    val toMove: Float = if (effectiveLevel > toLevel) {
      -(effectiveLevel - toLevel).coerceAtMost(speed * amount)
    } else {
      (toLevel - effectiveLevel).coerceAtMost(speed * amount)
    }
    effectiveLevel += toMove
    effectiveLevel = MathUtils.clamp(effectiveLevel, 0f, 1f)
    weapon.animation.frame = 1
    val originalX = weapon.sprite.centerX
    val originalY = weapon.sprite.centerY
    weapon.sprite.setCenter(originalX + effectiveLevel * MathUtils.getRandomNumberInRange(-range, range), originalY - effectiveLevel * MathUtils.getRandomNumberInRange(-range, range))
    weapon.animation.alphaMult = effectiveLevel
    weapon.sprite.setAdditiveBlend()
    weapon.sprite.color = color
  }

  class GlowData(var range: Float, var speed: Float, var color: Color, var init: Float)

  init {
    if (mag.containsKey(weapon.spec.weaponId)){
      val data = mag[weapon.spec.weaponId]
      val color = data!!.color
      effectiveLevel = data.init
      weapon.animation?.alphaMult = effectiveLevel
    }
  }
}