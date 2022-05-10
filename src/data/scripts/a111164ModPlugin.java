package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.ai.*;
import data.scripts.campaign.a111164CampaignPlugin;
import data.scripts.world.aEP_gen;


public class a111164ModPlugin extends BaseModPlugin
{
  public static final String RepairDrone_ID = "aEP_repairing_drone";
  public static final String DecomposeDrone_ID = "aEP_decompose_drone";
  public static final String DefenseDrone_ID = "aEP_defense_drone";
  public static final String AnMianDrone_ID = "aEP_anmian_drone";
  public static final String BB_Radar_ID = "aEP_BB_radar";
  public static final String TearingBeam_ID = "aEP_tearing_beam";
  public static final String TearingBeamFighter_ID = "aEP_tearing_beam_fighter";
  public static final String HLRepairBeam_ID = "aEP_HL_drone_repairbeam";


  //shipAI plugin pick
  @Override
  public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
    if (ship.getHullSpec().getHullId().equals(RepairDrone_ID)) {
      return new PluginPick<ShipAIPlugin>(new aEP_repairing_droneAI(member, ship), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    else if (ship.getHullSpec().getHullId().equals(DefenseDrone_ID)) {
      return new PluginPick<ShipAIPlugin>(new aEP_defense_droneAI(member, ship), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    else if (ship.getHullSpec().getHullId().equals(AnMianDrone_ID)) {
      return new PluginPick<ShipAIPlugin>(new aEP_defense_droneAI(member, ship), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    else if (ship.getHullSpec().getHullId().equals(DecomposeDrone_ID)) {
      return new PluginPick<ShipAIPlugin>(new aEP_decompose_droneAI(member, ship), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    else if (ship.getHullSpec().getHullId().equals("aEP_CM")) {
      member.setCaptain(null);
      return new PluginPick<ShipAIPlugin>(new aEP_CruiseMissileAI(ship,null), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    else if (ship.getHullSpec().getHullId().equals("aEP_MaoDian_drone")) {
      member.setCaptain(null);
      return new PluginPick<ShipAIPlugin>(new aEP_MaoDianDroneAI(ship), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    return null;
  }

  @Override
  public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
    switch (missile.getProjectileSpecId()) {
      case "aEP_harpoon_missile":
      return new PluginPick<MissileAIPlugin>(new aEP_MissileAI(missile,launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    return null;
  }

  //weaponAI plugin pick
  @Override
  public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {

    if (weapon.getId().equals(TearingBeam_ID)) {
      return new PluginPick<AutofireAIPlugin>(new aEP_tearing_beamAI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    if (weapon.getId().equals(TearingBeamFighter_ID)) {
      return new PluginPick<AutofireAIPlugin>(new aEP_tearing_beamAI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    if (weapon.getId().equals(BB_Radar_ID)) {
      //return new PluginPick<AutofireAIPlugin>(new aEP_BB_radarAI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }
    if (weapon.getId().equals(HLRepairBeam_ID)) {
      return new PluginPick<AutofireAIPlugin>(new aEP_HL_repairbeam_AI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }


    return null;
  }

  //create a sector
  @Override
  public void onNewGame() {

    new aEP_gen().generate(Global.getSector());
    SharedData.getData().getPersonBountyEventData().addParticipatingFaction("aEP_FSF");
    SectorAPI sector = Global.getSector();
    if (!sector.hasScript(a111164CampaignPlugin.class)) {
      sector.addScript(new a111164CampaignPlugin());
    }
  }


}
