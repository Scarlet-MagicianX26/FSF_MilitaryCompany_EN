package combat.impl.VEs

import com.fs.starfarer.api.combat.DamagingProjectileAPI
import combat.impl.aEP_BaseCombatEffect
import combat.plugin.aEP_CombatEffectPlugin
import combat.util.aEP_AngleTracker
import combat.util.aEP_Tool
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

open class aEP_SmokeTrail : aEP_BaseCombatEffect {
  var lastFrameLoc: Vector2f? = null
  var maxDistWithoutSmoke = 20f
  var spawnSmokeSize = 25f
  var endSmokeSize = 80f

  var initSpeed = 150f
  var initAngle = 10f
  var stopSpeed = 0.8f

  var fadeIn = 0.1f
  var fadOut = 0.9f
  var smokeLifeTime = 3f
  var color = Color(120,120,120,120)

  val smokeSpreadAngleTracker = aEP_AngleTracker(0f, 0f, 10f, 15f, -15f)

  constructor(m: DamagingProjectileAPI, maxDistWithoutSmoke: Float, lifeTime: Float, smokeSize: Float, maxSmokeSize: Float, color:Color) {
    init(m)
    lastFrameLoc = Vector2f(m.location.x,m.location.y)
    this.maxDistWithoutSmoke = maxDistWithoutSmoke
    this.smokeLifeTime = lifeTime
    this.spawnSmokeSize = smokeSize
    this.endSmokeSize = maxSmokeSize
    this.color = color
    this.smokeSpreadAngleTracker.max = initAngle
    this.smokeSpreadAngleTracker.min = -initAngle
    this.smokeSpreadAngleTracker.speed = initSpeed/2f
    this.smokeSpreadAngleTracker.randomizeTo()
  }

  override fun advance(amount: Float) {
    super.advance(amount)
    entity?: return
    var movedDistLastFrame = MathUtils.getDistance(lastFrameLoc, entity!!.location)

    //摆动角度处理
    smokeSpreadAngleTracker.advance(amount)
    if(smokeSpreadAngleTracker.isInPosition){
      smokeSpreadAngleTracker.randomizeTo()
    }


    //Global.getLogger(this.javaClass).info(movedDistLastFrame)
    if (movedDistLastFrame > maxDistWithoutSmoke) {
      //烟雾特效参数
      val smokeSize = spawnSmokeSize
      val sizeChange = (endSmokeSize - smokeSize) / smokeLifeTime
      val lastLocFacing = VectorUtils.getAngle(entity!!.location, lastFrameLoc)

      //Global.getCombatEngine().addFloatingText(m.getLocation(),movedDistLastFrame + "", 20f ,new Color(100,100,100,100),m, 0.25f, 120f);
      //当一帧移动少于2倍最小距离时，或者移动超过6倍最小距离，简化计算，直接在原地生成烟雾
      if(movedDistLastFrame < movedDistLastFrame * 2 ||
        movedDistLastFrame > movedDistLastFrame * 6){
        val smoke = aEP_MovingSmoke(entity!!.location)
        smoke.lifeTime = smokeLifeTime
        smoke.fadeIn = fadeIn
        smoke.fadeOut = fadOut
        smoke.sizeChangeSpeed = sizeChange
        smoke.size = smokeSize
        smoke.stopSpeed = stopSpeed
        smoke.color = color
        smoke.setInitVel(aEP_Tool.Speed2Velocity(lastLocFacing+smokeSpreadAngleTracker.curr,initSpeed))
        aEP_CombatEffectPlugin.addVE(smoke)

      }else{
        //处于之间时，生成一整条连续的烟雾线
        var num = 1
        while (movedDistLastFrame >= 0f) {
          val smokeSize = spawnSmokeSize
          val sizeChange = (endSmokeSize - smokeSize) / smokeLifeTime
          val lastLocFacing = VectorUtils.getAngle(entity!!.location, lastFrameLoc)
          val smokeLoc = aEP_Tool.getExtendedLocationFromPoint(entity!!.location, lastLocFacing, num * maxDistWithoutSmoke)
          val smoke = aEP_MovingSmoke(smokeLoc)
          smoke.lifeTime = smokeLifeTime
          smoke.fadeIn = fadeIn
          smoke.fadeOut = fadOut
          smoke.sizeChangeSpeed = sizeChange
          smoke.size = smokeSize
          smoke.stopSpeed = stopSpeed
          smoke.color = color
          smoke.setInitVel(aEP_Tool.Speed2Velocity(lastLocFacing+smokeSpreadAngleTracker.curr,initSpeed))
          aEP_CombatEffectPlugin.addVE(smoke)

          //Global.getCombatEngine().addFloatingText(m.getLocation(),movedDistLastFrame + "", 20f ,new Color(100,100,100,100),m, 0.25f, 120f);
          movedDistLastFrame -= maxDistWithoutSmoke
          num += 1
        }
      }
      lastFrameLoc = Vector2f(entity!!.location.x, entity!!.location.y)
    }
    advanceImpl(amount)
  }
}