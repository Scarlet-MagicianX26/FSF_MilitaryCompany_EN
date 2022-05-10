package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import combat.util.aEP_DataTool;
import data.scripts.campaign.intel.aEP_AWM1Intel;
import data.scripts.campaign.intel.aEP_AWM2Intel;
import data.scripts.campaign.intel.aEP_AWM3Intel;
import data.scripts.campaign.intel.aEP_BaseMission;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static data.scripts.campaign.intel.aEP_AWM1Intel.WEAPON_WEIGHT;


public class aEP_AdvanceWeaponMission extends BaseCommandPlugin
{
  InteractionDialogAPI dialog;
  String ruleId;
  java.util.Map<java.lang.String, MemoryAPI> memoryMap;

  /**
   * @param params commodityId, check threshold, moreOrLess.
   */

  @Override
  public boolean execute(java.lang.String ruleId, InteractionDialogAPI dialog, java.util.List<Misc.Token> params, java.util.Map<java.lang.String, MemoryAPI> memoryMap) {
    this.dialog = dialog;
    this.ruleId = ruleId;
    this.memoryMap = memoryMap;

    String param = params.get(0).string;
    float num = 0f;


    if (Global.getSector() != null && Global.getSector().getPlayerFleet() != null && Global.getSector().getPlayerFleet().getCargo() != null) {
      switch (param) {
        case "shouldStart":
          return shouldStart();
        case "show1":
          return show1(6f, "rare_bp");
        case "start1": {
          PersonAPI person = dialog.getInteractionTarget().getActivePerson();
          person.getRelToPlayer().setRel(person.getRelToPlayer().getRel() + 0.05f);
          return start1();
        }
        case "check1":
          return check("aEP_AWM1Intel");
        case "complete1": {
          PersonAPI person1 = dialog.getInteractionTarget().getActivePerson();
          person1.getRelToPlayer().setRel(person1.getRelToPlayer().getRel() + 0.1f);
          end("aEP_AWM1Intel");
          return true;
        }


        case "shouldStart2":
          return shouldStart2();
        case "show2":
          return show2();
        case "start2": {
          PersonAPI person = dialog.getInteractionTarget().getActivePerson();
          person.getRelToPlayer().setRel(person.getRelToPlayer().getRel() + 0.1f);
          return start2();
        }
        case "check2":
          return check("aEP_AWM2Intel");
        case "complete2": {
          PersonAPI person3 = dialog.getInteractionTarget().getActivePerson();
          person3.getRelToPlayer().setRel(person3.getRelToPlayer().getRel() + 0.1f);
          end("aEP_AWM2Intel");
          return true;
        }


        case "shouldStart3":
          return shouldStart3();
        case "start3":
          return start3();
        case "check3":
          return check("aEP_AWM3Intel");
        case "complete3": {
          PersonAPI person4 = dialog.getInteractionTarget().getActivePerson();
          person4.getRelToPlayer().setRel(person4.getRelToPlayer().getRel() + 0.1f);
          end("aEP_AWM3Intel");
          return true;
        }
        case "shouldGive3":
          return shouldGive3();
        case "give3":
          return give3();

      }

    }
    return false;
  }

  boolean check(String className) {
    boolean check = false;
    for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel()) {
      if (intel instanceof aEP_BaseMission) {

        aEP_BaseMission mission = (aEP_BaseMission) intel;
        if (mission.getName().contains(className) && mission.shouldEnd == true) {
          check = true;
        }
      }
    }
    return check;
  }

  void end(String className) {

    for (IntelInfoPlugin intel : Global.getSector().getIntelManager().getIntel()) {
      if (intel instanceof aEP_BaseMission) {
        aEP_BaseMission mission = (aEP_BaseMission) intel;
        if (mission.getName().contains(className) && mission.shouldEnd == true) {
          mission.readyToEnd = true;
        }
      }
    }

  }


  boolean shouldStart() {

    FactionAPI faction = Global.getSector().getFaction("aEP_FSF");
    for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {

      if (market.getPrimaryEntity().getId().equals("aEP_FSF_DefStation") && Global.getSector().getPlayerFaction().getRelationship("aEP_FSF") > 0.20f) {
        boolean has = false;
        for (PersonAPI person : market.getPeopleCopy()) {
          if (person.getMemoryWithoutUpdate().contains("$isaEP_Researcher")) {
            dialog.getInteractionTarget().setActivePerson(person);
            has = true;
            break;
          }
        }

        return has;
      }
    }
    return false;
  }


  boolean show1(float totalWeaponPoint, String tag) {

    List<String> requestWeaponList = new ArrayList<>();
    if (memoryMap.get(MemKeys.FACTION).contains("$AWM_1showed")) {
      requestWeaponList = (List) memoryMap.get(MemKeys.FACTION).get("$AWM_1showed");
    }
    else {
      for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
        if (spec.getTags().contains(tag)) {
          requestWeaponList.add(spec.getWeaponId());
        }
      }

      List<String> newList = new ArrayList<>();
      int num = 0;
      while (num <= totalWeaponPoint) {
        int i = (int) MathUtils.getRandomNumberInRange(0f, (float) requestWeaponList.size());
        newList.add(requestWeaponList.get(i));
        num = num + WEAPON_WEIGHT.get(Global.getSettings().getWeaponSpec(requestWeaponList.get(i)).getSize()).intValue();
        requestWeaponList.remove(i);
      }
      requestWeaponList = newList;
      memoryMap.get(MemKeys.FACTION).set("$AWM_1showed", requestWeaponList);
    }

    Color h = Misc.getHighlightColor();
    Color g = Misc.getGrayColor();
    Color c = Global.getSector().getFaction("aEP_FSF").getBaseUIColor();

    dialog.getTextPanel().addPara(aEP_DataTool.txt("AWM01_desc01"));
    for (String weaponId : requestWeaponList) {
      String weaponName = Global.getSettings().getWeaponSpec(weaponId).getWeaponName();
      dialog.getTextPanel().addPara("    - {%s}", g, h, weaponName);
    }
    dialog.getTextPanel().addPara(aEP_DataTool.txt("AWM01_desc02"));


    return true;
  }

  boolean start1() {
    //start train event
    List<String> requestWeaponList = new ArrayList<>();
    if (memoryMap.get(MemKeys.FACTION).contains("$AWM_1showed")) {
      requestWeaponList = (List) memoryMap.get(MemKeys.FACTION).get("$AWM_1showed");
      Global.getSector().addScript(new aEP_AWM1Intel(requestWeaponList));
      return true;
    }
    Global.getSector().addScript(new aEP_AWM1Intel(6f, "rare_bp"));
    return true;
  }

  boolean shouldStart2() {
    FactionAPI faction = Global.getSector().getFaction("aEP_FSF");
    boolean has = false;
    for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
      if (market.getPrimaryEntity().getId().equals("aEP_FSF_DefStation") && Global.getSector().getPlayerFaction().getRelationship("aEP_FSF") > 0.20f) {
        for (PersonAPI person : market.getPeopleCopy()) {
          if (person.getMemoryWithoutUpdate().contains("$isaEP_Researcher")) {
            dialog.getInteractionTarget().setActivePerson(person);
            has = true;
            break;
          }
        }
      }
    }

    boolean levelIsEnough = Global.getSector().getPlayerPerson().getStats().getLevel() >= 6;

    boolean mission1Completed = false;
    if (Global.getSector().getFaction("aEP_FSF").getMemoryWithoutUpdate().get("$AWM_1Complete") != null) {
      if (Global.getSector().getFaction("aEP_FSF").getMemoryWithoutUpdate().get("$AWM_1Complete").equals("true")) {
        mission1Completed = true;
      }
    }


    return has && levelIsEnough && mission1Completed;
  }

  boolean show2() {
    dialog.getOptionPanel().setEnabled(memoryMap.get("local").getString("$option"), false);

    if (!dialog.getOptionPanel().hasOption("aEP_researcher_stage1_talk05")) {
      dialog.getOptionPanel().addOption(aEP_DataTool.txt("aEP_AdvanceWeaponMission01"), "aEP_researcher_stage1_talk05");
    }
    return true;
  }

  boolean start2() {
    FactionAPI faction = Global.getSector().getFaction("aEP_FSF");
    Vector2f toSpawn = new Vector2f(0f, 0f);
    WeightedRandomPicker<StarSystemAPI> systemPicker = new WeightedRandomPicker<StarSystemAPI>();
    for (StarSystemAPI system : Global.getSector().getStarSystems()) {
      float mult = 0f;

      if (system.hasPulsar()) continue;
      if (system.hasTag(Tags.THEME_MISC_SKIP)) {
        mult = 1f;
      }
      else if (system.hasTag(Tags.THEME_MISC)) {
        mult = 3f;
      }
      else if (system.hasTag(Tags.THEME_REMNANT_NO_FLEETS)) {
        mult = 3f;
      }
      else if (system.hasTag(Tags.THEME_RUINS)) {
        mult = 5f;
      }
      else if (system.hasTag(Tags.THEME_REMNANT_DESTROYED)) {
        mult = 3f;
      }
      else if (system.hasTag(Tags.THEME_CORE_UNPOPULATED)) {
        mult = 1f;
      }

      for (MarketAPI market : Misc.getMarketsInLocation(system)) {
        if (market.isHidden()) continue;
        mult = 0f;
        break;
      }

      float distToPlayer = Misc.getDistanceToPlayerLY(system.getLocation());
      float noSpawnRange = Global.getSettings().getFloat("personBountyNoSpawnRangeAroundPlayerLY");
      if (distToPlayer < noSpawnRange) mult = 0f;

      if (mult <= 0) continue;

      float weight = system.getPlanets().size();
      for (PlanetAPI planet : system.getPlanets()) {
        if (planet.isStar()) continue;
        if (planet.getMarket() != null) {
          float h = planet.getMarket().getHazardValue();
          if (h <= 0f) weight += 5f;
          else if (h <= 0.25f) weight += 3f;
          else if (h <= 0.5f) weight += 1f;
        }
      }

      float dist = system.getLocation().length();
      float distMult = Math.max(0, 50000f - dist);

      systemPicker.add(system, weight * mult * distMult);
    }

    StarSystemAPI system = systemPicker.pick();

    if (system != null) {
      toSpawn = new Vector2f(2000f, 2000f);
      SectorEntityToken token = system.createToken(toSpawn);
      for (int i = 1; i <= 12; i++) {
        AsteroidAPI asteroid = token.getContainingLocation().addAsteroid(MathUtils.getRandomNumberInRange(2f, 12f));
        asteroid.setCircularOrbit(token, MathUtils.getRandomNumberInRange(0, 360), MathUtils.getRandomNumberInRange(0, 500), 10000000f);
      }


      //add debris field
      DebrisFieldTerrainPlugin.DebrisFieldParams params = new DebrisFieldTerrainPlugin.DebrisFieldParams(
        250f, // field radius - should not go above 1000 for performance reasons
        1f, // density, visual - affects number of debris pieces
        10000000f, // duration in days
        0f); // days the field will keep generating glowing pieces

      params.source = DebrisFieldTerrainPlugin.DebrisFieldSource.GEN;
      SectorEntityToken debris = Misc.addDebrisField(token.getContainingLocation(), params, new Random());
      debris.setCircularOrbit(token, 0, 0, 10000000f);

      //Global.getSector().getIntelManager().addIntel(new aEP_AWM2Intel(token));
      //Global.getSector().getIntelManager().queueIntel(new aEP_AWM2Intel(token));
      Global.getSector().addScript(new aEP_AWM2Intel(token, "aEP_typeB28_variant", "TYPE_B_028"));

    }

    return false;
  }

  boolean shouldStart3() {
    FactionAPI faction = Global.getSector().getFaction("aEP_FSF");
    boolean has = false;
    for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
      if (market.getPrimaryEntity().getId().equals("aEP_FSF_DefStation") && Global.getSector().getPlayerFaction().getRelationship("aEP_FSF") > 0.50f) {
        for (PersonAPI person : market.getPeopleCopy()) {
          if (person.getMemoryWithoutUpdate().contains("$isaEP_Researcher")) {
            dialog.getInteractionTarget().setActivePerson(person);
            has = true;
            break;
          }
        }
      }
    }

    boolean levelIsEnough = Global.getSector().getPlayerPerson().getStats().getLevel() >= 8;

    boolean mission2Completed = false;
    if (Global.getSector().getFaction("aEP_FSF").getMemoryWithoutUpdate().get("$AWM_2Complete") != null) {
      if (Global.getSector().getFaction("aEP_FSF").getMemoryWithoutUpdate().get("$AWM_2Complete").equals("true")) {
        mission2Completed = true;
      }
    }


    return has && levelIsEnough && mission2Completed;
  }

  boolean start3() {
    FactionAPI faction = Global.getSector().getFaction("aEP_FSF");
    Vector2f toSpawn = new Vector2f(0f, 0f);
    WeightedRandomPicker<StarSystemAPI> systemPicker = new WeightedRandomPicker<StarSystemAPI>();
    for (StarSystemAPI system : Global.getSector().getStarSystems()) {
      float mult = 0f;

      if (system.hasPulsar()) continue;
      if (system.hasTag(Tags.THEME_MISC_SKIP)) {
        mult = 1f;
      }
      else if (system.hasTag(Tags.THEME_MISC)) {
        mult = 3f;
      }
      else if (system.hasTag(Tags.THEME_REMNANT_NO_FLEETS)) {
        mult = 3f;
      }
      else if (system.hasTag(Tags.THEME_RUINS)) {
        mult = 5f;
      }
      else if (system.hasTag(Tags.THEME_REMNANT_DESTROYED)) {
        mult = 3f;
      }
      else if (system.hasTag(Tags.THEME_CORE_UNPOPULATED)) {
        mult = 1f;
      }

      for (MarketAPI market : Misc.getMarketsInLocation(system)) {
        if (market.isHidden()) continue;
        mult = 0f;
        break;
      }

      float distToPlayer = Misc.getDistanceToPlayerLY(system.getLocation());
      float noSpawnRange = Global.getSettings().getFloat("personBountyNoSpawnRangeAroundPlayerLY");
      if (distToPlayer < noSpawnRange) mult = 0f;

      if (mult <= 0) continue;

      float weight = system.getPlanets().size();
      for (PlanetAPI planet : system.getPlanets()) {
        if (planet.isStar()) continue;
        if (planet.getMarket() != null) {
          float h = planet.getMarket().getHazardValue();
          if (h <= 0f) weight += 5f;
          else if (h <= 0.25f) weight += 3f;
          else if (h <= 0.5f) weight += 1f;
        }
      }

      float dist = system.getLocation().length();
      float distMult = Math.max(0, 50000f - dist);

      systemPicker.add(system, weight * mult * distMult);
    }

    StarSystemAPI system = systemPicker.pick();
    WeightedRandomPicker<PlanetAPI> planetPicker = new WeightedRandomPicker<PlanetAPI>();

    for (PlanetAPI planet : system.getPlanets()) {
      if (!planet.isStar()) {
        planetPicker.add(planet, 1f);
      }
    }

    while (planetPicker.isEmpty()) ;
    {
      system = systemPicker.pick();
      for (PlanetAPI planet : system.getPlanets()) {
        if (!planet.isStar()) {
          planetPicker.add(planet, 1f);
        }
      }
    }


    if (system != null) {
      SectorEntityToken token = planetPicker.pick();
      //Global.getSector().getIntelManager().addIntel(new aEP_AWM2Intel(token));
      //Global.getSector().getIntelManager().queueIntel(new aEP_AWM2Intel(token));
      Global.getSector().addScript(new aEP_AWM3Intel(token, "FSF_pirate"));

    }

    return false;
  }

  boolean shouldGive3() {
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListWithFightersCopy()) {
      if (member.getHullSpec().getHullId().equals("aEP_ShenCeng_mk2")) {
        return true;
      }
    }
    return false;
  }

  boolean give3() {
    FleetMemberAPI toReplace = null;
    CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
    for (FleetMemberAPI member : fleet.getFleetData().getMembersListWithFightersCopy()) {
      if (member.getHullSpec().getHullId().equals("aEP_ShenCeng_mk2")) {
        toReplace = member;
        break;
      }
    }

    if (toReplace != null) {
      if (toReplace.getCaptain() != null) {
        fleet.getFleetData().removeOfficer(toReplace.getCaptain());
      }
      fleet.getFleetData().removeFleetMember(toReplace);
      fleet.getFleetData().addFleetMember("aEP_ShenCeng_Standard");
      Global.getSector().getFaction("aEP_FSF").addKnownShip("aEP_ShenCeng", false);
      Global.getSector().getFaction("aEP_FSF").addPriorityShip("aEP_ShenCeng");
    }
    fleet.forceSync();


    dialog.getTextPanel().addPara(aEP_DataTool.txt("aEP_AdvanceWeaponMission02"), Color.white, Color.red, toReplace.getHullSpec().getNameWithDesignationWithDashClass());
    dialog.getTextPanel().addPara(aEP_DataTool.txt("aEP_AdvanceWeaponMission03"), Color.white, Color.green, Global.getSettings().getHullSpec("aEP_ShenCeng").getNameWithDesignationWithDashClass());
    return true;
  }

}

