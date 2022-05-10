package combat.impl.VEs

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.IntervalUtil
import combat.impl.aEP_BaseCombatEffect
import combat.util.aEP_Tool
import org.lazywizard.lazylib.FastTrig
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

open class aEP_MovingSprite : aEP_BaseCombatEffect{

  private var position = Vector2f(0f,0f)
  var velocity = Vector2f(0f, 0f)
  var angle = 0f
  var acceleration = Vector2f(0f, 0f)
  var angleSpeed= 0f


  val stopForceTimer = IntervalUtil(0.1f,0.1f)
  var stopSpeed = 1f

  var fadeIn = 0f
  var fadeOut = 0f

  var size: Vector2f = Vector2f(20f,20f)
  var sizeChangeSpeed: Vector2f = Vector2f(0f,0f)

  var color = Color(255,255,255,255)
  var changingColor = Color(0,0,0,0)

  private var spriteTexId : Int
  private var point1Left = Vector2f(0f,0f)
  private var point1Right = Vector2f(0f,0f)
  private var point2Left = Vector2f(0f,0f)
  private var point2Right = Vector2f(0f,0f)

  constructor(position: Vector2f, size: Vector2f, angle: Float, spriteId: String){
    val id: Array<String> = spriteId.split("\\.".toRegex()).toTypedArray()
    if(spriteId.contains('/')){
      spriteTexId = Global.getSettings().getSprite(spriteId).textureId
    }else{
      spriteTexId = Global.getSettings().getSprite(id[0], id[1]).textureId
    }
    this.position = position
    this.angle = angle
    this.size = size
  }

  constructor(sprite: SpriteAPI, position: Vector2f?){
    spriteTexId = sprite.textureId
    if(position != null)this.position = position
    this.angle = sprite.angle
    this.size = Vector2f(sprite.width,sprite.height)
    this.color = sprite.color
  }

  constructor(position: Vector2f, velocity: Vector2f, angleSpeed: Float,angle: Float, fadeIn:Float, full:Float, fadeOut: Float,sizeChange:Vector2f, size:Vector2f, spriteId:String, color:Color){
    val id: Array<String> = spriteId.split("\\.".toRegex()).toTypedArray()
    if(spriteId.contains('/')){
      spriteTexId = Global.getSettings().getSprite(spriteId).textureId
    }else{
      spriteTexId = Global.getSettings().getSprite(id[0], id[1]).textureId
    }
    this.position = position
    this.velocity = velocity
    this.angle = angle
    this.angleSpeed = angleSpeed
    this.size = size
    this.sizeChangeSpeed = sizeChange
    this.lifeTime = fadeIn+full+fadeOut
    this.fadeIn = fadeIn/lifeTime
    this.fadeOut = fadeOut/lifeTime
    this.color = color
  }

  override fun advance(amount: Float) {
    super.advance(amount)

    //默认颜色是处于 full状态
    changingColor = color
    val fadeInTime = fadeIn*lifeTime
    if (time < fadeInTime) {
      val R = color.red
      val G = color.green
      val B = color.blue
      val transparency = MathUtils.clamp(color.alpha * time / fadeInTime, 0f, 250f).toInt()
      changingColor = Color(R, G, B, transparency)
    }

    //fade out
    val fadeOutTime = fadeOut*lifeTime
    if (lifeTime - time < fadeOutTime) {
      val R = color.red
      val G = color.green
      val B = color.blue
      val transparency = MathUtils.clamp(color.alpha * (lifeTime - time) / fadeOutTime, 0f, 250f).toInt()
      changingColor = Color(R, G, B, transparency)
    }
    velocity = Vector2f(velocity.x + acceleration.x * amount,velocity.y + acceleration.y)
    position = Vector2f(position.x + velocity.x * amount, position.y + velocity.y * amount)
    updatePositon(position, angle)
    //每隔0.1秒进行减速
    stopForceTimer.advance(amount)
    if (stopForceTimer.intervalElapsed()) {
      velocity.scale(stopSpeed)
      angleSpeed *= stopSpeed
    }

    angle = aEP_Tool.angleAdd(angle, angleSpeed * amount)
    val sizeX = Math.abs(size.getX() + sizeChangeSpeed.getX() * amount)
    val sizeY = Math.abs(size.getY() + sizeChangeSpeed.getY() * amount)
    size = Vector2f(sizeX,sizeY)
    advanceImpl(amount)
  }

  override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
    //render会在 advance之前被调用
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, spriteTexId)


    //begin
    GL11.glBegin(GL11.GL_QUADS)
    val red = changingColor.red
    val green = changingColor.green
    val blue = changingColor.blue
    val alpha = changingColor.alpha

    GL11.glColor4ub(red.toByte(), green.toByte(), blue.toByte(), alpha.toByte())
    GL11.glTexCoord2f(0f, 0f)
    GL11.glVertex2f(point1Left.getX(), point1Left.getY())
    GL11.glTexCoord2f(1f, 0f)
    GL11.glVertex2f(point1Right.getX(), point1Right.getY())
    GL11.glTexCoord2f(1f, 1f)
    GL11.glVertex2f(point2Right.getX(), point2Right.getY())
    GL11.glTexCoord2f(0f, 1f)
    GL11.glVertex2f(point2Left.getX(), point2Left.getY())

    //end
    GL11.glEnd()
  }

  open fun setInitVel(vel: Vector2f) {
    velocity = Vector2f(velocity.x + vel.x, velocity.y + vel.y)
  }

  open fun addAcc(toAddAcceleration: Vector2f) {
    acceleration = Vector2f(acceleration.x + toAddAcceleration.x, acceleration.y + toAddAcceleration.y)
  }

  private fun updatePositon (position: Vector2f,angle: Float){
    val rad = Math.toRadians(angle.toDouble())
    val x0 = FastTrig.cos(rad).toFloat()
    val y0 = FastTrig.sin(rad).toFloat()
    //计算旋转的是半长，不是全长
    val length = size.x/2f
    val height = size.y/2f
    point1Left = Vector2f(position.x - length*x0 + height*y0,position.y - length*y0 - height*x0)
    point1Right = Vector2f(position.x + length*x0 + height*y0,position.y + length*y0 - height*x0)
    point2Left = Vector2f(position.x - length*x0 - height*y0,position.y - length*y0 + height*x0)
    point2Right = Vector2f(position.x+ length*x0 - height*y0,position.y + length*y0 + height*x0)
  }

}

open class aEP_SingleFrame : aEP_BaseCombatEffect {
  var sprite: SpriteAPI
  var location: Vector2f
  //现已废弃废弃功能，原用于在暂停下依旧运行的 advance系统中使用
  var endWhilePause = false
  private var renderSprite: SpriteAPI? = null

  constructor(spriteId:String, angle:Float, color:Color?, location:Vector2f, size:Vector2f?){
    this.location = location
    val id: Array<String> = spriteId.split("\\.".toRegex()).toTypedArray()
    if(spriteId.contains('/')){
      sprite = Global.getSettings().getSprite(spriteId)
    }else{
      sprite = Global.getSettings().getSprite(id[0], id[1])
    }
    sprite.angle = angle
    if(size!=null) sprite.setSize(size.x, size.y)
    lifeTime = 0f
    if (color != null) {
      sprite.color = color
    }
  }

  constructor(spite:SpriteAPI, angle:Float, color:Color?, location:Vector2f, size:Vector2f?){
    this.location = location
    sprite = spite
    sprite.angle = angle
    if(size != null) sprite.setSize(size.x, size.y)
    lifeTime = 0f
    if (color != null) {
      sprite.color = color
    }
  }

  override fun advance(amount: Float) {
    super.advance(amount)
    advanceImpl(amount)
    //只有在一种情况下不会本帧内 cleanup
    //现已废弃废弃功能，原用于在暂停下依旧运行的 advance系统中使用
    if(Global.getCombatEngine().isPaused && !endWhilePause){
      return
    }
    cleanup()
  }

  //由于会出现同帧调用 2次render渲染出重影的问题，在里面加一个检测
  //具体原因不明
  override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
    if(shouldEnd) return
    renderSprite = sprite?: return
    renderSprite!!.renderAtCenter(location.x, location.y)
    //Global.getLogger(this.javaClass).info(Global.getCombatEngine().getTotalElapsedTime(false))
  }


}