package data.scripts.campaign.intel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import combat.util.aEP_DataTool;
import data.hullmods.aEP_CruiseMissileCarrier;
import data.scripts.campaign.items.aEP_CruiseMissile;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

import static combat.util.aEP_DataTool.txt;

public class aEP_AWM3Intel extends aEP_BaseMission
{
  CampaignFleetAPI targetFleet;
  SectorEntityToken token;
  String shipName;

  public aEP_AWM3Intel(SectorEntityToken whereToSpawn, String targetShipId) {
    this.sector = Global.getSector();
    this.faction = Global.getSector().getFaction("aEP_FSF");
    ending = false;
    ended = false;
    shipName = targetShipId;
    this.token = whereToSpawn;
    setName(this.getClass().getSimpleName());
    setPostingLocation(token);
    setWhereToReturn(Global.getSector().getEconomy().getMarket("aEP_FSF_DefStation").getPrimaryEntity());


    //add Fleet
    CampaignFleetAPI targetFleet = FleetFactoryV3.createFleet(new FleetParamsV3(
      new Vector2f(0f, 0f),
      "aEP_FSF",
      1f,// qualityMod
      FleetTypes.PERSON_BOUNTY_FLEET,
      180, // combatPts
      0f, // freighterPts
      0f, // tankerPts
      0f, // transportPts
      0f, // linerPts
      0f,  // utilityPts
      1f
    ));

    FleetMemberAPI flagship = targetFleet.getFleetData().addFleetMember("aEP_ShenCeng_mk2_Standard");
    flagship.getVariant().addPermaMod("reinforcedhull", true);
    flagship.setId(shipName);
    targetFleet.getFleetData().setFlagship(flagship);
    aEP_CruiseMissileCarrier.LoadingMissile loading = new aEP_CruiseMissileCarrier.LoadingMissile();
    loading.setFleetMember(flagship.getId());
    loading.setLoadedNum(1);
    Global.getSector().addScript(loading);


    targetFleet.getFlagship().setShipName("AWM Testing Obj CMC02");
    targetFleet.setFaction("pirates");
    targetFleet.setName(aEP_DataTool.txt("AWM03_mission03"));
    targetFleet.getFleetData().sort();
    //add captain
    PersonAPI person = Global.getFactory().createPerson();
    person.setPortraitSprite("graphics/portraits/portrait_pirate02.png");
    person.setGender(FullName.Gender.MALE);
    person.setFaction("pirates");
    person.setRankId(Ranks.TERRORIST);
    person.setPersonality(Personalities.AGGRESSIVE);
    person.getStats().setSkillLevel(Skills.CONTAINMENT_PROCEDURES,2);
    person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE,3);
    person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS,3);
    person.setFleet(targetFleet);
    targetFleet.getFlagship().setCaptain(person);
    targetFleet.setCommander(person);
    targetFleet.forceSync();

    targetFleet.getCargo().addSpecial(new SpecialItemData("aEP_CM",null),30f);
    SalvageEntityGenDataSpec.DropData drop = new SalvageEntityGenDataSpec.DropData();
    drop.addSpecialItem("aEP_CM", 5);
    targetFleet.addDropValue(drop);


    token.getContainingLocation().spawnFleet(token, 0f, 0f, targetFleet);
    targetFleet.getAI().addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, token, 999999f, null);
    targetFleet.addScript(new EntityWantToMissileAttackPlayer(targetFleet));
    targetFleet.getMemoryWithoutUpdate().set("$isFSFPirate", true);
    targetFleet.getMemoryWithoutUpdate().set("$core_fightToTheLast", true);
    targetFleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
    targetFleet.getMemoryWithoutUpdate().set("$cfai_makeAllowDisengage", false);
    targetFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MISSION_IMPORTANT, true);
    targetFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
    targetFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
    targetFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
    targetFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
    targetFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
    this.targetFleet = targetFleet;
    setTargetToken(targetFleet);

    setImportant(true);
    Global.getSector().getIntelManager().addIntel(this);
    Global.getSector().getIntelManager().queueIntel(this);
  }

  @Override
  public void advance(float amount) {
    addTime(amount);
    //Global.getSector().getPlayerFleet().addFloatingText("1",Color.BLUE,0.75f);


    boolean isGone = true;
    for (FleetMemberAPI member : targetFleet.getFleetData().getMembersListCopy()) {
      if (member.getId().equals(shipName)) {
        isGone = false;
      }
    }
    if (!targetFleet.isAlive()) isGone = true;

    //在目标舰队消失后，生成一次残骸
    if (isGone && shouldEnd == false) {

      //这艘特殊舰的默认打捞系数是0，手动生成一个废船打捞数据
      DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData("aEP_ShenCeng_mk2_Standard", ShipRecoverySpecial.ShipCondition.WRECKED, 0f), false);
      params.ship.shipName = shipName;
      params.ship.nameAlwaysKnown = true;
      params.durationDays = 999999f;
      params.ship.addDmods = true;
      //用上面的废船打捞数据生成一个大地图打捞entity
      SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(targetFleet.getContainingLocation(), Entities.WRECK, Factions.NEUTRAL, params);
      Vector2f toLocation = targetFleet.getLocation();
      ship.setLocation(toLocation.x, toLocation.y);
      ship.setDiscoverable(true);
      Misc.setSalvageSpecial(ship, params);
      //设定这个废船必须用故事点捞
      ShipRecoverySpecial.ShipRecoverySpecialData params2 = new ShipRecoverySpecial.ShipRecoverySpecialData("Prototype");
      params2.storyPointRecovery = true;
      params2.addShip(new ShipRecoverySpecial.PerShipData("aEP_ShenCeng_mk2_Standard", ShipRecoverySpecial.ShipCondition.AVERAGE));
      Misc.setSalvageSpecial(ship, params2);


      shouldEnd = true;
    }


  }

  @Override
  public void readyToEnd() {
    targetFleet.despawn();
  }

  @Override
  public String getIcon() {
    return Global.getSettings().getSpriteName("aEP_icons", "AWM1");
  }

  //this part control brief bar on lower left
  @Override
  public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
    Color h = Misc.getHighlightColor();
    Color g = Misc.getGrayColor();
    Color c = faction.getBaseUIColor();

    info.setParaFontDefault();
    info.addPara(txt("AWM03_title"), c, 3f);
    info.setBulletedListMode("    - ");
    info.addPara(txt("AWM03_mission01"), 10f, g, h, token.getContainingLocation().getName(), ((PlanetAPI) token).getTypeNameWithWorld());

  }


  //this control info part on right
  @Override
  public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
    Color hightLight = Misc.getHighlightColor();
    Color grayColor = Misc.getGrayColor();
    Color whiteColor = Misc.getTextColor();
    Color barColor = faction.getDarkUIColor();
    Color titleTextColor = faction.getColor();


    info.setParaFontDefault();
    info.addImages(250f, 90f, 3f, 10f, targetFleet.getCommander().getPortraitSprite(), targetFleet.getFaction().getCrest());
    info.addPara(txt("AWM03_mission02") + ": ", 10f, whiteColor, targetFleet.getFaction().getBaseUIColor(), targetFleet.getCommander().getNameString());
    info.addShipList(8, 1 + targetFleet.getMembersWithFightersCopy().size() / 8, 40f, barColor, targetFleet.getMembersWithFightersCopy(), 10f);
    info.addSectionHeading(txt("mission_require"), titleTextColor, barColor, Alignment.MID, 30f);
    info.addPara(txt("mission_destroy"), hightLight, 3f);
    info.setBulletedListMode("    - ");
    if (shouldEnd) {
      info.addPara(txt("mission_destroyed"), Color.green, 10f);
    }


    if (Global.getSettings().isDevMode()) {
      info.addPara("devMode force finish", Color.yellow, 10f);
      info.addButton("Finish Mission", "Finish Mission", 120, 20, 20f);
    }

  }

  @Override
  public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
    if (buttonId.equals("Finish Mission")) {
      shouldEnd = true;
    }
  }

  //control tags
  @Override
  public Set<String> getIntelTags(SectorMapAPI map) {
    Set<String> tags = new LinkedHashSet();
    tags.add("aEP_FSF");
    tags.add("Missions");
    return tags;
  }

  @Override
  public String getSortString() {
    return "FSF";
  }


  class EntityWantToMissileAttackPlayer implements EveryFrameScript
  {
    CampaignFleetAPI token;

    EntityWantToMissileAttackPlayer(CampaignFleetAPI fleet) {
      token = fleet;
    }

    /**
     * @return true when the script is finished and can be cleaned up by the engine.
     */
    @Override
    public boolean isDone() {
      for (FleetMemberAPI member : token.getMembersWithFightersCopy()) {
        if (member.getVariant().getHullSpec().getHullId().equals("aEP_ShenCeng_mk2")) {
          return false;
        }
      }
      return true;
    }

    /**
     * @return whether advance() should be called while the campaign engine is paused.
     */
    @Override
    public boolean runWhilePaused() {
      return false;
    }

    /**
     * @param amount in seconds. Use SectorAPI.getClock() to figure out how many campaign days that is.
     */
    @Override
    public void advance(float amount) {
      CampaignFleetAPI fleet = token;

      if (fleet.isVisibleToPlayerFleet() && fleet.getFaction().isHostileTo(Global.getSector().getPlayerFleet().getFaction()) && MathUtils.getDistance(fleet.getLocation(), Global.getSector().getPlayerFleet().getLocation()) < 1000) {
        lunchToPlayer(fleet);
      }

    }


    void lunchToPlayer(CampaignFleetAPI fleet) {
      for (FleetMemberAPI member : fleet.getMembersWithFightersCopy()) {
        for (EveryFrameScript EF : Global.getSector().getScripts()) {
          if (EF instanceof aEP_CruiseMissileCarrier.LoadingMissile) {
            if (((aEP_CruiseMissileCarrier.LoadingMissile) EF).getFleetMember().equals(member.getId())) {
              if (((aEP_CruiseMissileCarrier.LoadingMissile) EF).getLoadedNum() >= 1) {
                aEP_CruiseMissile.createMissile(fleet, VectorUtils.getAngle(fleet.getLocation(), Global.getSector().getPlayerFleet().getLocation()));
                ((aEP_CruiseMissileCarrier.LoadingMissile) EF).setEnd(true);
                return;
              }

            }
          }
        }
      }

    }

  }
}
