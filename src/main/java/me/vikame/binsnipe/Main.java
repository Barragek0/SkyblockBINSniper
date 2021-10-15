package me.vikame.binsnipe;

import java.awt.GraphicsEnvironment;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.vikame.binsnipe.command.CommandParser;
import me.vikame.binsnipe.config.ConfigFile;
import me.vikame.binsnipe.util.KeyboardListener;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

/* A (hopefully) simple to use BIN sniper for Hypixel Skyblock.
 *
 * Heavily inspired by 'csjh' and their SkyblockSniper python script (https://github.com/csjh/SkyblockSniper)
 */
public class Main {

  private static ScheduledExecutorService THREAD_POOL;
  public static ConfigFile config;

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

      config = new ConfigFile(new File(System.getProperty("user.dir"), "BINSniper.config"));
      config.load();

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

      // This should never occur, but just in-case!
      if(console != null) {
        CommandParser commandParser = new CommandParser(console);
        commandParser.start();
      }
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

  static <T> CompletableFuture<T> exec(Supplier<T> supplier) {
    return CompletableFuture.supplyAsync(supplier, THREAD_POOL);
  }

  static void schedule(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
    THREAD_POOL.scheduleAtFixedRate(runnable, initialDelay, delay, unit);
  }
}
