package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

import static combat.util.aEP_DataTool.txt;

public class aEP_AWM2Intel extends aEP_BaseMission
{
  CampaignFleetAPI targetFleet;
  SectorEntityToken token;
  String shipName;
  private final boolean isImportant = true;

  public aEP_AWM2Intel(SectorEntityToken whereToSpawn, String variantId, String targetShipId) {
    this.sector = Global.getSector();
    this.faction = Global.getSector().getFaction("aEP_FSF");
    this.shipName = targetShipId;
    ending = false;
    ended = false;
    this.token = whereToSpawn;
    setName(this.getClass().getSimpleName());
    setPostingLocation(token);
    setWhereToReturn(Global.getSector().getEconomy().getMarket("aEP_FSF_DefStation").getPrimaryEntity());


    //add Fleet
    CampaignFleetAPI targetFleet = Global.getFactory().createEmptyFleet("derelict", "奇怪的无人舰", true);
    targetFleet.getFleetData().addFleetMember(variantId);
    targetFleet.getFleetData().setFlagship(targetFleet.getFleetData().getMembersListWithFightersCopy().get(0));
    targetFleet.getFlagship().setId(targetShipId);
    targetFleet.getFleetData().setOnlySyncMemberLists(false);
    targetFleet.getFleetData().sort();
    targetFleet.setAI(null);
    targetFleet.setNullAIActionText("...");
    //add captain
    PersonAPI person = Global.getFactory().createPerson();
    person.setFaction("derelict");

    CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(Commodities.GAMMA_CORE);
    person.setAICoreId(Commodities.GAMMA_CORE);
    person.setName(new FullName(spec.getName(), "", FullName.Gender.ANY));
    person.setPortraitSprite("graphics/portraits/portrait_ai3b.png");
    person.getStats().setLevel(8);
    person.getStats().setSkillLevel(Skills.CONTAINMENT_PROCEDURES,2);
    person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE,3);
    person.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS,3);
    person.getStats().setSkillLevel(Skills.POLARIZED_ARMOR,3);
    person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS,3);
    person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY,3);

    person.setRankId(Ranks.SPACE_CAPTAIN);
    person.setPersonality(Personalities.RECKLESS);

    person.setFleet(targetFleet);
    targetFleet.getFlagship().setCaptain(person);
    targetFleet.setCommander(person);
    targetFleet.forceSync();

    token.getContainingLocation().spawnFleet(token, 0f, 0f, targetFleet);
    targetFleet.getMemoryWithoutUpdate().set("$isTypeB28", true);
    targetFleet.getMemoryWithoutUpdate().set("$core_fightToTheLast", true);
    targetFleet.getMemoryWithoutUpdate().set("$cfai_holdVsStronger", true);
    targetFleet.getMemoryWithoutUpdate().set("$cfai_makeAllowDisengage", true);
    targetFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MISSION_IMPORTANT, true);
    targetFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY, true);
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
    for (FleetMemberAPI member : targetFleet.getFleetData().getMembersListWithFightersCopy()) {
      if (member.getId().equals(shipName)) {
        isGone = false;
      }
    }
    if (!targetFleet.isAlive()) isGone = true;

    if (isGone && shouldEnd == false) {
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
    info.addPara(txt("AWM02_title"), c, 3f);
    info.setBulletedListMode("    - ");
    info.addPara(txt("AWM02_mission01"), 10f, g, h, token.getContainingLocation().getName());
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
    info.addPara(txt("AWM02_mission01") + ": ", 10f, whiteColor, targetFleet.getFaction().getBaseUIColor(), targetFleet.getCommander().getNameString());
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
}
