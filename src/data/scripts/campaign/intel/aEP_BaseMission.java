package data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;

import java.awt.*;

public class aEP_BaseMission extends BaseIntelPlugin
{
  public boolean shouldEnd = false;
  public boolean readyToEnd = false;
  public float lifeTime = 0f;
  public float time = 0f;
  public SectorAPI sector;
  FactionAPI faction;
  private String name;
  private SectorEntityToken whereToReturn;
  private SectorEntityToken targetToken;


  public void addTime(float amount) {
    float days = Global.getSector().getClock().convertToDays(amount);
    if (endingTimeRemaining == null) endingTimeRemaining = 0f;
    if (isEnded()) {
      return;
    }

    if (isEnding()) {
      endingTimeRemaining = endingTimeRemaining - days;
      if (endingTimeRemaining <= 0) {
        ended = true;
        notifyEnded();
      }
      return;
    }

    //if time > life, start ending, by default ending time is 0 so is immediately end
    if (lifeTime > 0f) {
      time = time + days;
      if (time > lifeTime) {
        readyToEnd = true;
      }
    }

    //if ready to end, run readyToEnd code and start ending
    if (readyToEnd) {
      readyToEnd();
      ending = true;
    }
  }

  public void readyToEnd() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setWhereToReturn(SectorEntityToken where) {
    this.whereToReturn = where;
  }

  public void setTargetToken(SectorEntityToken where) {
    this.targetToken = where;
  }


  @Override
  public String getSmallDescriptionTitle() {
    return "Mission Target";
  }


  @Override
  public Color getTitleColor(ListInfoMode mode) {
    return faction.getBaseUIColor();
  }

  @Override
  public FactionAPI getFactionForUIColors() {
    return faction;
  }

  @Override
  public SectorEntityToken getPostingLocation() {
    if (shouldEnd) {
      return whereToReturn;
    }
    return postingLocation;
  }

  @Override
  public SectorEntityToken getMapLocation(SectorMapAPI map) {
    if (shouldEnd) {
      return whereToReturn;
    }
    return targetToken;
  }

}
