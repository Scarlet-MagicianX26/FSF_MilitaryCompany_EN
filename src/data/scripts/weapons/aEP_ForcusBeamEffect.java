package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;

public class aEP_ForcusBeamEffect implements BeamEffectPlugin
{
  static final float DAMAGE_MULT_IF_INTO_HULL = 0.5f;
  static final float REDUCE_THRESHOLD = 2f;
  aEP_FocusBeamDamageListner listener = null;

  @Override
  public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
    if (beam.getSource() == null) return;
    ShipAPI ship = beam.getSource();
    if (listener == null && beam.getDamageTarget() != null && beam.getDamageTarget() instanceof ShipAPI) {
      listener = new aEP_FocusBeamDamageListner(beam);
      ((ShipAPI) beam.getDamageTarget()).addListener(listener);
    }
    if (listener == null) return;
    listener.getLastHitResult();

  }

  class aEP_FocusBeamDamageListner implements DamageListener, AdvanceableListener, DamageTakenModifier
  {
    ShipAPI ship;
    BeamAPI source;
    float time = 1f;
    ApplyDamageResultAPI result;

    public aEP_FocusBeamDamageListner(BeamAPI source) {
      this.source = source;
      this.ship = source.getSource();
    }

    @Override
    public void advance(float amount) {
      time -= amount;
      if (time < 0) ship.removeListener(this);
    }

    //source 是伤害源，比如ShipAPI或者AsteroidAPI
    //每遭受一次伤害，先 modifyDamageTaken才会 report
    //发生在伤害已经施加在船体之后
    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
      // 只有对结构超过x点的伤害会存入result
      if (source == this.ship) {
        if (result.getDamageToHull() > REDUCE_THRESHOLD) {
          this.result = result;
        }
      }
    }

    //先于实际施加伤害
    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
      if (shieldHit) return null;
      if (!(param instanceof BeamAPI)) {
        result = null;
        return null;
      }
      if (result != null) {
        //aEP_Tool.addDebugText(result.getDamageToHull()+"");
        //上一步已经过滤，这里处理的每一个伤害都已经打进结构
        //过滤掉不属于BeamAPI的伤害
        if (param == source) {
          damage.getModifier().modifyMult("aEP_FocusBeamDamageListner", DAMAGE_MULT_IF_INTO_HULL);
        }
      }
      return null;
    }

    public ApplyDamageResultAPI getLastHitResult() {
      time += 0.1f;
      return result;
    }
  }
}

