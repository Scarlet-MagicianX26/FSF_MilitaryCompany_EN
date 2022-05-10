package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class aEP_DecomposeDroneBeamEffect implements BeamEffectPlugin
{

  static final float PARTS_CHANCE = 0.25f;
  private static final int INTERVAL_TIME = 10;//for beam effect, 10 = 1 second
  private static final float CONVERT_EFFICIENCY = 1f / 400f;//1 means 1 damage convert to 1 supply
  private static final Map<String, Float> FRAGMENTATION_CORE_DAMAGE = new HashMap();

  static {
    FRAGMENTATION_CORE_DAMAGE.put("aEP_tearing_beam", 1200f);
    FRAGMENTATION_CORE_DAMAGE.put("aEP_tearing_beam_fighter", 400f);
  }


  private int timer = 0;
  private CargoAPI cargo;
  private String id;

  @Override
  public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
    ShipAPI ship = beam.getWeapon().getShip();
    Map<WeaponAPI,Float> map = new HashMap<>();
    if (engine.getCustomData().get("aEP_decompose_drone_supplies") != null) {
      map = ((Map<WeaponAPI, Float>) engine.getCustomData().get("aEP_decompose_drone_supplies"));
    }
    else {
      engine.getCustomData().put("aEP_decompose_drone_supplies", map);
    }

    if (!ship.isFighter() && !ship.isDrone() && map.get(beam.getWeapon()) != null) {
      float storedSupply = map.get(beam.getWeapon());
      //Global.getCombatEngine().addFloatingText(beam.getWeapon().getLocation(),(Float)storedSupplies.get(beam.getWeapon()) + "",20f ,new Color (200, 200, 50, 240),beam.getWeapon().getShip(),0.25f, 120f);
      if (storedSupply > 1f) {
        if (checkCargoAvailable(engine, ship) == true) {
          cargo = getCargo(engine, ship);
          cargo.addSupplies(1f);
          map.put(beam.getWeapon(), storedSupply - 1f);
          Global.getCombatEngine().addFloatingText(beam.getWeapon().getLocation(),
            "get 1 supply",
            20f,
            new Color(200, 200, 50, 240),
            beam.getWeapon().getShip(),
            0.25f, 120f);
          if (MathUtils.getRandomNumberInRange(0f, 1f) < PARTS_CHANCE)
            cargo.addCommodity("aEP_remain_part", 1);
        }

      }
    }

    //Global.getCombatEngine().addFloatingText(beam.getWeapon().getLocation(),(Float)storedSupplies.get(beam.getWeapon()) + "",20f ,new Color (200, 200, 50, 240),beam.getWeapon().getShip(),0.25f, 120f);

    if (beam.didDamageThisFrame() && beam.getDamageTarget() instanceof ShipAPI && ((ShipAPI) beam.getDamageTarget()).isHulk() && !((ShipAPI) beam.getDamageTarget()).isPiece()) {
      timer = timer + 1;
      //Global.getCombatEngine().addFloatingText(beam.getFrom(), timer + "", 20f ,new Color (0, 100, 200, 240),beam.getSource(), 0.25f, 120f);
      if (timer > INTERVAL_TIME) {
        //annihilate damage deduce due to armor
        ((ShipAPI) beam.getDamageTarget()).getMutableStats().getEffectiveArmorBonus().modifyPercent("aEP_DecomposeDroneBeamEffect", -1000f);
        beam.getDamageTarget().getVelocity().scale(0.8f);
        float damage = FRAGMENTATION_CORE_DAMAGE.get(beam.getWeapon().getSpec().getWeaponId());
        DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f,//float duration,
          5f,//float radius,
          1f,//float coreRadius,
          damage,//float maxDamage,
          damage / 4f,//float minDamage,
          CollisionClass.PROJECTILE_NO_FF, //collisionClass,
          CollisionClass.PROJECTILE_FIGHTER, //collisionClassByFighter,
          0.5f,//float particleSizeMin,
          1f,//float particleSizeRange,
          0.5f,//float particleDuration,
          6,//int particleCount,
          new Color(250, 250, 100, 200),//java.awt.Color particleColor,
          new Color(200, 200, 150, 100));//java.awt.Color explosionColor);
        spec.setDamageType(DamageType.FRAGMENTATION);

        DamagingProjectileAPI fragmentationDamage = engine.spawnDamagingExplosion(spec,//DamagingExplosionSpec spec,
          beam.getSource(),//ShipAPI source,
          beam.getTo(),//Vector2f location,
          false);//boolean canDamageSource);

        float computedDamage = Math.abs(fragmentationDamage.getDamage().getDamage() * CONVERT_EFFICIENCY);
        float currSupplies = 0f;
        if (map.get(beam.getWeapon()) == null)
          map.put(beam.getWeapon(), computedDamage);
        else {
          currSupplies = map.get(beam.getWeapon());
          map.put(beam.getWeapon(), computedDamage + currSupplies);
        }


        timer = 0;
      }


    }

  }

  private CargoAPI getCargo(CombatEngineAPI engine, ShipAPI ship) {
    if (engine.getPlayerShip().getOwner() != ship.getOwner())
      return ship.getFleetMember().getFleetData().getFleet().getCargo();
    return Global.getSector().getPlayerFleet().getCargo();
  }

  private boolean checkCargoAvailable(CombatEngineAPI engine, ShipAPI ship) {
    return engine.isInCampaign() && !engine.isInCampaignSim() && ship != null && ship.getOwner() == 0;

  }
}

