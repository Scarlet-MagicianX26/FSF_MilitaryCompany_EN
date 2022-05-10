package data.scripts.ai

import com.fs.starfarer.api.combat.ShipAIConfig
import com.fs.starfarer.api.combat.ShipAIPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags

open class aEP_BaseShipAI: ShipAIPlugin {

  var stopFiringTime = 1f;
  var needsRefit = false;
  val shipAIConfig = ShipAIConfig()
  val shipAIFlags = ShipwideAIFlags()
  var ship:ShipAPI?

  constructor(ship : ShipAPI?){
    this.ship = ship
  }

  override fun setDoNotFireDelay(amount: Float) {
    this.stopFiringTime = stopFiringTime
  }

  override fun forceCircumstanceEvaluation() {

  }

  override fun advance(amount: Float) {
    aiFlags.advance(amount)
    advanceImpl(amount)
  }

  /**
   * 用这个
   */
  open fun advanceImpl(amount: Float){

  }

  /**
   * Only called for fighters, not regular ships or drones.
   * @return whether the fighter needs refit
   */
  override fun needsRefit(): Boolean {
    return needsRefit
  }

  override fun getAIFlags(): ShipwideAIFlags {
    return shipAIFlags
  }

  override fun cancelCurrentManeuver() {

  }

  override fun getConfig(): ShipAIConfig {
    return shipAIConfig
  }
}