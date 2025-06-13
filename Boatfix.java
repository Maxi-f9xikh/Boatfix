package de.maxi.boatfix;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.DistExecutor;

@Mod(Boatfix.MOD_ID)
public class Boatfix {
    public static final String MOD_ID = "boatfix";

    private boolean lastUseKeyState = false;
    private boolean isEatingInBoat = false;

    public Boatfix() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(this);
        });
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return; // Wichtig: START statt END!
        if (!Config.ENABLE_BOAT_FIX.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) return;

        LocalPlayer player = mc.player;

        // Reset wenn nicht im Boot
        if (!(player.getVehicle() instanceof Boat)) {
            isEatingInBoat = false;
            lastUseKeyState = false;
            return;
        }

        // Aktueller Use-Key Status
        boolean useKeyPressed = mc.options.keyUse.isDown() || mc.mouseHandler.isRightPressed();

        // Prüfen ob Use-Key gerade gedrückt wurde (rising edge)
        boolean useKeyJustPressed = useKeyPressed && !lastUseKeyState;

        // Update für nächsten Tick
        lastUseKeyState = useKeyPressed;

        // Wenn Use-Key losgelassen, stoppe Essen
        if (!useKeyPressed && isEatingInBoat) {
            isEatingInBoat = false;
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            return;
        }

        // Wenn Use-Key gerade gedrückt wurde und wir essbare Items haben
        if (useKeyJustPressed && canEat(player)) {
            startEatingWithKeyboardTrick(mc, player);
        }

        // Sicherstellen dass Essen nicht unterbrochen wird
        if (isEatingInBoat && useKeyPressed && !player.isUsingItem() && canEat(player)) {
            // Essen wurde unterbrochen, starte es erneut
            continueEatingWithKeyboardTrick(mc, player);
        }
    }

    private boolean canEat(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.isEdible() || offHand.isEdible();
    }

    private void startEatingWithKeyboardTrick(Minecraft mc, LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        InteractionHand hand;
        if (mainHand.isEdible()) {
            hand = InteractionHand.MAIN_HAND;
        } else if (offHand.isEdible()) {
            hand = InteractionHand.OFF_HAND;
        } else {
            return;
        }

        // Der Trick: Temporär die Bewegungstasten "freigeben"
        KeyMapping keyUp = mc.options.keyUp;
        KeyMapping keyDown = mc.options.keyDown;
        KeyMapping keyLeft = mc.options.keyLeft;
        KeyMapping keyRight = mc.options.keyRight;

        // Speichere originale Zustände
        boolean originalUp = keyUp.isDown();
        boolean originalDown = keyDown.isDown();
        boolean originalLeft = keyLeft.isDown();
        boolean originalRight = keyRight.isDown();

        try {
            // Setze alle Bewegungstasten temporär auf "nicht gedrückt"
            setKeyState(keyUp, false);
            setKeyState(keyDown, false);
            setKeyState(keyLeft, false);
            setKeyState(keyRight, false);

            // Jetzt Essen starten
            player.startUsingItem(hand);
            isEatingInBoat = true;

        } finally {
            // Stelle originale Zustände wieder her
            setKeyState(keyUp, originalUp);
            setKeyState(keyDown, originalDown);
            setKeyState(keyLeft, originalLeft);
            setKeyState(keyRight, originalRight);
        }
    }

    private void continueEatingWithKeyboardTrick(Minecraft mc, LocalPlayer player) {
        // Gleiche Logik wie startEating
        startEatingWithKeyboardTrick(mc, player);
    }

    private void setKeyState(KeyMapping key, boolean pressed) {
        try {
            // Zugriff auf das private 'isDown' Feld
            java.lang.reflect.Field isDownField = KeyMapping.class.getDeclaredField("isDown");
            isDownField.setAccessible(true);
            isDownField.setBoolean(key, pressed);
        } catch (Exception e) {
            // Fallback: Versuche über clickCount
            try {
                java.lang.reflect.Field clickCountField = KeyMapping.class.getDeclaredField("clickCount");
                clickCountField.setAccessible(true);
                clickCountField.setInt(key, pressed ? 1 : 0);
            } catch (Exception e2) {
                // Silent fail - ohne Reflection funktioniert der Trick nicht
            }
        }
    }
}
