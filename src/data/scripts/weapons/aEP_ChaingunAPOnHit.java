package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import combat.plugin.aEP_BuffEffect;
import combat.impl.buff.aEP_UpKeepIncrease;
import org.lwjgl.util.vector.Vector2f;

public class aEP_ChaingunAPOnHit implements OnHitEffectPlugin
{


  private static final float MAX_STACK = 400f;
  private static final float DAMAGE_TO_UPKEEP_INCREASE = 4f;
  private static final float BUFF_LIFETIME = 10f;
  Vector2f vel = new Vector2f(0f, 0f);

  @Override
  public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
    if (target instanceof ShipAPI && shieldHit) {
      //engine.addFloatingText(engine.getPlayerShip().getMouseTarget(),size +"",20f,new Color(100,100,100,100),engine.getPlayerShip(),1f,5f);
      aEP_BuffEffect.addThisBuff(target, new aEP_UpKeepIncrease(BUFF_LIFETIME, (ShipAPI) target, false, MAX_STACK, projectile.getEmpAmount(), "aEP_ChaingunAPOnHit"));
    }
  }
}
