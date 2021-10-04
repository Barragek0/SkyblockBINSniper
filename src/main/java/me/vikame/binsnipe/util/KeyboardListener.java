package me.vikame.binsnipe.util;

import me.vikame.binsnipe.Config;
import me.vikame.binsnipe.Main;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import static org.jnativehook.keyboard.NativeKeyEvent.VC_CONTROL;
import static org.jnativehook.keyboard.NativeKeyEvent.VC_V;

public class KeyboardListener implements NativeKeyListener {
	
	private static boolean controlPressed = false;
	public static boolean canMoveOn = false;
	
	@Override
	public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
	}
	
	@Override
	public void nativeKeyPressed(NativeKeyEvent e) {
    if (Config.KEY_LISTENER_DEBUG)
      System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
		switch (e.getKeyCode()) {
			case VC_CONTROL:
				Main.printDebug("controlPressed = true");
				controlPressed = true;
				break;
			case VC_V:
				if (controlPressed) {
					Main.printDebug("canMoveOn = true");
					Main.printDebug("controlPressed = false");
					canMoveOn = true;
					controlPressed = false;
				}
				break;
		}
	}
	
	@Override
	public void nativeKeyReleased(NativeKeyEvent e) {
		if (Config.KEY_LISTENER_DEBUG)
			System.out.println("Key Released: " + NativeKeyEvent.getKeyText(e.getKeyCode()));
		if (e.getKeyCode() == VC_CONTROL) {
			Main.printDebug("controlPressed = false");
			controlPressed = false;
		}
	}
}
