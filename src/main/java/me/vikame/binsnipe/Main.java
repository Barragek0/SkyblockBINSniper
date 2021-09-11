package me.vikame.binsnipe;

import java.awt.GraphicsEnvironment;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/* A (hopefully) simple to use BIN sniper for Hypixel Skyblock.
 *
 * Heavily inspired by 'csjh' and their SkyblockSniper python script (https://github.com/csjh/SkyblockSniper)
 */
public class Main {

  private static ScheduledExecutorService THREAD_POOL;

  public static void main(String[] args) throws IOException {
    Console console = System.console();
    if (console == null
        && !GraphicsEnvironment.isHeadless()) { // Ensure we have a console window to work with!
      String filename = Main.class.getProtectionDomain().getCodeSource().getLocation().toString()
          .substring(6);

      StringBuilder allArgs = new StringBuilder();
      for (String str : args) {
        allArgs.append(str).append(' ');
      }

      Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "cmd", "/k",
          "java -jar -Dfile.encoding=UTF-8 \"" + filename + "\" " + allArgs.toString().trim()});
    } else {
      /* Setting pooled_thread_count to be too high can result in slower speeds depending on the capabilities of your network connection.
       * From the tests I have done, I have noted the values I would recommend you use below.
       *
       * Maximum speed:  1 thread per 12.50Mb/s
       * Moderate speed: 1 thread per 16.67Mb/s
       * General speed:  1 thread per 25.00Mb/s
       *
       * As an example, my own internet connection has a download speed of ~100.0Mb/s, and would mean that for the above values I would use:
       *
       * Maximum speed:  8 threads
       * Moderate speed: 6 threads
       * General speed:  4 threads
       */
      int pooled_thread_count = Runtime.getRuntime().availableProcessors();
      if (args.length > 0) {
        try {
          pooled_thread_count = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
          System.err.println(
              "Invalid argument '" + args[0] + "': expected thread count as an integer");
          return;
        }
      }

      THREAD_POOL = Executors.newScheduledThreadPool(pooled_thread_count);

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
              continue;
            }

            try {
              String prop = properties.getProperty(field.getName());

              Class<?> type = field.getType();
              if (type == boolean.class || type == Boolean.class) {
                field.setBoolean(null, Boolean.parseBoolean(prop));
              } else if (type == int.class || type == Integer.class) {
                field.setInt(null, Integer.parseInt(prop));
              } else if (type == float.class || type == Float.class) {
                field.set(null, Float.parseFloat(prop));
              } else {
                System.out.println("Could not parse data type for " + field.getName() + ".");
                continue;
              }

              System.out.println(" > " + field.getName() + ": " + prop);
            } catch (IllegalAccessException e) {
              e.printStackTrace();
              System.out.println("Failed to get default config value for '" + field + "'");
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
          System.out.println("Failed to read configuration data. Using defaults...");
        }
      } else {
        if (!config.createNewFile()) {
          System.out.println(
              "Couldn't create configuration file! You will be forced to use default sniper preferences.");
        } else {
          Properties properties = new Properties();

          for (Field field : Config.class.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers())) {
              continue;
            }

            try {
              properties.setProperty(field.getName(), field.get(null).toString());
            } catch (IllegalAccessException e) {
              e.printStackTrace();
              System.out.println("Failed to get default config value for '" + field + "'");
            }
          }

          FileWriter writer = new FileWriter(config);
          properties.store(writer, "Skyblock BIN Sniper configuration data");
          writer.close();

          System.out.println("A Configuration file to edit sniper preferences has been created at '"
              + config.getAbsolutePath() + "'");
        }
      }

      Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdown));

      System.out.println();

      new BINSniper();
    }
  }

  public static ScheduledExecutorService pool() {
    return THREAD_POOL;
  }

  public static void shutdown() {
    THREAD_POOL.shutdown();
    try {
      if (!THREAD_POOL.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
        System.err.println("Failed to block until thread-pool finished queued tasks.");
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static void exec(Runnable runnable) {
    THREAD_POOL.submit(runnable);
  }

  public static <T> Future<T> exec(Callable<T> callable) {
    return THREAD_POOL.submit(callable);
  }

  public static void execLater(Runnable runnable, long delay, TimeUnit unit) {
    THREAD_POOL.schedule(runnable, delay, unit);
  }

  public static void schedule(Runnable runnable, long initialDelay, long delay, TimeUnit unit) {
    THREAD_POOL.scheduleAtFixedRate(runnable, initialDelay, delay, unit);
  }

  public static <T> void execAll(Collection<Callable<T>> collection) {
    for (Callable<T> callable : collection) {
      THREAD_POOL.submit(callable);
    }
  }

  public static <T> void execAll(Collection<T> collection, Consumer<T> consumer) {
    for (final T t : collection) {
      Callable<T> callable = () -> {
        consumer.accept(t);
        return t;
      };

      THREAD_POOL.submit(callable);
    }
  }

  public static <T> void execAll(Callable<T>[] array) {
    execAll(Arrays.asList(array));
  }

  public static <T> void execAll(T[] array, Consumer<T> consumer) {
    execAll(Arrays.asList(array), consumer);
  }

}
