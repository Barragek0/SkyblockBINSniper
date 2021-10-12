package me.vikame.binsnipe;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.vikame.binsnipe.util.KeyboardListener;
import me.vikame.binsnipe.util.PrimitiveHelper;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

/* A (hopefully) simple to use BIN sniper for Hypixel Skyblock.
 *
 * Heavily inspired by 'csjh' and their SkyblockSniper python script (https://github.com/csjh/SkyblockSniper)
 */
public class Main {

  private static ScheduledExecutorService THREAD_POOL;

  public static void main(String[] args) throws IOException, NativeHookException {
    Console console = System.console();
    if (console == null
        && !GraphicsEnvironment.isHeadless()) { // Ensure we have a console window to work with!
      String filename =
          Main.class.getProtectionDomain().getCodeSource().getLocation().toString().substring(6);

      StringBuilder allArgs = new StringBuilder();
      for (String str : args) {
        allArgs.append(str).append(' ');
      }

      /*
       * The above lines of code, alongside this one, start a new instance of the windows command prompt and execute the command below.
       * When the command below is put together, it is 'cmd /c start cmd /k java -jar Dfile.encoding=UTF-8 -Xmx1024M "${filename}" ${allArgs}'
       * which, when ran, simply runs the .JAR file that was executed (the BIN sniper!) and nothing else.
       *
       * This is done to ensure we have a console window to print output to, and so you can easily stop the program (via the exit button on the console)
       * and is in no way a suspicious piece of code. If you *still* do not trust this, you can simply run the JAR file via the command-line yourself
       * (via 'java -jar <jarFile>') which will not trigger this block of code.
       *
       * To add to this, this code is directly taken (and slightly modified) from https://stackoverflow.com/a/29138847
       */
      Runtime.getRuntime()
          .exec(
              new String[] {
                "cmd",
                "/c",
                "start",
                "cmd",
                "/k",
                "java -jar -Dfile.encoding=UTF-8 -Xmx1024M \""
                    + filename
                    + "\" "
                    + allArgs.toString().trim()
              });
    } else {
      System.setErr(System.out);

      File config = new File(System.getProperty("user.dir"), "BINSniper.config");
      if (config.exists()) {
        System.out.println("Loading sniper configuration...");
        try {
          FileReader reader = new FileReader(config);
          Properties properties = new Properties();
          properties.load(reader);

          System.out.println("Configuration:");

          for (Field field : Config.class.getDeclaredFields()) {
            if (!properties.containsKey(field.getName())) {
              properties.setProperty(
                  field.getName(),
                  field.getType().equals(boolean.class)
                          || field.getType().equals(int.class)
                          || field.getType().equals(long.class)
                          || field.getType().equals(float.class)
                      ? String.valueOf(field.get(Main.class))
                      : field.getType().equals(java.util.List.class)
                          ? String.join(",", (java.util.List) field.get(Main.class))
                          : "");
              properties.store(new FileOutputStream(config), null);
            }
            try {
              String prop = properties.getProperty(field.getName());

              Class<?> type = PrimitiveHelper.wrap(field.getType());
              if (type == Boolean.class) {
                field.setBoolean(null, Boolean.parseBoolean(prop));
              } else if (type == Integer.class) {
                field.setInt(null, Integer.parseInt(prop.replace(",", "")));
              } else if (type == Float.class) {
                field.set(null, Float.parseFloat(prop.replace(",", "")));
              } else if (type == Long.class) {
                field.set(null, Long.parseLong(prop.replace(",", "")));
              } else if (type == java.util.List.class) {
                field.set(null, Stream.of(prop.split(",", -1)).collect(Collectors.toList()));
              } else {
                System.out.println(
                    "Could not parse data type for "
                        + field.getName()
                        + " with type "
                        + field.getType()
                        + ".");
                continue;
              }

              if (Number.class.isAssignableFrom(type)) {
                prop = NumberFormat.getInstance().format(field.get(null));
              }

              System.out.println(" > " + field.getName() + ": " + prop);
            } catch (IllegalAccessException e) {
              e.printStackTrace();
              System.out.println("Failed to set '" + field + "' to config value.");
            }
          }

          reader.close();
        } catch (IOException | IllegalAccessException e) {
          e.printStackTrace();
          System.out.println("Failed to read configuration data. Using defaults...");
        }
      } else if (config.createNewFile()) {
        Properties properties = new Properties();

        for (Field field : Config.class.getDeclaredFields()) {
          if (Modifier.isTransient(field.getModifiers())) {
            continue;
          }

          try {
            Object value = field.get(null);

            String propValue;
            if (value instanceof Number) {
              propValue = NumberFormat.getInstance().format(value);
            } else {
              propValue = value.toString();
            }

            properties.setProperty(field.getName(), propValue);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.out.println("Failed to get default config value for '" + field + "'");
          }
        }

        FileWriter writer = new FileWriter(config);
        properties.store(writer, "Skyblock BIN Sniper configuration data");
        writer.close();

        System.out.println(
            "A Configuration file to edit sniper preferences has been created at '"
                + config.getAbsolutePath()
                + "'");
      } else {
        System.out.println(
            "Couldn't create configuration file! You will be forced to use default sniper preferences.");
      }

      THREAD_POOL = Executors.newScheduledThreadPool(Config.POOLED_THREAD_COUNT);

      // only start keyboard listener if we're going to iterate
      if (Config.ITERATE_RESULTS_TO_CLIPBOARD) {
        // Logger defaults to all, change it to warning
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        // Register the keyboard listener
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(new KeyboardListener());
      }

      BINSniper sniper = new BINSniper();

      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    shutdown();
                    sniper.cleanup();

                    try {
                      GlobalScreen.unregisterNativeHook();
                    } catch (NativeHookException e) {
                      e.printStackTrace();
                    }
                  }));

      System.out.println();
    }
  }

  static void printDebug(String line) {
    if (Config.DEBUG) {
      System.out.println(line);
    }
  }

  public static ScheduledExecutorService pool() {
    return THREAD_POOL;
  }

  private static void shutdown() {
    THREAD_POOL.shutdown();
    try {
      if (!THREAD_POOL.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
        System.err.println("Failed to block until thread-pool finished queued tasks.");
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  static CompletableFuture<Void> exec(Runnable runnable) {
    return CompletableFuture.runAsync(runnable, THREAD_POOL);
  }

  static void schedule(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
    THREAD_POOL.scheduleAtFixedRate(runnable, initialDelay, delay, unit);
  }
}
