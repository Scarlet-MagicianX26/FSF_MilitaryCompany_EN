package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import combat.impl.VEs.aEP_MovingSprite;
import combat.plugin.aEP_CombatEffectPlugin;
import combat.util.aEP_Tool;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class aEP_ChaingunOnFire implements OnFireEffectPlugin
{

  private static final List<String> flames = new ArrayList<>();

  static {
    flames.add("weapons.LBCG_flame");
    flames.add("weapons.LBCG_flame_core");
    flames.add("weapons.LBCG_flame2");
    flames.add("weapons.LBCG_flame2_core");
  }

  int spriteNum = 0;


  @Override
  public void onFire(DamagingProjectileAPI proj, WeaponAPI weapon, CombatEngineAPI engine) {

    //create shell
    float side = 1f;
    if (MathUtils.getShortestRotation(weapon.getCurrAngle(), VectorUtils.getAngle(weapon.getLocation(), proj.getSpawnLocation())) > 0)
      side = -1f;

    //create flame
    spriteNum = spriteNum + 2;
    if (spriteNum >= flames.size()) {
      spriteNum = 0;
    }
    Vector2f offset = proj.getSpawnLocation();
    aEP_CombatEffectPlugin.Mod.addVE(new aEP_MovingSprite(offset,//position
      new Vector2f(0f, 0f),
      0f,
      weapon.getCurrAngle() - 90f,
      0f,
      0.075f,
      0.025f,
      new Vector2f(-56f, -48f),
      new Vector2f(28f, 24f),
      flames.get(spriteNum),
      new Color(200, 200, 50, 120)));

    int color = (int) (180 * Math.random()) + 40;
    aEP_CombatEffectPlugin.Mod.addVE(new aEP_MovingSprite(offset,//position
      new Vector2f(0f, 0f),
      0f,
      weapon.getCurrAngle() - 90f,
      0f,
      0.075f,
      0.025f,
      new Vector2f(-56f, -48f),
      new Vector2f(28f, 24f),
      flames.get(spriteNum + 1),
      new Color(220, 220, color, 220)));
    engine.addSmoothParticle(offset,
      new Vector2f(0f, 0f),
      40f + MathUtils.getRandomNumberInRange(0, 40),
      1f,
      0.1f,
      new Color(200, 200, 50, 60 + MathUtils.getRandomNumberInRange(0, 60)));

    float distFromOffset = 0;
    while (distFromOffset < 15f) {
      engine.addSmoothParticle(aEP_Tool.getExtendedLocationFromPoint(offset, weapon.getCurrAngle(), distFromOffset),
        new Vector2f(0f, 0f),
        0.6f * (21 - distFromOffset),//size
        1f,//brightness
        0.075f,//duration
        new Color(200, 200, 50, 180));
      distFromOffset = distFromOffset + 2f;
    }


    //VEs.createCustomVisualEffect(new SmokeTrail(proj));

  }


}
