package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.IntervalUtil;
import combat.impl.VEs.aEP_MovingSmoke;
import combat.impl.VEs.aEP_SpreadRing;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import combat.impl.aEP_BaseCombatEffect;
import combat.plugin.aEP_CombatEffectPlugin;
import data.scripts.util.MagicAnim;
import org.lwjgl.opengl.GL20;
import shaders.aEP_BloomMask;
import shaders.aEP_BloomShader;
import combat.util.aEP_Tool;
import combat.util.aEP_DataTool;
import data.scripts.weapons.aEP_FCLAnimation;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static combat.util.aEP_DataTool.txt;
import static java.lang.Math.PI;

public class aEP_ExtremeOverloadScript extends BaseShipSystemScript
{
  static final float MAX_ROF_BUFF = 2.5f;
  static final float FLUX_PERCENT_TO_OVERLOAD_TIME = 0.1f;//how many percent accumulated flux of total flux convert to 1 second overload
  static final float MAX_OVERLOAD_TIME = 8f;
  static final float EXTRA_CAP = 6000f;
  static final float FLUX_MULT_AFTER = 3f;

  static final Color RING_COLOR = new Color(180, 90, 90, 65);
  static final float DEFLEX_RANGE = 1000f;
  static final float RING_WIDTH = 400f;
  static final float FRINGE_WIDTH = 80f;
  static final float SPREAD_SPEED = 1000f;
  static final float DAMAGE_TO_MISSILE = 160f;


  aEP_DataTool.floatDataRecorder accumulatedFlux = new aEP_DataTool.floatDataRecorder();

  float overloadTime = 0f;
  CombatEngineAPI engine = Global.getCombatEngine();
  float amount;


  float smokeTimer = 0;


  @Override   //run every frame
  public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

    ShipAPI ship = (ShipAPI) stats.getEntity();


    //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),totalHardConverted.getTotal() + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,5f);


    //add all generated softflux
    float softFluxNow = ship.getFluxTracker().getCurrFlux() - ship.getFluxTracker().getHardFlux();
    accumulatedFlux.addRenewData(softFluxNow);
    //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),totalHardConverted.getTotal() + "",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,5f);


    if (accumulatedFlux.getTotal() > ship.getFluxTracker().getMaxFlux() * FLUX_PERCENT_TO_OVERLOAD_TIME * MAX_OVERLOAD_TIME)
      overloadTime = MAX_OVERLOAD_TIME;
    else
      overloadTime = accumulatedFlux.getTotal() / (FLUX_PERCENT_TO_OVERLOAD_TIME * ship.getFluxTracker().getMaxFlux());


    float chargeLevel = 1f - (1f-effectLevel) * (1f-effectLevel);


    //smoke while boosting
    smokeTimer = smokeTimer + amount;
    if (smokeTimer > 0.1f) {
      smokeTimer = smokeTimer - 0.1f;
      for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
        if (slot.getWeaponType() == WeaponAPI.WeaponType.SYSTEM) {
          float angle = aEP_Tool.angleAdd(slot.getAngle(), ship.getFacing()) + MathUtils.getRandomNumberInRange(-slot.getArc() / 2f, slot.getArc() / 2f);
          aEP_MovingSmoke smoke = new aEP_MovingSmoke(slot.computePosition(ship));
          smoke.setInitVel(aEP_Tool.Util.Speed2Velocity(angle, 40));
          smoke.setInitVel(ship.getVelocity());
          smoke.setStopSpeed(0.9f);
          smoke.setFadeIn(0f);
          smoke.setFadeOut(1f);
          smoke.setLifeTime(effectLevel*4f);
          smoke.setSize(10f);
          smoke.setSizeChangeSpeed(15);
          smoke.setColor(new Color(150 + (int) (effectLevel * 60), 180, 180, (int) (20 * effectLevel) + 40));
          aEP_CombatEffectPlugin.Mod.addVE(smoke);
        }

      }
    }

    //move deco weapon
    openDeco(ship, effectLevel);

    //buff ROF and extra flux caps
    stats.getBallisticRoFMult().modifyFlat(id, chargeLevel * MAX_ROF_BUFF);
    stats.getFluxCapacity().modifyFlat(id, EXTRA_CAP);
    stats.getBallisticAmmoRegenMult().modifyMult(id,chargeLevel* MAX_ROF_BUFF);

    //add weapon glow and jitter
    ship.setWeaponGlow(0.3f * chargeLevel,//float glow,
      new Color(240, 240, 150, 100),//java.awt.Color color,
      EnumSet.of(WeaponAPI.WeaponType.BALLISTIC));//java.util.EnumSet<WeaponAPI.WeaponType> types)


    //jitter will stop itself once it not been called
    ship.setJitter(ship,
      new Color(120, 50, 50, (int) (80 * effectLevel)),
      chargeLevel,// intensity
      4,//copies
      10f * chargeLevel);// range
  }


  @Override  //run once when unapply
  public void unapply(MutableShipStatsAPI stats, String id) {
    ShipAPI ship = (ShipAPI) stats.getEntity();

    //在这修改数值
    stats.getBallisticRoFMult().unmodify(id);
    stats.getFluxCapacity().unmodify(id);
    stats.getBallisticAmmoRegenMult().unmodify(id);


    //stop weapon glowing
    ship.setWeaponGlow(0f,//float glow,
      new Color(240, 240, 150, 100),//java.awt.Color color,
      EnumSet.allOf(WeaponAPI.WeaponType.class));//java.util.EnumSet<WeaponAPI.WeaponType> types)

    //move decos
    openDeco(ship, 0f);

    if (overloadTime > 0.1f)
      endSystem(ship);

    accumulatedFlux.reset();
    overloadTime = 0f;
  }


  @Override
  public StatusData getStatusData(int index, State state, float effectLevel) {
    if (index == 0) {
      return new StatusData(txt("ExtremeOverload01") + ": " + (int) accumulatedFlux.getTotal(), false);
    }
    else if (index == 1) {
      return new StatusData(txt("ExtremeOverload02") + ": " + ((int) (overloadTime * 100f)) / 100f, true);
    }
    return null;
  }

  @Override
  public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
    return null;
  }

  void openDeco(ShipAPI ship, float effectLevel) {
    //move deco weapon
    for (WeaponAPI weapon : ship.getAllWeapons()) {
      if (weapon.getSlot().getId().contains("AM")) {
        float to = MathUtils.clamp(effectLevel * 2f, 0f, 1f);
        if (weapon.getSlot().getId().contains("01") || weapon.getSlot().getId().contains("04")) {
          ((aEP_FCLAnimation) weapon.getEffectPlugin()).getDecoMoveController().setRange(6f);
          ((aEP_FCLAnimation) weapon.getEffectPlugin()).setMoveToLevel(to);
        }
        if (weapon.getSlot().getId().contains("02") || weapon.getSlot().getId().contains("05")) {
          ((aEP_FCLAnimation) weapon.getEffectPlugin()).getDecoMoveController().setRange(18f);
          ((aEP_FCLAnimation) weapon.getEffectPlugin()).setMoveToLevel(to);
        }
        if (weapon.getSlot().getId().contains("03")) {
          ((aEP_FCLAnimation) weapon.getEffectPlugin()).setMoveToLevel(effectLevel);
          ((aEP_FCLAnimation) weapon.getEffectPlugin()).setRevoToLevel(to);
        }
        if (weapon.getSlot().getId().contains("06")) {
          ((aEP_FCLAnimation) weapon.getEffectPlugin()).setMoveToLevel(effectLevel);
          ((aEP_FCLAnimation) weapon.getEffectPlugin()).setRevoToLevel(to);
        }
      }
      if (weapon.getSlot().getId().contains("LM")) {
        float to = MathUtils.clamp((effectLevel - 0.5f) * 2f, 0f, 1f);
        ((aEP_FCLAnimation) weapon.getEffectPlugin()).setMoveToLevel(to);
      }
      if (weapon.getSlot().getId().contains("GW")) {
        float to = MathUtils.clamp((effectLevel - 0.5f) * 2f, 0f, 1f);
        //((aEP_FCLAnimation) weapon.getEffectPlugin()).setMoveToLevel(to);
        ((aEP_FCLAnimation) weapon.getEffectPlugin()).setGlowToLevel(effectLevel);
        //aEP_Tool.addDebugText(""+weapon.getAnimation().getFrame());
      }
    }
  }

  void endSystem(ShipAPI ship) {
    //give a stop weapon debuff
    aEP_CombatEffectPlugin.Mod.addEffect(new stopWeapon(overloadTime, ship));
    //create emp arc
    float facing = 0f;
    while (facing < 360) {
      Global.getCombatEngine().spawnEmpArc(ship,//ShipAPI damageSource,
        aEP_Tool.getExtendedLocationFromPoint(ship.getLocation(), facing, (float) Math.random() * ship.getCollisionRadius() * 2f),// Vector2f point,
        ship,// CombatEntityAPI pointAnchor,
        ship,// CombatEntityAPI empTargetEntity,
        DamageType.ENERGY,// DamageType damageType,
        0f,// float damAmount,
        0f,// float empDamAmount,
        ship.getCollisionRadius() * 4f,// float maxRange,
        null,// java.lang.String impactSoundId,
        25f,// float thickness,
        new Color(100, 100, 100, 80),// java.awt.Color fringe,
        new Color(150, 50, 50, 120));// java.awt.Color core)
      facing += (float) Math.random() * 20f;
    }

    //create distortion
    WaveDistortion wave = new WaveDistortion(ship.getLocation(), new Vector2f(0, 0));
    wave.setSize(DEFLEX_RANGE);
    wave.setLifetime(0.5f);
    wave.fadeInSize(0.5f);
    wave.setIntensity(20f);
    wave.fadeOutIntensity(1f);
    DistortionShader.addDistortion(wave);

    //create ring
    aEP_SpreadRing ring = new DeflexRing(
      SPREAD_SPEED,
      RING_WIDTH,
      RING_COLOR,
      0f,
      DEFLEX_RANGE,
      ship.getLocation(),
      ship);
    ring.setLayers(EnumSet.of(CombatEngineLayers.BELOW_SHIPS_LAYER));
    ring.getInitColor().setToColor(0,0,0,0,1);
    aEP_CombatEffectPlugin.Mod.addEffect(ring);


    //create ring fringe
    aEP_SpreadRing ringFringe = new aEP_SpreadRing(
      SPREAD_SPEED,
      FRINGE_WIDTH,
      new Color(250, 250, 250, 120),
      0f,
      DEFLEX_RANGE,
      ship.getLocation());
    ringFringe.getInitColor().setToColor(0, 0, 0, 0, 2f);
    ringFringe.setLayers(EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER));
    ringFringe.getEndColor().setColor(200, 200, 200, 0);
    ringFringe.getInitColor().setToColor(0,0,0,0,1);
    aEP_CombatEffectPlugin.Mod.addEffect(ringFringe);
    //aEP_BloomShader.add(ringFringe);
    //Global.getSoundPlayer().playSound("");

    //生成烟雾
    //生成雾气
    float i = 0f;
    while (i < 360f){
      i += MathUtils.getRandomNumberInRange(15,30);
      Vector2f p = aEP_Tool.getExtendedLocationFromPoint(ship.getLocation(),i,MathUtils.getRandomNumberInRange(ship.getCollisionRadius(),ship.getCollisionRadius()+300f));
      Global.getCombatEngine().addNebulaSmokeParticle(p,
              new Vector2f(MathUtils.getRandomNumberInRange(-50,50),MathUtils.getRandomNumberInRange(-50,50)),
              300f,
              1.5f,
              0.15f,
              1f,
              3f,
              new Color(255,225,225,40)
      );
    }

    aEP_BaseCombatEffect blur = new Blur(ship, ship.getCollisionRadius(), 3f);
    blur.setRenderInShader(true);
    aEP_CombatEffectPlugin.Mod.addEffect(blur);
    aEP_BloomShader.add(blur);

  }

  class stopWeapon extends aEP_BaseCombatEffect {
    ShipAPI ship;

    stopWeapon(float time, ShipAPI entity) {
      ship = entity;
      setEntity(ship);
      setLifeTime(time);


      ship.getMutableStats().getVentRateMult().modifyMult("aEP_stopWeapon", 0f);
      ship.getMutableStats().getFluxDissipation().modifyMult("aEP_stopWeapon", FLUX_MULT_AFTER);
    }

    @Override
    public void advanceImpl(float amount) {
      //start weapon glowing
      ship.setWeaponGlow(1f,//float glow,
              new Color(240, 240, 150, 100),//java.awt.Color color,
              EnumSet.of(WeaponAPI.WeaponType.BALLISTIC));//java.util.EnumSet<WeaponAPI.WeaponType> types)
      ship.setHoldFireOneFrame(true);
      ship.blockCommandForOneFrame(ShipCommand.FIRE);
    }

    @Override
    public void readyToEnd() {
      ship.setWeaponGlow(0f,//float glow,
              new Color(240, 240, 150, 100),//java.awt.Color color,
              EnumSet.of(WeaponAPI.WeaponType.BALLISTIC));//java.util.EnumSet<WeaponAPI.WeaponType> types)
      ship.getMutableStats().getVentRateMult().unmodify("aEP_stopWeapon");
      ship.getMutableStats().getFluxDissipation().unmodify("aEP_stopWeapon");
    }
  }

  class DeflexRing extends aEP_SpreadRing {
    CombatEntityAPI entity;
    List<CombatEntityAPI> list = new ArrayList<>();

    public DeflexRing(float speed, float width, Color initColor, float startRadius, float endRadius, Vector2f center, CombatEntityAPI entity) {
      super(speed, width, initColor, startRadius, endRadius, center);
      this.entity = entity;
    }

    @Override
    public void advanceImpl(float amount) {
      //Global.getLogger(this.getClass()).info("actived");
      for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(getCenter(), DEFLEX_RANGE)) {
        if (proj.getSource().getOwner() != entity.getOwner() && MathUtils.getDistance(proj.getLocation(), entity.getLocation()) < getRingRadius()) {
          if (list.contains(proj)) continue;
          float projFacing = VectorUtils.getFacing(proj.getVelocity());
          float projLocToShipAngle = VectorUtils.getAngle(proj.getLocation(), entity.getLocation());

          float angleDist = MathUtils.getShortestRotation(projLocToShipAngle, projFacing);
          if (Math.abs(angleDist) < 60) {
            if (angleDist > 0) {
              projFacing = aEP_Tool.angleAdd(projLocToShipAngle, 60);
            }
            else {
              projFacing = aEP_Tool.angleAdd(projLocToShipAngle, -60);
            }
            float projSpeed = aEP_Tool.Velocity2Speed(proj.getVelocity()).y;
            Vector2f projNewVelocity = aEP_Tool.Util.Speed2Velocity(projFacing, projSpeed);
            proj.setFacing(projFacing);
            proj.getVelocity().setX(projNewVelocity.getX());
            proj.getVelocity().setY(projNewVelocity.getY());
            float effectiveDamage = proj.getDamage().getDamage();
            if (proj.getDamage().getType() == DamageType.FRAGMENTATION) effectiveDamage /= 4f;
            list.add(proj);
          }
        }
      }
      for (DamagingProjectileAPI proj : CombatUtils.getMissilesWithinRange(getCenter(), DEFLEX_RANGE)) {
        if (proj.getSource().getOwner() != entity.getOwner() && MathUtils.getDistance(proj.getLocation(), entity.getLocation()) < getRingRadius()) {
          if (proj.getCollisionClass() == null || proj.getCollisionClass() == CollisionClass.NONE) continue;
          if (list.contains(proj)) continue;
          engine.applyDamage(proj,//target
                  proj.getLocation(),//point
                  DAMAGE_TO_MISSILE,//damage
                  DamageType.FRAGMENTATION,
                  0f,
                  true,//deal softflux
                  true,//is bypass shield
                  proj);//damage source
          list.add(proj);
          float effectiveDamage = proj.getDamage().getDamage();
          if (proj.getDamage().getType() == DamageType.FRAGMENTATION) effectiveDamage /= 4f;
        }
      }

    }
  }

  class Blur extends aEP_BloomMask {
    float toRenderRadius;
    CombatEntityAPI anchor;

    public Blur(CombatEntityAPI anchor, float toRenderRadius, float lifeTime) {
      this.toRenderRadius = toRenderRadius;
      this.anchor = anchor;
      setLifeTime(lifeTime);
    }

    @Override
    public void draw() {
      aEP_BloomShader.setLevelX(4f *(1 - getTime()/getLifeTime()) );
      aEP_BloomShader.setLevelY(4f *(1 - getTime()/getLifeTime()) );
      aEP_BloomShader.setLevelAlpha(1f - getTime()/getLifeTime() );

      //begin
      int numOfVertex = 36;
      GL11.glBegin(GL11.GL_POLYGON);
      Vector2f center = anchor.getLocation();
      Vector2f converted = convertPositionToScreen(center);
      GL11.glTexCoord2f(converted.x, converted.y);
      GL11.glVertex2f(center.x, center.y);

      for (int i = 0; i <= numOfVertex; i++) {
        float r = toRenderRadius;
        Vector2f pointNear = new Vector2f(center.x + r * (float) FastTrig.cos((2f * PI * i) / numOfVertex), center.y + r * (float) FastTrig.sin((2f * PI * i) / numOfVertex));
        converted = convertPositionToScreen(pointNear);
        GL11.glTexCoord2f(converted.x, converted.y);
        GL11.glVertex2f(pointNear.x, pointNear.y);
      }

      GL11.glEnd();
    }
  }
}