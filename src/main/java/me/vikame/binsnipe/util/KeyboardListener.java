package me.vikame.binsnipe.util;

import me.vikame.binsnipe.Config;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.awt.*;

import static org.jnativehook.NativeInputEvent.*;
import static org.jnativehook.keyboard.NativeKeyEvent.*;

public class KeyboardListener implements NativeKeyListener {

  public static Runnable pasteCallback;

  @Override
  public void nativeKeyTyped(NativeKeyEvent e) {}

  @Override
  public void nativeKeyPressed(NativeKeyEvent e) {}

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

    if (e.getKeyCode() == VC_T
        && (e.getModifiers() & CTRL_MASK) == 0
        && (e.getModifiers() & SHIFT_MASK) == 0
        && (e.getModifiers() & ALT_MASK) == 0
        && Config.AUTOMATICALLY_PASTE_AFTER_PRESSING_CHAT_KEYBIND) {
      try {
        Robot robot = new Robot();
        robot.keyPress(VC_CONTROL);
        robot.delay(15 + (int) (Math.random() * 10));
        robot.keyPress(VC_V);
        robot.delay(15 + (int) (Math.random() * 10));
        robot.keyRelease(VC_V);
        robot.delay(15 + (int) (Math.random() * 10));
        robot.keyRelease(VC_CONTROL);
      } catch (AWTException ex) {
        ex.printStackTrace();
      }
    }
  }
}
