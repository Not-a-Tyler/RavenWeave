package keystrokesmod.client.module.modules.combat;

import net.weavemc.loader.api.event.RenderGameOverlayEvent;
import net.weavemc.loader.api.event.SubscribeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.CoolDown;
import keystrokesmod.client.utils.Utils;
import me.PianoPenguin471.events.AttackEntityEvent;
import me.PianoPenguin471.events.LivingUpdateEvent;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class ShiftTap extends Module {
    public SliderSetting range, chance, tapMultiplier;
    public TickSetting onlyPlayers;
    public TickSetting onlySword;
    public TickSetting dynamic;
    public DoubleSliderSetting waitMs;
    public DoubleSliderSetting actionMs;
    public DoubleSliderSetting hitPer;
    public int hits, rhit;
    public boolean call;
    private WtapState state = WtapState.NONE;
    private final CoolDown timer = new CoolDown(0);
    private Entity target;

    public Random r = new Random();

    public ShiftTap() {
        super("ShiftTap", ModuleCategory.combat);

        this.registerSetting(onlyPlayers = new TickSetting("Only combo players", true));
        this.registerSetting(onlySword = new TickSetting("Only sword", false));
        this.registerSetting(waitMs = new DoubleSliderSetting("Release w for ... ms", 30, 40, 1, 300, 1));
        this.registerSetting(actionMs = new DoubleSliderSetting("WTap after ... ms", 20, 30, 1, 300, 1));
        this.registerSetting(hitPer = new DoubleSliderSetting("Once every ... hits", 1, 1, 1, 10, 1));
        this.registerSetting(chance = new SliderSetting("Chance %", 100, 0, 100, 1));
        this.registerSetting(range = new SliderSetting("Range: ", 3, 1, 6, 0.05));
        this.registerSetting(dynamic = new TickSetting("Dynamic wait time", false));
        this.registerSetting(tapMultiplier = new SliderSetting("wait time sensitivity", 1F, 0F, 5F, 0.1F));
    }

    @SubscribeEvent
    public void onRender2D(RenderGameOverlayEvent e) {
        if (!this.enabled) return;
        if (state == WtapState.NONE)
            return;
        if (state == WtapState.WAITINGTOTAP && timer.hasFinished()) {
            startCombo();
        } else if (state == WtapState.TAPPING && timer.hasFinished()) {
            finishCombo();
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent e) {
        if (!this.enabled) return;
        target = e.target;

        if (isSecondCall())
            wTap();
    }

    @SubscribeEvent
    public void onForgeEvent(LivingUpdateEvent e) {
        if (!this.enabled) return;
        if (e.getEntityLiving().hurtTime > 0
                && e.getEntityLiving().hurtTime == e.getEntityLiving().maxHurtTime && e.entity == this.target)
            wTap();
    }


    public void wTap() {
        if (state != WtapState.NONE)
            return;
        if (!(Math.random() <= chance.getInput() / 100)) {
            hits++;
        }
        if (mc.thePlayer.getDistanceToEntity(target) > range.getInput()
                || (onlyPlayers.isToggled() && !(target instanceof EntityPlayer))
                || (onlySword.isToggled() && !Utils.Player.isPlayerHoldingSword()) || !(rhit >= hits))
            return;
        trystartCombo();
    }

    public void finishCombo() {
        state = WtapState.NONE;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        hits = 0;
        int easports = (int) (hitPer.getInputMax() - hitPer.getInputMin() + 1);
        rhit = ThreadLocalRandom.current().nextInt((easports));
        rhit += (int) hitPer.getInputMin();
    }

    public void startCombo() {
        state = WtapState.TAPPING;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        double cd = ThreadLocalRandom.current().nextDouble(waitMs.getInputMin(), waitMs.getInputMax() + 0.01);
        if (dynamic.isToggled()) {
            cd = 3 - mc.thePlayer.getDistanceToEntity(target) < 3
                    ? (cd + (3 - mc.thePlayer.getDistanceToEntity(target) * tapMultiplier.getInput() * 10))
                    : cd;
        }

        timer.setCooldown((long) cd);
        timer.start();
    }

    public void trystartCombo() {
        state = WtapState.WAITINGTOTAP;
        timer.setCooldown(
                (long) ThreadLocalRandom.current().nextDouble(actionMs.getInputMin(), actionMs.getInputMax() + 0.01));
        timer.start();
    }

    private boolean isSecondCall() {
        if (call) {
            call = false;
            return true;
        } else {
            call = true;
            return false;
        }
    }

    public enum WtapState {
        NONE, WAITINGTOTAP, TAPPING,
    }
}
