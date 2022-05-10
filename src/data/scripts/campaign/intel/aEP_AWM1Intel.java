package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.List;
import java.util.*;

import static combat.util.aEP_DataTool.txt;

public class aEP_AWM1Intel extends aEP_BaseMission
{


  public static final Map<WeaponAPI.WeaponSize, Float> WEAPON_WEIGHT = new HashMap<WeaponAPI.WeaponSize, Float>();

  static {
    WEAPON_WEIGHT.put(WeaponAPI.WeaponSize.SMALL, 1f);
    WEAPON_WEIGHT.put(WeaponAPI.WeaponSize.MEDIUM, 2f);
    WEAPON_WEIGHT.put(WeaponAPI.WeaponSize.LARGE, 3f);
  }

  List<String> requestWeaponList = new ArrayList<>();
  List<String> haveWeaponList = new ArrayList<>();

  public aEP_AWM1Intel(float totalWeaponPoint, String tag) {
    this.sector = Global.getSector();
    this.faction = Global.getSector().getFaction("aEP_FSF");
    ending = false;
    ended = false;
    endingTimeRemaining = 0f;
    setName("aEP_AWM1Intel");
    setWhereToReturn(Global.getSector().getEconomy().getMarket("aEP_FSF_DefStation").getPrimaryEntity());
    for (WeaponSpecAPI spec : Global.getSettings().getAllWeaponSpecs()) {
      if (spec.getTags().contains(tag)) {
        requestWeaponList.add(spec.getWeaponId());
      }
    }

    List<String> newList = new ArrayList<>();
    int num = 0;
    while (num <= totalWeaponPoint) {
      int i = (int) MathUtils.getRandomNumberInRange(0f, (float) requestWeaponList.size() - 1);
      newList.add(requestWeaponList.get(i));
      num = num + WEAPON_WEIGHT.get(Global.getSettings().getWeaponSpec(requestWeaponList.get(i)).getSize()).intValue();
      requestWeaponList.remove(i);
    }
    requestWeaponList = newList;


    setImportant(true);
    Global.getSector().getIntelManager().addIntel(this);
    Global.getSector().getIntelManager().queueIntel(this);
  }

  public aEP_AWM1Intel(List<String> requestWeaponList) {
    this.sector = Global.getSector();
    this.faction = Global.getSector().getFaction("aEP_FSF");
    ending = false;
    ended = false;
    endingTimeRemaining = 0f;
    setName("aEP_AWM1Intel");
    setWhereToReturn(Global.getSector().getEconomy().getMarket("aEP_FSF_DefStation").getPrimaryEntity());
    this.requestWeaponList = requestWeaponList;
    this.setImportant(true);

    Global.getSector().getIntelManager().addIntel(this);
    Global.getSector().getIntelManager().queueIntel(this);

  }

  @Override
  public void advance(float amount) {
    addTime(amount);

    haveWeaponList.clear();
    CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
    for (String id : requestWeaponList) {
      if (cargo.getNumWeapons(id) >= 1) {
        haveWeaponList.add(id);
      }
    }


    if (haveWeaponList.size() >= requestWeaponList.size() && shouldEnd == false) {
      shouldEnd = true;
    }


  }

  @Override
  public void readyToEnd() {
    CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
    for (String weaponName : haveWeaponList) {
      cargo.removeItems(CargoAPI.CargoItemType.WEAPONS, weaponName, 1);
    }
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
    info.addPara("AWM01:搜集武器", c, 3f);
    info.setBulletedListMode("    - ");
    //this is title
    for (String weaponId : requestWeaponList) {
      String weaponName = Global.getSettings().getWeaponSpec(weaponId).getWeaponName();
      info.addPara("找到{%s}", 3f, g, h, weaponName);
    }

  }


  //this control info part on right
  @Override
  public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
    Color h = Misc.getHighlightColor();
    Color g = Misc.getGrayColor();
    Color c = faction.getBaseUIColor();


    info.setParaFontDefault();
    info.addPara(txt("AWM01_title"), c, 3f);
    info.setBulletedListMode("    - ");
    for (String weaponId : requestWeaponList) {
      String weaponName = Global.getSettings().getWeaponSpec(weaponId).getWeaponName();
      if (!haveWeaponList.contains(weaponId)) {
        info.addPara(txt("AWM01_mission01"), 10f, g, h, weaponName);
      }
      else {
        info.addPara(txt("AWM01_mission02"), 10f, g, Color.green, weaponName);
      }

    }

    if (shouldEnd) {
      info.addPara(txt("AWM01_mission03"), 10f);
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
