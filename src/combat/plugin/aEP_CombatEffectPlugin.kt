package combat.plugin

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import combat.impl.aEP_BaseCombatEffect
import java.util.*

class aEP_CombatEffectPlugin : EveryFrameCombatPlugin, aEP_BaseCombatEffect{


  var amount = 0f;
  private var engine: CombatEngineAPI? = null
  private val effects: LinkedList<CombatLayeredRenderingPlugin> = LinkedList()
  private val pureVisualEffects: LinkedList<CombatLayeredRenderingPlugin> = LinkedList()
  private val newEffects: LinkedList<CombatLayeredRenderingPlugin> = LinkedList()

  constructor(){
    aEP_BaseCombatEffect()
    this.layers = EnumSet.allOf(CombatEngineLayers::class.java)
  }

  /**
   * EveryFramePlugin的实现
   * 唯一的作用是在 init的时候 new一个实例加入 LayeredRenderingPlugin
   */
  override fun init(engine: CombatEngineAPI?) {
    this.engine = engine!!
    //将 LayeredRenderingPlugin的实例放入 customData，不在任何地方引用，这样战斗结束后内存会被回收
    val render = aEP_CombatEffectPlugin()
    engine.customData["aEP_CombatRenderPlugin"] = render
    engine.addLayeredRenderingPlugin(render)
    effects.clear()
    pureVisualEffects.clear()
    Global.getLogger(this.javaClass).info("aEP_CombatEffectPlugin init")
  }

  override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {

  }

  override fun renderInWorldCoords(viewport: ViewportAPI?) {}

  override fun renderInUICoords(viewport: ViewportAPI?) {}

  override fun processInputPreCoreControls(amount: Float, events: MutableList<InputEventAPI>?) {}

  /**
   * LayeredRenderingPlugin的实现
   */
  override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
    val toRemove: MutableList<aEP_BaseCombatEffect> = ArrayList()
    for (e in pureVisualEffects) {
      if(!e.activeLayers.contains(layer)) continue
      if(e is aEP_BaseCombatEffect && e.renderInShader) continue
      e.render(layer, viewport)
    }
    pureVisualEffects.removeAll(toRemove)
    toRemove.clear()

    for (e in effects) {
      if(!e.activeLayers.contains(layer)) continue
      if(e is aEP_BaseCombatEffect && e.renderInShader) continue
      e.render(layer, viewport)
    }
    effects.removeAll(toRemove)
    toRemove.clear()
  }

  /**
   * LayeredRenderingPlugin的 advance部分
   * 和 render分开
   * 注意一下暂停时，advance并不调用，render会继续每帧调用
   * 请务必把逻辑和渲染分开写！！
   */
  override fun advance(amount: Float) {
    engine = Global.getCombatEngine()
    if(engine!!.isPaused) this.amount = 0f else this.amount = amount
    this.amount = amount

    val max = 999999
    if (pureVisualEffects.size > max) {
      var num = 0
      while (num < pureVisualEffects.size - max) {
        pureVisualEffects.removeAt(num)
        num ++
      }
    }
    /**
     * newEffects用于储存本帧新生成的 effects
     * 避免 effect生成新 effect导致遍历过程中动态修改报错
     * 对应 VE是不起效的，所以避免 VE中生成新的 VE
     */
    for (e in newEffects) {
      effects.add(e)
    }

    val toRemove: MutableList<CombatLayeredRenderingPlugin> = ArrayList()
    for (e in pureVisualEffects) {
      if(e.isExpired){
        toRemove.add(e)
        continue
      }
      e.advance(amount)
    }
    pureVisualEffects.removeAll(toRemove)
    toRemove.clear()

    for (e in effects) {
      if(e.isExpired){
        toRemove.add(e)
        continue
      }
      e.advance(amount)
    }
    effects.removeAll(toRemove)
    //Global.getLogger(this.javaClass).info("newEffects.size: "+newEffects.size)
    newEffects.clear()
  }

  /**
   * 静态函数部分，防止内存泄漏不能在任何地方保留此类的的引用
   */
  companion object Mod{
    fun addEffect(e: aEP_BaseCombatEffect){
      val c = Global.getCombatEngine().customData["aEP_CombatRenderPlugin"] as aEP_CombatEffectPlugin?
      c?.newEffects?.add(e) ?:Global.getLogger(this.javaClass).info(this.javaClass.name+" has not been initialized")
      //Global.getLogger(this.javaClass).info(this.javaClass.name+": added to effect list")
    }

    fun addVE(e: aEP_BaseCombatEffect){
      val c = Global.getCombatEngine().customData["aEP_CombatRenderPlugin"] as aEP_CombatEffectPlugin?
      c?.pureVisualEffects?.add(e) ?:Global.getLogger(this.javaClass).info(this.javaClass.name+" has not been initialized")
      //Global.getLogger(this.javaClass).info(this.javaClass.name+": added to VE list")
    }
  }
}
