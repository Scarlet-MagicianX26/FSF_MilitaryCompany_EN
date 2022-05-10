package combat.impl

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import org.lazywizard.lazylib.MathUtils
import java.util.*

open class aEP_BaseCombatEffect : CombatLayeredRenderingPlugin {

  var time = 0f
  var lifeTime = 0f
  var entity : CombatEntityAPI? = null
  var shouldEnd = false;
  var endWithoutClean = false;
  var layers: EnumSet<CombatEngineLayers> = EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER)
  var radius = 999999f;
  var renderInShader = false

  constructor()

  constructor(lifeTime: Float){
    this.lifeTime = lifeTime
  }

  constructor(lifeTime: Float, entity: CombatEntityAPI?){
    this.lifeTime = lifeTime
    this.entity = entity
  }

  /**
   * 若初始化 entity，则 entity消失后也会自动终结
   */
  override fun init(entity: CombatEntityAPI?) {
    this.entity = entity
  }

  /**
   * 等于下一帧强制结束，下一帧的advance不会触发
   */
  override fun cleanup() {
    shouldEnd = true;
  }

  override fun isExpired(): Boolean {
    if(shouldEnd){
      if(!endWithoutClean){
        readyToEnd()
      }
      radius = -1f
      return true
    }
    return false
  }

  /**
   * 不应该override此方法，使用advanceImpl
   * time 处于 (0,lifeTime] 的区间内
   * time 刚好到 lifeTime的一帧仍然会运行
   * 当 shouldEnd为 true，准备在下一帧结束时，不会运行
   * 当 lifeTime <= 0 时，按时终结机制不生效，请在advanceImpl里面手动控制
   */
  override fun advance(amount: Float) {
    //若 entity不为空，则进行 entity检测，不过就直接结束
    if(entity != null) {
      if(!Global.getCombatEngine().isEntityInPlay(entity) ||
        ((entity is ShipAPI) && !(entity as ShipAPI).isAlive)){
        shouldEnd = true
      }
    }
    if(shouldEnd) return

    time += amount
    MathUtils.clamp(time,0f,lifeTime)
    advanceImpl(amount)
    if(time >= lifeTime && lifeTime > 0){
      shouldEnd = true
    }
  }

  /**
   * 在time已经增加之后触发，无法得到time == 0的第一帧
   */
  open fun advanceImpl(amount: Float){

  }

  /**
   * 结束时运行一次
   */
  open fun readyToEnd(){

  }

  override fun getActiveLayers(): EnumSet<CombatEngineLayers> {
    return layers
  }

  /**
   * 在结束时会把 radius设置为-1，用于 shaderPlugin中不经过 isExpire方法也能检测是否结束
   */
  override fun getRenderRadius(): Float {
    return radius
  }

  override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {

  }
}