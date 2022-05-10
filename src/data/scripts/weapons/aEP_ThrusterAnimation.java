package data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class aEP_ThrusterAnimation implements EveryFrameWeaponEffectPlugin
{

  private static final float FLAME_CHANGE_SPEED = 80 / 30f;// OriginalHeight/30 = total fade/full in half second
  private static final Color flameGlowColor = new Color(150, 150, 25, 20);
  private static final float maxFlameState = 30;// 0 == non flame, control flame length
  private AnimationAPI anime;
  private SpriteAPI sprite;
  private WeaponAPI weapon;
  private ShipAPI ship;
  private ShipEngineControllerAPI.ShipEngineAPI e;
  private float flameState = 0;//initial open state
  private final float animeFrameInterval = 3;//control anima play speed
  private float animeTimer = 0;
  private float amount = 0f;
  private final float ORIGINAL_HEIGHT = 80f;
  private final float ORIGINAL_WIDTH = 10f;

  @Override
  public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

    this.ship = weapon.getShip();
    this.weapon = weapon;
    this.anime = weapon.getAnimation();
    this.amount = amount;

    if (engine.isPaused()) {
      return;
    }


    if (ship.getSystem().getEffectLevel() < 0.99) {

      flameState = 0;
    }
    else {
      float toFlameState = getIntegrateAccelerate(ship, weapon);
      if (flameState < toFlameState * maxFlameState) {
        flameState = flameState + 1;
      }
      else {
        flameState = flameState - 1;
      }
    }

    flameState = MathUtils.clamp(flameState, 0, maxFlameState);
    //set size for all frames of anime
    int originalFrame = anime.getFrame();
    for (int i = 0; i < anime.getNumFrames() - 1; i++) {
      anime.setFrame(i);
      sprite = weapon.getSprite();
      controlFlameLength(flameState / maxFlameState, sprite);
    }
    anime.setFrame(originalFrame);


    animeTimer = animeTimer + 1;
    if (animeTimer > animeFrameInterval) {
      if (anime.getFrame() < anime.getNumFrames() - 1) {
        anime.setFrame(anime.getFrame() + 1);
      }
      else {
        anime.setFrame(1);// frame 0 is empty
      }
      animeTimer = 0;
    }
    sprite = weapon.getSprite();
    controlFlameLength(flameState / maxFlameState, sprite);


  }


  private void controlFlameLength(float flameLevel, SpriteAPI thisFrameSprite) {

    float flameLevelNow = thisFrameSprite.getHeight() / ORIGINAL_HEIGHT;
    Vector2f endOfFlame = aEP_Tool.getExtendedLocationFromPoint(weapon.getLocation(), weapon.getCurrAngle(), thisFrameSprite.getHeight());
    Vector2f speedVector = aEP_Tool.getDistVector(weapon.getLocation(), endOfFlame);
    speedVector = VectorUtils.resize(speedVector, thisFrameSprite.getHeight() * 10 / 2);
    speedVector = Vector2f.add(speedVector, ship.getVelocity(), null);
    float length = 0f;
    float widthDecreasePerLength = thisFrameSprite.getWidth() / thisFrameSprite.getHeight();
    while (length < thisFrameSprite.getHeight() / 2f) {
      float size = (thisFrameSprite.getWidth() - widthDecreasePerLength * length) * 2f;
      Global.getCombatEngine().addSmoothParticle(aEP_Tool.getExtendedLocationFromPoint(weapon.getLocation(), weapon.getCurrAngle(), length),//loc
        new Vector2f(0f, 0f),//speed
        size,//size
        flameLevel,//brightness
        amount,//duration
        new Color(flameGlowColor.getRed(), flameGlowColor.getGreen(), flameGlowColor.getBlue(), (int) (flameGlowColor.getAlpha() * (1 - length / (thisFrameSprite.getHeight() / 2f)))));
      length = length + (size) / 6f + 1f;
    }
    if (flameLevel < flameLevelNow) {
      if (flameLevelNow - flameLevel > FLAME_CHANGE_SPEED) {
        thisFrameSprite.setSize(ORIGINAL_WIDTH * flameLevelNow - FLAME_CHANGE_SPEED, ORIGINAL_HEIGHT * flameLevelNow - FLAME_CHANGE_SPEED);
      }
      else {
        thisFrameSprite.setSize(ORIGINAL_WIDTH * flameLevel, ORIGINAL_HEIGHT * flameLevel);
      }
    }
    else {
      if (flameLevel - flameLevelNow > FLAME_CHANGE_SPEED) {
        thisFrameSprite.setSize(ORIGINAL_WIDTH * flameLevelNow + FLAME_CHANGE_SPEED, ORIGINAL_HEIGHT * flameLevelNow + FLAME_CHANGE_SPEED);
      }
      else {
        thisFrameSprite.setSize(ORIGINAL_WIDTH * flameLevel, ORIGINAL_HEIGHT * flameLevel);
      }
    }
    thisFrameSprite.setCenter(ORIGINAL_WIDTH * flameLevel / 2, ORIGINAL_HEIGHT * flameLevel / 2);
  }

  private float getIntegrateAccelerate(ShipAPI ship, WeaponAPI weapon) {
    if (ship.getEngineController().isDecelerating()) {
      if (aEP_Tool.Velocity2Speed(ship.getVelocity()).y < 5) {
        return 0;
      }
      float stopAngle = aEP_Tool.angleAdd(VectorUtils.getFacing(ship.getVelocity()), 0f);
      float shortestRotation = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), stopAngle));
      if (shortestRotation < 90) {
        return 1 - shortestRotation / 90;
      }
      return 0;
    }

    if (ship.getEngineController().isTurningLeft()) {
      if (weapon.getSlot().getAngle() < 0 && weapon.getSlot().getLocation().getX() > 0) {
        if (weapon.getSlot().getLocation().getX() / (weapon.getShip().getSpriteAPI().getHeight() / 2) > 0.75f) {
          return 1;
        }
        else {
          return 1 * (weapon.getSlot().getLocation().getX() / (0.75f * weapon.getShip().getSpriteAPI().getHeight() / 2));
        }

      }

      if (weapon.getSlot().getAngle() > 0 && weapon.getSlot().getLocation().getX() < 0) {
        if (Math.abs(weapon.getSlot().getLocation().getX() / (weapon.getShip().getSpriteAPI().getHeight() / 2)) > 0.75f) {
          return 1;
        }
        else {
          return 1 * Math.abs((weapon.getSlot().getLocation().getX() / (0.75f * weapon.getShip().getSpriteAPI().getHeight() / 2)));
        }

      }
      return 0;
    }

    if (ship.getEngineController().isTurningRight()) {
      if (weapon.getSlot().getAngle() > 0 && weapon.getSlot().getLocation().getX() > 0) {
        if (weapon.getSlot().getLocation().getX() / (weapon.getShip().getSpriteAPI().getHeight() / 2) > 0.75f) {
          return 1;
        }
        else {
          return 1 * (weapon.getSlot().getLocation().getX() / (0.75f * weapon.getShip().getSpriteAPI().getHeight() / 2));
        }

      }

      if (weapon.getSlot().getAngle() < 0 && weapon.getSlot().getLocation().getX() < 0) {
        if (Math.abs(weapon.getSlot().getLocation().getX() / (weapon.getShip().getSpriteAPI().getHeight() / 2)) > 0.75f) {
          return 1;
        }
        else {
          return 1 * Math.abs((weapon.getSlot().getLocation().getX() / (0.75f * weapon.getShip().getSpriteAPI().getHeight() / 2)));
        }

      }
      return 0;
    }

    if (ship.getEngineController().isStrafingLeft()) {
      float stopAngle = aEP_Tool.angleAdd(ship.getFacing(), 270f);
      float shortestRotation = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), stopAngle));
      if (shortestRotation < 90) {
        return 1 - shortestRotation / 90;
      }
      return 0;
    }

    if (ship.getEngineController().isStrafingRight()) {
      float stopAngle = aEP_Tool.angleAdd(ship.getFacing(), 90f);
      float shortestRotation = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), stopAngle));
      if (shortestRotation < 90) {
        return 1 - shortestRotation / 90;
      }
      return 0;
    }

    if (ship.getEngineController().isAcceleratingBackwards()) {
      float stopAngle = aEP_Tool.angleAdd(ship.getFacing(), 0f);
      float shortestRotation = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), stopAngle));
      if (shortestRotation < 90) {
        return 1 - shortestRotation / 90;
      }
      return 0;
    }

    if (ship.getEngineController().isAccelerating()) {
      float stopAngle = aEP_Tool.angleAdd(ship.getFacing(), 180f);
      float shortestRotation = Math.abs(MathUtils.getShortestRotation(weapon.getCurrAngle(), stopAngle));
      if (shortestRotation < 90) {
        return 1 - shortestRotation / 90;
      }
      return 0;
    }


    return 0;
  }

  private ShipEngineControllerAPI.ShipEngineAPI getNearestEngine(WeaponAPI w, ShipAPI ship) {
    float findDist = 30f;
    ShipEngineControllerAPI.ShipEngineAPI shipEngine = null;
    if (ship.getEngineController() == null || ship.getEngineController().getShipEngines() == null) {
      return null;
    }
    for (ShipEngineControllerAPI.ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
      if (MathUtils.getDistance(weapon.getLocation(), eng.getEngineSlot().computePosition(ship.getLocation(), ship.getFacing())) <= findDist) {
        shipEngine = eng;
        findDist = MathUtils.getDistance(weapon.getLocation(), eng.getLocation());
      }
    }
    return shipEngine;
  }

}

        	         	       	       	 
