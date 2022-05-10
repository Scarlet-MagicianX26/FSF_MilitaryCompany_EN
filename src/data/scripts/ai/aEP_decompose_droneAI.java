//by Tartiflette, for the guiding part
//by a111164
package data.scripts.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class aEP_decompose_droneAI implements ShipAIPlugin
{

  private static final float MOVEMENT_MOD_CHANGE_RANGE = 400f;//how far from target that drone change its movement mod
  private static final float ROTATE_RANGE = 100f;//how far from target that drone rotate around
  private static final float ROTATE_SPEED = 0.25f;// how many degree change per frame
  private static final float INTERVAL = 3f;//interval between two target search,by second
  private static final float REFORM_INTERVAL = 0.5f;//interval between reforms when toTarget == parentShip,by second
  private static final float REFORM_RANGE = 1.5f;//interval between reforms when toTarget == parentShip,by second
  private static final float REFORM_DIST_FROM_PARENT = 1.5f;//interval between reforms when toTarget == parentShip,by Colli
  private static final float SEARCH_TARGET_RANGE = 4000f;//max searching range, should equal to wing's max range
  private static final float RETURN_THRESHOLD = 8f;//after have how much supply than drone return
  private final ShipwideAIFlags flags = new ShipwideAIFlags();
  private final ShipAIConfig config = new ShipAIConfig();
  private CombatEntityAPI lastFrameTarget;
  private CombatEngineAPI engine;
  private final ShipAPI ship;
  private ShipAPI parentShip;
  private WeaponAPI weapon;
  private CombatEntityAPI toTarget;
  private Vector2f toTargetPo;
  private float timer;
  private float reformTimer;
  private float rotateCurrAngle;
  private boolean shouldDissipate = false;
  private boolean shouldReturn = false;

  public aEP_decompose_droneAI(FleetMemberAPI member, ShipAPI ship) {
    this.ship = ship;
    this.engine = Global.getCombatEngine();
  }

  public void cancelCurrentManeuver() {
  }

  public void forceCircumstanceEvaluation() {
  }

  public void setDoNotFireDelay(float amount) {
  }

  public ShipwideAIFlags getAIFlags() {
    return this.flags;
  }

  public boolean needsRefit() {
    return false;
  }

  public ShipAIConfig getConfig() {
    return this.config;
  }

  @Override
  public void advance(float amount) {
    ///////////////
    if (engine == null || engine.isPaused() || ship == null) {
      engine = Global.getCombatEngine();
      return;
    }

    //get parent ship
    if (ship.getWing().getSourceShip() == null) {
      parentShip = aEP_Tool.getNearestFriendCombatShip(ship);
    }
    else {
      parentShip = ship.getWing().getSourceShip();
    }

    if (parentShip == null || !parentShip.isAlive() || !ship.isAlive()) {
      return;
    }
    /////////////

    timer = timer + amount;
    reformTimer = reformTimer + amount;


    //all supplies check
    float allSupplies = 0f;
    for (WeaponAPI w : ship.getAllWeapons()) {
      weapon = w;
      if (engine.getCustomData().get("aEP_decompose_drone_supplies") != null) {
        if(((Map<WeaponAPI, Float>) engine.getCustomData().get("aEP_decompose_drone_supplies")).get(w) != null) {
          allSupplies += ((Map<WeaponAPI, Float>) engine.getCustomData().get("aEP_decompose_drone_supplies")).get(w);
        }
      }
      else {
        Map<WeaponAPI,Float> map = new HashMap<>();
        engine.getCustomData().put("aEP_decompose_drone_supplies",map);
      }
    }

    //should return check
    if (allSupplies >= RETURN_THRESHOLD && !shouldReturn) {
      shouldReturn = true;
    }


    //find a target
    if (!shouldReturn) {
      if (toTarget == null || timer >= INTERVAL) {
        toTarget = findTargetNow(parentShip, ship, SEARCH_TARGET_RANGE);
        if (toTarget == null) {
          toTarget = parentShip;
        }
        toTargetPo = toTarget.getLocation();
        timer = 0;
      }
    }

    //find toTargetPo according to target type
    if (!shouldReturn) {
      if (toTarget == parentShip) {
        if (reformTimer > REFORM_INTERVAL) {
          Vector2f centerPointAtBackOfParent = aEP_Tool.getExtendedLocationFromPoint(parentShip.getLocation(), parentShip.getFacing(), -(parentShip.getCollisionRadius()) * REFORM_DIST_FROM_PARENT);
          float randomAngle = (float) Math.random() * 360f;
          float randomDist = (float) Math.random() * parentShip.getCollisionRadius() * REFORM_RANGE;
          toTargetPo = aEP_Tool.getExtendedLocationFromPoint(centerPointAtBackOfParent, randomAngle, randomDist);
          reformTimer = 0f;
        }
      }
      else {
        if (lastFrameTarget != toTarget) {
          rotateCurrAngle = (float) Math.random() * 360f;
          float rotateDist = toTarget.getCollisionRadius() + ROTATE_RANGE;
          toTargetPo = aEP_Tool.getExtendedLocationFromPoint(toTarget.getLocation(), rotateCurrAngle, rotateDist);

        }
        else {
          rotateCurrAngle = rotateCurrAngle + ROTATE_SPEED;
          float rotateDist = toTarget.getCollisionRadius() + ROTATE_RANGE;
          toTargetPo = aEP_Tool.getExtendedLocationFromPoint(toTarget.getLocation(), rotateCurrAngle, rotateDist);
        }
      }
    }


    //null protection
    if (toTarget == null || toTargetPo == null || ship.getLocation() == null) {
      return;
    }
    ship.setShipTarget((ShipAPI) toTarget);
    float dist = MathUtils.getDistance(ship.getLocation(), toTargetPo);


    //shield check
    if (ship.getFluxLevel() > 0.95) {
      shouldDissipate = true;
    }
    if (ship.getFluxLevel() <= 0) {
      shouldDissipate = false;
    }

    if (!shouldDissipate) {
      ship.getShield().toggleOn();
    }
    else {
      ship.getShield().toggleOff();
    }

    //system AI check
    if (toTarget != parentShip) {

      if (dist <= 350f || Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getFacing(VectorUtils.getDirectionalVector(ship.getLocation(), toTargetPo)))) > 60f) {
        aEP_Tool.toggleSystemControl(ship, false);
      }


      if (dist > 350f && Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getFacing(VectorUtils.getDirectionalVector(ship.getLocation(), toTargetPo)))) <= 60f) {
        aEP_Tool.toggleSystemControl(ship, true);
      }

    }
    else {

      if (dist <= parentShip.getCollisionRadius() * REFORM_RANGE * 2) {
        aEP_Tool.toggleSystemControl(ship, false);
      }

      if (dist > parentShip.getCollisionRadius() * REFORM_RANGE * 2 && Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getFacing(VectorUtils.getDirectionalVector(ship.getLocation(), toTargetPo)))) < 60f) {
        aEP_Tool.toggleSystemControl(ship, true);
      }

    }


    //return parent if we should
    if (shouldReturn || (allSupplies > 0 && toTarget == parentShip)) {
      toTarget = parentShip;

      returnToParent(ship, parentShip, amount);
      return;
    }


    //normal move
    //Change movement mod due to range
    if (toTarget == parentShip) {
      if (MathUtils.getDistance(ship.getLocation(), toTargetPo) > parentShip.getCollisionRadius() * REFORM_RANGE * 2) {
        aEP_Tool.moveToPosition(ship, toTargetPo);
      }
      else {
        aEP_Tool.Util.setToPosition(ship, toTargetPo, amount);
        aEP_Tool.Util.moveToAngle(ship, parentShip.getFacing());
      }
      return;
    }


    if (dist > toTarget.getCollisionRadius() + MOVEMENT_MOD_CHANGE_RANGE) {
      aEP_Tool.moveToPosition(ship, toTargetPo);
    }
    else {
      aEP_Tool.Util.setToPosition(ship, toTargetPo, amount);
      aEP_Tool.Util.moveToAngle(ship, VectorUtils.getFacing(VectorUtils.getDirectionalVector(ship.getLocation(), toTarget.getLocation())));
    }


  }

  private CombatEntityAPI findTargetNow(ShipAPI parentShip, ShipAPI ship, float range) {
    if (parentShip == null || ship == null) {
      return null;
    }

    CombatEntityAPI closestShip = null;
    List<CombatEntityAPI> allEntities = CombatUtils.getEntitiesWithinRange(parentShip.getLocation(), range);
    float closestDist = SEARCH_TARGET_RANGE;
    for (CombatEntityAPI c : allEntities) {
      if (c instanceof ShipAPI && c != null) {
        ShipAPI s = (ShipAPI) c;
        if (s.isHulk() && !s.isPiece() && !s.isStation() && !s.isStationModule() && !s.isDrone() && !s.isFighter() && MathUtils.getDistance(s, ship) < closestDist) {
          closestShip = s;
          closestDist = MathUtils.getDistance(s, ship);
        }


      }
    }
    return closestShip;
  }


  //can't use method in AITool, we have supply to add
  void returnToParent(ShipAPI ship, ShipAPI parentShip, float amount) {
    ship.giveCommand(ShipCommand.HOLD_FIRE, null, 0);
    String id = "returnToParent";

    if (parentShip.getLaunchBaysCopy().size() <= 0) {
      Color callBackColor = (ship.getShield() != null ? ship.getShield().getInnerColor() : new Color(100, 100, 200, 100));
      Global.getCombatEngine().spawnExplosion(ship.getLocation(), //loc
        new Vector2f(0f, 0f), //velocity
        callBackColor, //color
        ship.getCollisionRadius() * 3f, //range
        0.5f);//duration
      Global.getCombatEngine().removeEntity(ship);
    }

    //landing check
    boolean landingStarted = ship.isLanding();

    toTargetPo = ship.getWing().getSource().getLandingLocation(ship);
    toTarget = parentShip;
    float dist = MathUtils.getDistance(ship.getLocation(), toTargetPo);

    if (dist > 100f) {
      aEP_Tool.moveToPosition(ship, toTargetPo);
      if (landingStarted) {
        ship.abortLanding();
      }
    }
    else {
      ship.getMutableStats().getMaxSpeed().modifyFlat(id, parentShip.getMaxSpeed() + ship.getMaxSpeed());
      aEP_Tool.Util.moveToAngle(ship, parentShip.getFacing());
      aEP_Tool.Util.setToPosition(ship, toTargetPo, amount);
    }

    if (dist <= 50f) {

      if (!landingStarted) {
        ship.beginLandingAnimation(parentShip);
        ship.getMutableStats().getMaxSpeed().modifyFlat(id, parentShip.getMaxSpeed() + ship.getMaxSpeed());
      }

    }

    if (ship.isFinishedLanding()) {
      if (aEP_Tool.checkCargoAvailable(engine, ship))// && engine.isInCampaign())
      {
        CargoAPI cargo = aEP_Tool.Util.getPlayerCargo();
        for (WeaponAPI w : ship.getAllWeapons()) {
          if (((Map<WeaponAPI, Float>) engine.getCustomData().get("aEP_decompose_drone_supplies")).get(w) != null) {
            float supplyNum = ((Map<WeaponAPI, Float>) engine.getCustomData().get("aEP_decompose_drone_supplies")).get(w);
            cargo.addSupplies(supplyNum);
            String toAdd = String.format("%s", (int) (supplyNum * 100) / 100);
            Global.getCombatEngine().addFloatingText(ship.getLocation(),
              "get " + toAdd + " supply",
              20f,
              new Color(200, 200, 50, 240),
              ship,
              0.25f, 120f);
            cargo.addCommodity("aEP_remain_part", supplyNum);
            ((Map<WeaponAPI, Float>) engine.getCustomData().get("aEP_decompose_drone_supplies")).put(w, 0f);
          }
          else {
            ((Map<WeaponAPI, Float>) engine.getCustomData().get("aEP_decompose_drone_supplies")).put(w, 0f);
          }
        }

      }
      ship.getWing().getSource().land(ship);
    }

  }


}