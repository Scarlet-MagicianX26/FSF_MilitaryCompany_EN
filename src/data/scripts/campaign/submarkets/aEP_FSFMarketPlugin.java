package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.lazywizard.lazylib.MathUtils;

import java.util.*;

public class aEP_FSFMarketPlugin extends BaseSubmarketPlugin
{

  private static final Map weaponWeight = new HashMap();
  private static final List<String> hullmods = new ArrayList();


  static {
    hullmods.add("aEP_SoftfluxDissipate");
    hullmods.add("aEP_RapidDissipate");
    hullmods.add("aEP_TargetSystem");
    hullmods.add("aEP_VanguardHull");
    hullmods.add("aEP_ControledShield");
    hullmods.add("aEP_HotLoader");
  }


  @Override
  public void init(SubmarketAPI submarket) {
    this.submarket = submarket;
    this.market = submarket.getMarket();
  }

  @Override
  public float getTariff() {
    return 1f;
  }

  @Override
  public void updateCargoPrePlayerInteraction() {
    sinceLastCargoUpdate = 0f;
    CargoAPI marketCargo = submarket.getCargo();
    //如果可以刷新就开始加东西
    if (okToUpdateShipsAndWeapons()) {
      sinceSWUpdate = 0f;
      pruneWeapons(0f);

      int weaponNum = MathUtils.clamp(market.getSize() * 3,3,30);
      int fighterNum = 1 + market.getSize();

      //add some weapon
      addWeapons(weaponNum,weaponNum+6,3,"aEP_FSF_only");

      //add hullmod if player haven't learn it
      for (String modName : hullmods) {
        if (!Global.getSector().getPlayerFleet().getFaction().getKnownHullMods().contains(modName)) {
          boolean hadInStack = false;
          for (CargoStackAPI cargoStack : marketCargo.getStacksCopy()) {

            if (cargoStack.getHullModSpecIfHullMod() != null) {
              if (cargoStack.getHullModSpecIfHullMod().getId().equals(modName)) {
                hadInStack = true;
              }
            }
          }
          if (hadInStack == false) {
            marketCargo.addHullmods(modName, 1);
          }

        }

      }
      addFighters(fighterNum, fighterNum, 3, submarket.getFaction().getId()); //min number, max number, max tier, faction id

      getCargo().getMothballedShips().clear();
      FactionDoctrineAPI doctrineOverrided = submarket.getFaction().getDoctrine().clone();

      doctrineOverrided.setCombatFreighterProbability(0.25f);
      doctrineOverrided.setShipSize(5);
      doctrineOverrided.setShipQuality(5);


      doctrineOverrided.setCarriers(3);
      doctrineOverrided.setWarships(3);
      doctrineOverrided.setPhaseShips(3);
      doctrineOverrided.setFleets(4);

      //这3个东西设为0以后，上升流mk1的爆率会和mk3一样低
      doctrineOverrided.setCombatFreighterProbability(0f);
      doctrineOverrided.setCombatFreighterCombatUseFraction(0f);
      doctrineOverrided.setCombatFreighterCombatUseFractionWhenPriority(0f);


      //addShip("aEP_ShangShengLiu_mk3_Standard",false,5);
      addShips(submarket.getFaction().getId(),//faction id
        100f, // combat
        40f, // freighter
        40f, // tanker
        20f, // transport
        20f, // liner
        20f, // utilityPts
        1.5f, // qualityOverride
        0f, // qualityMod
        ShipPickMode.PRIORITY_ONLY,//FactionAPI.ShipPickMode modeOverride, at what priority to pick ship in all availables
        doctrineOverrided, 20);// FactionDoctrineAPI doctrineOverride, at what fraction to pick ship among all availables
    }

    getCargo().sort();
  }

  @Override
  public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
    FactionAPI player = Global.getSector().getPlayerFaction();
    RepLevel fsfLevel = Global.getSector().getFaction("aEP_FSF").getRelationshipLevel(player);
    RepLevel hegeLevel = Global.getSector().getFaction("hegemony").getRelationshipLevel(player);
    RepLevel indeLevel = Global.getSector().getFaction("independent").getRelationshipLevel(player);

    if (action == TransferAction.PLAYER_SELL) return true;
    if (action == TransferAction.PLAYER_BUY && !fsfLevel.isAtWorst(RepLevel.NEUTRAL)) return true;
    //if(action == TransferAction.PLAYER_BUY && !hegeLevel.isAtWorst(RepLevel.SUSPICIOUS)) return true;
    return action == TransferAction.PLAYER_BUY && !indeLevel.isAtWorst(RepLevel.NEUTRAL);
  }

  @Override
  public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {

    FactionAPI player = Global.getSector().getPlayerFaction();
    RepLevel fsfLevel = Global.getSector().getFaction("aEP_FSF").getRelationshipLevel(player);
    RepLevel hegeLevel = Global.getSector().getFaction("hegemony").getRelationshipLevel(player);
    RepLevel indeLevel = Global.getSector().getFaction("independent").getRelationshipLevel(player);

    if (action == TransferAction.PLAYER_SELL) return true;
    if (action == TransferAction.PLAYER_BUY && !fsfLevel.isAtWorst(RepLevel.NEUTRAL)) return true;
    //if(action == TransferAction.PLAYER_BUY && !hegeLevel.isAtWorst(RepLevel.SUSPICIOUS)) return true;
    return action == TransferAction.PLAYER_BUY && !indeLevel.isAtWorst(RepLevel.NEUTRAL);
  }


  @Override
  public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {

    FactionAPI player = Global.getSector().getPlayerFaction();
    RepLevel fsfLevel = Global.getSector().getFaction("aEP_FSF").getRelationshipLevel(player);
    RepLevel hegeLevel = Global.getSector().getFaction("hegemony").getRelationshipLevel(player);
    RepLevel indeLevel = Global.getSector().getFaction("independent").getRelationshipLevel(player);

    if (action == TransferAction.PLAYER_SELL) return "这里是公司的卖场，朋友";
    if (!fsfLevel.isAtWorst(RepLevel.NEUTRAL)) return "我们这里对你有一些不好的记录 (require FSF-中立)";
    //if(action == TransferAction.PLAYER_BUY && !hegeLevel.isAtWorst(RepLevel.SUSPICIOUS)) return "We don't want trouble with Hegemony (require NEUTRAL)";
    if (action == TransferAction.PLAYER_BUY && !indeLevel.isAtWorst(RepLevel.NEUTRAL))
      return "自由联盟对于你有一些不好的记录 (require 自由联盟-中立)";
    return "这里是公司的卖场，朋友";

  }

  @Override
  public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
    FactionAPI player = Global.getSector().getPlayerFaction();
    RepLevel fsfLevel = Global.getSector().getFaction("aEP_FSF").getRelationshipLevel(player);
    RepLevel hegeLevel = Global.getSector().getFaction("hegemony").getRelationshipLevel(player);
    RepLevel indeLevel = Global.getSector().getFaction("independent").getRelationshipLevel(player);

    if (action == TransferAction.PLAYER_SELL) return "这里是公司的卖场，朋友";
    if (!fsfLevel.isAtWorst(RepLevel.NEUTRAL)) return "我们这里对你有一些不好的记录 (require FSF-中立)";
    //if(action == TransferAction.PLAYER_BUY && !hegeLevel.isAtWorst(RepLevel.SUSPICIOUS)) return "We don't want trouble with Hegemony (require NEUTRAL)";
    if (action == TransferAction.PLAYER_BUY && !indeLevel.isAtWorst(RepLevel.NEUTRAL))
      return "自由联盟对于你有一些不好的记录 (require 自由联盟-中立)";
    return "这里是公司的卖场，朋友";

  }

  @Override
  public boolean isHidden() {
    if(submarket.getFaction().getId().equals("aEP_FSF")){
      return false;
    }
    return true;
  }
}
