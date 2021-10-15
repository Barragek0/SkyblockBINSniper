package me.vikame.binsnipe.util;

import static org.jnativehook.NativeInputEvent.CTRL_MASK;
import static org.jnativehook.keyboard.NativeKeyEvent.VC_V;

import me.vikame.binsnipe.Config;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class KeyboardListener implements NativeKeyListener {

  public static Runnable pasteCallback;

  @Override
  public void nativeKeyTyped(NativeKeyEvent e) {}

  @Override
  public void nativeKeyPressed(NativeKeyEvent e) {
    if(Config.KEY_LISTENER_DEBUG) {
      System.out.println(
          "Key Pressed: "
              + NativeKeyEvent.getModifiersText(e.getModifiers())
              + "+"
              + NativeKeyEvent.getKeyText(e.getKeyCode()));
    }
  }

  @Override
  public void nativeKeyReleased(NativeKeyEvent e) {
    if (Config.KEY_LISTENER_DEBUG) {
      System.out.println(
          "Key Released: "
              + NativeKeyEvent.getModifiersText(e.getModifiers())
              + "+"
              + NativeKeyEvent.getKeyText(e.getKeyCode()));
    }

    if (e.getKeyCode() == VC_V) {
      if ((e.getModifiers() & CTRL_MASK) != 0) {
        if (pasteCallback != null) {
          pasteCallback.run();
          pasteCallback = null;
        }
      }
    }
  }
}
