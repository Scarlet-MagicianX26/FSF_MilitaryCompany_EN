package data.hullmods

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.AdvanceableListener
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats.EXPLOSION_DAMAGE_MULT
import com.fs.starfarer.api.impl.campaign.ids.Stats.EXPLOSION_RADIUS_MULT
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import combat.impl.VEs.aEP_MovingSmoke
import combat.plugin.aEP_CombatEffectPlugin.Mod.addVE
import combat.util.aEP_DataTool
import combat.util.aEP_Tool
import combat.util.aEP_Tool.Util.Speed2Velocity
import combat.util.aEP_Tool.Util.angleAdd
import combat.util.aEP_Tool.Util.getExtendedLocationFromPoint
import combat.util.aEP_Tool.Util.killMissile
import data.scripts.util.MagicAnim
import data.scripts.weapons.aEP_FCLAnimation
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class aEP_FighterSpecial: HullModEffect {

  var hullmod : HullModEffect? = null

  override fun init(spec: HullModSpecAPI?) {
    //类查找过，找到实际对应的代码存入 hullmod变量
    //classForName查不到就保持null
    val id = spec?.id
    try {
      val e = Class.forName(aEP_FighterSpecial::class.java.getPackage().name + "." + id)
      hullmod = e.newInstance() as HullModEffect
      Global.getLogger(this.javaClass).info("aEP_FighterSpecialLoaded :" +e.name)
    } catch (e: ClassNotFoundException) {
      e.printStackTrace()
    } catch (e: InstantiationException) {
      e.printStackTrace()
    } catch (e: IllegalAccessException) {
      e.printStackTrace()
    }

  }

  override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
    hullmod?: return
    hullmod!!.applyEffectsBeforeShipCreation(hullSize,stats,id)
  }

  override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
    hullmod?: return
    hullmod!!.applyEffectsAfterShipCreation(ship, id)
  }

  override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String {
    hullmod?: return ""
    hullmod!!.getDescriptionParam(index, hullSize)
    return ""
  }

  override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?, ship: ShipAPI?): String {
    hullmod?: return ""
    hullmod!!.getDescriptionParam(index, hullSize, ship)
    return ""
  }

  override fun applyEffectsToFighterSpawnedByShip(fighter: ShipAPI?, ship: ShipAPI?, id: String?) {
    hullmod?:return
    hullmod!!.applyEffectsToFighterSpawnedByShip(fighter, ship, id)
  }

  override fun isApplicableToShip(ship: ShipAPI?): Boolean {
    hullmod?:false
    hullmod!!.isApplicableToShip(ship)
    return false
  }

  override fun getUnapplicableReason(ship: ShipAPI?): String {
    hullmod?: return ""
    hullmod!!.getUnapplicableReason(ship)
    return ""
  }

  /**
   * ship may be null from autofit.
   * @param ship
   * @param marketOrNull
   * @param mode
   * @return
   */
  override fun canBeAddedOrRemovedNow(ship: ShipAPI?, marketOrNull: MarketAPI?, mode: CampaignUIAPI.CoreUITradeMode?): Boolean {
    hullmod?: return false
    hullmod!!.canBeAddedOrRemovedNow(ship, marketOrNull, mode)
    return false
  }

  override fun getCanNotBeInstalledNowReason(ship: ShipAPI?, marketOrNull: MarketAPI?, mode: CampaignUIAPI.CoreUITradeMode?): String {
    hullmod?: return ""
    hullmod!!.getCanNotBeInstalledNowReason(ship, marketOrNull, mode)
    return ""
  }

  /**
   * Not called while paused.
   * But, called when the fleet data needs to be re-synced,
   * with amount=0 (such as if, say, a fleet member is moved around.
   * in the fleet screen.)
   * @param member
   * @param amount
   */
  override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
    hullmod?:return
    hullmod!!.advanceInCampaign(member, amount)
  }

  /**
   * Not called while paused.
   * @param ship
   * @param amount
   */
  override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
    hullmod?:return
    hullmod!!.advanceInCombat(ship,amount)
  }

  /**
   * Hullmods that return true here should only ever be built-in, as cost changes aren't handled when
   * these mods can be added or removed to/from the variant.
   * @return
   */
  override fun affectsOPCosts(): Boolean {
    return false
    return hullmod!!.affectsOPCosts()
    return false
  }

  /**
   * ship may be null, will be for modspecs. hullsize will always be CAPITAL_SHIP for modspecs.
   * @param hullSize
   * @param ship
   * @param isForModSpec
   * @return
   */
  override fun shouldAddDescriptionToTooltip(hullSize: ShipAPI.HullSize?, ship: ShipAPI?, isForModSpec: Boolean): Boolean {
    return false
    return hullmod!!.shouldAddDescriptionToTooltip(hullSize, ship, isForModSpec)
    return false
  }

  /**
   * ship may be null, will be for modspecs. hullsize will always be CAPITAL_SHIP for modspecs.
   * @param tooltip
   * @param hullSize
   * @param ship
   * @param width
   * @param isForModSpec
   */
  override fun addPostDescriptionSection(tooltip: TooltipMakerAPI?, hullSize: ShipAPI.HullSize?, ship: ShipAPI?, width: Float, isForModSpec: Boolean) {
    hullmod?: return
    hullmod!!.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec)
  }

  override fun getBorderColor(): Color? {
    hullmod?: return null
    return hullmod!!.borderColor
    return Color(255,255,255)
  }

  override fun getNameColor(): Color? {
    hullmod?: return null
    return hullmod!!.nameColor
    return Color(255,255,255)
  }

  /**
   * Sort order within the mod's display category. Not used when category == 4, since then
   * the order is determined by the order in which the player added the hullmods.
   * @return
   */
  override fun getDisplaySortOrder(): Int {
    hullmod?:return 99
    return hullmod!!.displaySortOrder
    return 99
  }

  /**
   * Should return 0 to 4; -1 for "use default".
   * The default categories are:
   * 0: built-in mods in the base hull
   * 1: perma-mods that are not story point mods
   * 2: d-mods
   * 3: mods built in via story points
   * 4: regular mods
   *
   * @return
   */
  override fun getDisplayCategoryIndex(): Int {
    hullmod?:return 4
    return hullmod!!.displayCategoryIndex
    return 4
  }
}

//锚点无人机护盾插件
class aEP_MaoDianShield : aEP_BaseHullMod() {
  val TIME_TO_EXTEND = 2f
  //ship文件里的护盾半径也要改，否则护盾中心在屏幕外的时候不会渲染护盾
  val MAX_SHIELD_RADIUS = 400f
  val MAX_MOVE_TIME = 20f
  val FLUX_INCREASE = 500f
  val RADAR_SPEED = -90f
  val EXPLODSION_DAMAGE_MULT = 0.005f
  val EXPLODSION_RANGE_MULT = 0.5f

  val id = "aEP_MaoDianShield"
  /**
   * 使用这个
   */
  override fun applyEffectsAfterShipCreationImpl(ship: ShipAPI?, id: String?) {
    if (ship?.mutableStats == null) return
    ship.mutableStats.empDamageTakenMult.modifyMult(this.id,0f)
    if(!ship.hasListenerOfClass(ShieldListener::class.java)){
      ship.addListener(ShieldListener(ship))
    }
    ship.mutableStats.dynamic.getStat(EXPLOSION_DAMAGE_MULT).modifyMult(id,EXPLODSION_DAMAGE_MULT)
    ship.mutableStats.dynamic.getStat(EXPLOSION_RADIUS_MULT).modifyMult(id,EXPLODSION_RANGE_MULT)
  }

  inner class ShieldListener(val ship: ShipAPI) : AdvanceableListener, DamageTakenModifier, HullDamageAboutToBeTakenListener{
    var time = 0f
    var shieldTime =0f
    var moveTime = 0f
    val empArcTracker = IntervalUtil(0.1f,0.6f)
    val particleTracker = IntervalUtil(0.1f,0.1f)

    var shouldEnd = false
    override fun advance(amount: Float) {
      time = MathUtils.clamp(time + aEP_Tool.getAmount(ship),0f,999f)
      val shieldLevel = shieldTime/TIME_TO_EXTEND
      val fluxLevel = ship.fluxLevel
      //改变盾大小，和只改radius冲突，需要每帧调用，别动最好，因为盾的外圈是最先渲染的，动态调整会影响ring的贴图
      val rad = MAX_SHIELD_RADIUS * MagicAnim.smooth(shieldLevel)
      ship.shield?.setRadius(rad,
        Global.getSettings().getSpriteName("aEP_hullstyle","aEP_shield_inner03"),
        Global.getSettings().getSpriteName("aEP_hullstyle","aEP_shield_outer03"))
      //顺便改变船碰撞圈的大小
      ship.collisionRadius = MathUtils.clamp(rad,40f,MAX_SHIELD_RADIUS)
      //盾颜色
      ship.shield.innerColor = Color(0.5f+0.5f*fluxLevel,
        0.5f,
        0.65f*(1f-fluxLevel),
        (0.2f * shieldLevel * shieldLevel) +MagicAnim.smooth((0.35f*(1f-fluxLevel))))

      //若开盾强制增加软幅能，增加护盾时间。若不开盾增加机动时间，重置护盾时间，机动超时后快速涨幅能
      if(ship.shield?.isOn == true){
        //强制360度盾
        ship.shield?.activeArc = 360f
        shieldTime = MathUtils.clamp(shieldTime + aEP_Tool.getAmount(ship),0f,TIME_TO_EXTEND)
        ship.fluxTracker.increaseFlux((FLUX_INCREASE+ship.mutableStats.fluxDissipation.modifiedValue)*amount,false)
        for(w in ship.allWeapons){
          if(w.spec.weaponId == "aEP_maodian_glow"){
            //注意一下这3个存进去的类并不是初始化的时候就一起初始化的
            (w.effectPlugin as aEP_FCLAnimation).decoGlowController.toLevel = 1f
          }
        }
      }else{
        shieldTime = MathUtils.clamp(shieldTime - aEP_Tool.getAmount(ship),0f,TIME_TO_EXTEND)
        moveTime = MathUtils.clamp(moveTime + aEP_Tool.getAmount(ship),0f,MAX_MOVE_TIME)
        if(moveTime >= MAX_MOVE_TIME){
          ship.fluxTracker.increaseFlux((ship.fluxTracker.maxFlux * 0.2f + ship.mutableStats.fluxDissipation.modifiedValue)*amount,false)
        }
        for(w in ship.allWeapons){
          if(w.spec.weaponId == "aEP_maodian_glow"){
            //注意一下这3个存进去的类并不是初始化的时候就一起初始化的
            (w.effectPlugin as aEP_FCLAnimation)?.decoGlowController?.toLevel = 0f
          }
        }
      }

      //旋转雷达
      for(w in ship.allWeapons){
        if(w.spec.weaponId == "aEP_maodian_radar"){
          w.currAngle = (w.currAngle + amount * RADAR_SPEED)
        }
      }

      //生成粒子
      if(ship.shield?.isOn == true && shieldLevel >= 0.35f){
        particleTracker.advance(amount)
        if(particleTracker.intervalElapsed()){
          val shieldRad = MAX_SHIELD_RADIUS*shieldLevel
          val num = 8
          var i = 0
          val angleChange = 360f/num
          while (i < num){
            val angle = MathUtils.getRandomNumberInRange(i * angleChange,i * angleChange + angleChange)
            val range = MathUtils.getRandomNumberInRange(ship.collisionRadius,shieldRad * 1f / 4f)
            val moveDist = shieldRad - range
            val point = aEP_Tool.getExtendedLocationFromPoint(ship.location,angle,range)
            Global.getCombatEngine().addSmoothParticle(point,
              aEP_Tool.Speed2Velocity(angle,moveDist * 1.5f),
              20f,
              1f,
              0.666f,
              aEP_Tool.getColorWithAlphaChange(ship.shield.innerColor,3f))
            i ++
          }
        }
      }

      //幅能快满的时候泛红
      if(fluxLevel > 0.35f){
        ship.isJitterShields = false
        var intense = (fluxLevel-0.35f)/0.65f
        intense *= intense
        ship.setJitter(id,Color(250,100,100,(255*intense).toInt()),intense,4,1f)

        //大于0.65开始漏电
        empArcTracker.advance(amount * (fluxLevel * 0.6f + 0.75f))
        if(empArcTracker.intervalElapsed() && fluxLevel > 0.65f){
          val from = MathUtils.getRandomPointInCircle(ship.location, 40f)
          val angle = VectorUtils.getAngle(ship.location,from)
          Global.getCombatEngine().spawnEmpArcVisual(
            from,
            ship,
            MathUtils.getRandomPointInCone(ship.location,40f + 100f,aEP_Tool.angleAdd(angle,-15f),aEP_Tool.angleAdd(angle,15f)),
            ship,
            2f,
            Color.magenta,
            Color.white)
        }

      }

      //如果本帧幅能满了，准备自毁
      if(ship.fluxLevel > 0.99f){
        shouldEnd = true
      }

      //自毁
      if(shouldEnd){
        val suppressBefore = Global.getCombatEngine()?.getFleetManager(ship.owner)?.isSuppressDeploymentMessages
        Global.getCombatEngine()?.getFleetManager(ship.owner)?.isSuppressDeploymentMessages = true
        ship.collisionRadius = 40f
        Global.getCombatEngine().applyDamage(
          ship,
          ship.location,
          (ship.hitpoints + ship.hullSpec.armorRating) * 5f,
          DamageType.HIGH_EXPLOSIVE,
          0f,
          true,
          false,
          ship)
        Global.getCombatEngine().removeEntity(ship)
        Global.getCombatEngine()?.getFleetManager(ship.owner)?.isSuppressDeploymentMessages = suppressBefore?:false
      }
    }

    /**
     * Modifications to damage should ONLY be made using damage.getModifier().
     *
     * param can be:
     * null
     * DamagingProjectileAPI
     * BeamAPI
     * EmpArcEntityAPI
     * Something custom set by a script
     *
     * @return the id of the stat modification to damage.getModifier(), or null if no modification was made
     */
    override fun modifyDamageTaken(param: Any?, target: CombatEntityAPI?, damage: DamageAPI?, point: Vector2f?, shieldHit: Boolean): String? {
      if(!shieldHit) return null
      //造成软幅能的伤害，和软幅能光束，都不计入
      if(damage?.isSoftFlux?: true) return null
      if(param is BeamAPI && !damage!!.isForceHardFlux) return null
      var fluxToSheild = (damage?.damage?: 0.01f) * (damage?.modifier?.modifiedValue ?:1f)
      val damageMap = HashMap<DamageType,Float >()
      damageMap.put(DamageType.HIGH_EXPLOSIVE,0.5f)
      damageMap.put(DamageType.ENERGY,1f)
      damageMap.put(DamageType.KINETIC, 2f)
      damageMap.put(DamageType.FRAGMENTATION,0.25f)
      fluxToSheild *= damageMap[damage?.type ?: DamageType.FRAGMENTATION]!!
      fluxToSheild *= ship.mutableStats.shieldAbsorptionMult.computeMultMod() * ship.hullSpec.shieldSpec.fluxPerDamageAbsorbed
      var softFlux = ship.fluxTracker.currFlux - ship.fluxTracker.hardFlux
      val afterHitFlux = softFlux - fluxToSheild
      if(afterHitFlux >=0){
        ship.fluxTracker.decreaseFlux(fluxToSheild)
        ship.fluxTracker.increaseFlux(fluxToSheild,true)
        damage?.modifier?.modifyMult(id,0f)
       // Global.getLogger(this.javaClass).info(fluxToSheild)
      }else{
        val percent = MathUtils.clamp((-afterHitFlux)/fluxToSheild,0f,1f)
        ship.fluxTracker.decreaseFlux(softFlux)
        ship.fluxTracker.increaseFlux(fluxToSheild,true)
        damage?.modifier?.modifyMult(id,percent)
      }
      return id
    }

    /**
     * if true is returned, the hull damage to be taken is negated.
     * @param param
     * @param ship
     * @param point
     * @param damageAmount
     * @return
     */
    override fun notifyAboutToTakeHullDamage(param: Any?, ship: ShipAPI?, point: Vector2f?, damageAmount: Float): Boolean {
      if(damageAmount > ship?.hitpoints?: 999999f){
        shouldEnd = true
        return true
      }
      return false
    }
  }
}

//巡洋导弹引信插件
class aEP_CruiseMissile : BaseHullMod() {
  companion object {
    const val FUSE_RANGE = 150f
  }

  override fun advanceInCombat(ship: ShipAPI, amount: Float) {
    if (ship == null || !Global.getCombatEngine().isInPlay(ship) || !ship.isAlive) {
      return
    }
    ship.mutableStats.combatEngineRepairTimeMult.modifyFlat("aEP_CruiseMissile", 0.1f)
    ship.mutableStats.engineDamageTakenMult.modifyMult("aEP_CruiseMissile", 0f)
    ship.mutableStats.acceleration.unmodify()
    ship.mutableStats.maxSpeed.unmodify()
    ship.mutableStats.deceleration.unmodify()
    ship.mutableStats.maxTurnRate.unmodify()
    ship.mutableStats.turnAcceleration.unmodify()
    ship.isInvalidTransferCommandTarget = true
    for (s in Global.getCombatEngine().ships) {
      if (s.owner != ship.owner && !s.isFighter && !s.isDrone && !s.isShuttlePod) {
        if (MathUtils.getDistance(ship, s) <= FUSE_RANGE) {
          val point = ship.location
          val vel = Vector2f(0f, 0f)
          val yellowSmoke = Color(250, 250, 220)
          val engine = Global.getCombatEngine()
          //add red center smoke
          engine.spawnExplosion(
            point,
            vel,
            yellowSmoke,
            800f,
            4f
          )

          //add white center glow
          engine.addHitParticle(
            point,
            vel, 400f, 1f,
            Color.white
          )


          //随机烟雾
          var num = 1
          while (num <= 16) {
            val loc = MathUtils.getRandomPointInCircle(point, 600f)
            val sizeGrowth = MathUtils.getRandomNumberInRange(0, 100).toFloat()
            val sizeAtMin = MathUtils.getRandomNumberInRange(300f, 600f)
            val moveSpeed = MathUtils.getRandomNumberInRange(50, 200).toFloat()
            val smoke = aEP_MovingSmoke(loc)
            smoke.setInitVel(Speed2Velocity(VectorUtils.getAngle(point, loc), moveSpeed))
            smoke.fadeIn = 0.12f
            smoke.fadeOut = 0.88f
            smoke.lifeTime = 4f
            smoke.size = sizeAtMin
            smoke.sizeChangeSpeed = sizeGrowth
            smoke.color = Color(200, 200, 200, MathUtils.getRandomNumberInRange(100, 200))
            addVE(smoke)
            num++
          }

          //环状烟雾
          val SIZE_MULT = 4f
          val numMax = 24
          var angle = 0f
          for (i in 0 until numMax) {
            val loc = getExtendedLocationFromPoint(point, angle, 150f * SIZE_MULT)
            val sizeGrowth = 60 * SIZE_MULT
            val sizeAtMin = 175 * SIZE_MULT
            val moveSpeed = 60f * SIZE_MULT
            val smoke = aEP_MovingSmoke(loc)
            smoke.setInitVel(Speed2Velocity(VectorUtils.getAngle(point, loc), moveSpeed))
            smoke.fadeIn = 0.1f
            smoke.fadeOut = 0.7f
            smoke.lifeTime = 3f
            smoke.sizeChangeSpeed = sizeGrowth
            smoke.size = sizeAtMin
            smoke.color = Color(200, 200, 200, 75)
            addVE(smoke)
            angle += 360f / numMax
          }

          //生成弹丸
          //360/(36/2) = 20
          val numOfProj = 1
          angle = 0f
          while (num <= 240) {
            angle = angleAdd(angle, MathUtils.getRandomNumberInRange(1, 36).toFloat())
            val pro1 = engine.spawnProjectile(
              ship,  //source ship
              ship.allWeapons[0],  //source weapon,
              "aEP_CM_shot",  //whose proj to be use
              getExtendedLocationFromPoint(point, angle, 100f),  //loc
              angle,  //facing
              null
            )
            val damage = (pro1 as DamagingProjectileAPI).damage
            val damageSpec = pro1.projectileSpec.damage
            damage.damage = damageSpec.damage
            damage.type = damageSpec.type
            val speedRandom = MathUtils.getRandomNumberInRange(0.8f, 1.2f)
            val newX = pro1.getVelocity().x * speedRandom
            val newY = pro1.getVelocity().y * speedRandom
            pro1.getVelocity()[newX] = newY
            num++
          }
          //apply center explode damage
          val spec = DamagingExplosionSpec(
            1f,
            800f,
            FUSE_RANGE,
            2000f,
            500f,
            CollisionClass.MISSILE_FF,  //by ship
            CollisionClass.MISSILE_FF,  //by fighter
            0f, 0f, 0f, 0,
            Color.white, Color.white
          )
          spec.damageType = DamageType.HIGH_EXPLOSIVE
          engine.spawnDamagingExplosion(spec, ship, point)
          engine.combatNotOverFor = engine.combatNotOverFor + 10f
          killMissile(ship, engine)
          engine.removeEntity(ship)
          break
        }
      }
    }
  }
}
