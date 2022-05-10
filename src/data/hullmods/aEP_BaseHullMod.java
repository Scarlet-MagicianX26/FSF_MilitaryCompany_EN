package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.util.MagicIncompatibleHullmods;
import combat.util.aEP_DataTool;

import java.util.*;

public class aEP_BaseHullMod extends BaseHullMod
{
  public Set<String> notCompatibleList = new HashSet<>();
  public Set<String> haveToBeWithMod = new HashSet<>();

  @Override
  public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
    //遍历所有已经安装船插，若存在任何一个排斥，返回false
    Iterator iterator = ship.getVariant().getHullMods().iterator();
    boolean shouldRemove = false;
    String conflictId = "";
    while (iterator.hasNext()){
      String it = (String) iterator.next();
      if(notCompatibleList.contains(it)) {
        shouldRemove = true;
        conflictId = it;
      }
    }
    if(shouldRemove){
      //如果本 mod是 built-in的话，移除发生冲突的另一个，反之移除本 mod
      if(!ship.getVariant().getNonBuiltInHullmods().contains(id)){
        MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(),conflictId,id);
      }else {
        MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(),id,conflictId);
      }
    }
    
    //遍历必须安装的船插表，若任何一个未安装，返回false，移除自己
    //如果不满足其他安装条件，也移除自己
    shouldRemove = false;
    iterator = haveToBeWithMod.iterator();
    while (iterator.hasNext()){
      String it = (String) iterator.next();
      if(!ship.getVariant().hasHullMod(it)) {
        shouldRemove = true;
      }
    }
    //自定义的安装条件
    if(!isApplicableToShip(ship)){
      shouldRemove = true;
    }
    if(shouldRemove){
      ship.getVariant().removeMod(id);
    }

    applyEffectsAfterShipCreationImpl(ship,id);
  }

  @Override
  public boolean isApplicableToShip(ShipAPI ship) {
    //遍历必须安装的船插表，若任何一个未安装，返回false
    Iterator iterator = haveToBeWithMod.iterator();
    while (iterator.hasNext()){
      if(!ship.getVariant().hasHullMod((String) iterator.next())) {
        return false;
      }
    }
    //遍历所有已经安装船插，若存在任何一个排斥，返回false
    iterator = ship.getVariant().getHullMods().iterator();
    while (iterator.hasNext()){
      if(notCompatibleList.contains(iterator.next())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getUnapplicableReason(ShipAPI ship) {
    //缺失任何一个必须，返回false
    Iterator iterator = haveToBeWithMod.iterator();
    while (iterator.hasNext()){
      if(!ship.getVariant().hasHullMod((String) iterator.next())) {
        return aEP_DataTool.txt("HaveToBeWith") +": " + showModName(haveToBeWithMod);
      }
    }
    //拥有任何一个排斥，返回false
    iterator = ship.getVariant().getHullMods().iterator();
    while (iterator.hasNext()){
      if(notCompatibleList.contains(iterator.next())) {
        return aEP_DataTool.txt("not_compatible") +": " + showModName(notCompatibleList);
      }
    }
    return aEP_DataTool.txt("not_compatible");
  }

  /**
   * 使用这个
   */
  public void applyEffectsAfterShipCreationImpl(ShipAPI ship, String id){
  }

  private String showModName(Set<String> list) {
    StringBuffer toReturn = new StringBuffer();
    for (String id : list.toArray(new String[list.size()]) ) {
      toReturn.append(Global.getSettings().getHullModSpec(id).getDisplayName() + " ");
    }
    return toReturn.toString();
  }
}
