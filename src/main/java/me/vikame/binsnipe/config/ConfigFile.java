package me.vikame.binsnipe.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import me.vikame.binsnipe.Config;
import me.vikame.binsnipe.config.parsers.ArrayParser;
import me.vikame.binsnipe.config.parsers.BooleanParser;
import me.vikame.binsnipe.config.parsers.DoubleParser;
import me.vikame.binsnipe.config.parsers.FloatParser;
import me.vikame.binsnipe.config.parsers.IntegerParser;
import me.vikame.binsnipe.config.parsers.LongParser;
import me.vikame.binsnipe.config.parsers.StringParser;
import me.vikame.binsnipe.util.PrimitiveHelper;

public class ConfigFile {

  private final File configFile;
  private final Map<Class<?>, Parser<?>> parsers;

  public ConfigFile(File configFile) {
    this.configFile = configFile;
    this.parsers = new HashMap<>();

    registerParser(new StringParser());
    registerParser(new LongParser());
    registerParser(new IntegerParser());
    registerParser(new FloatParser());
    registerParser(new DoubleParser());
    registerParser(new BooleanParser());
  }

  public void registerParser(Parser<?> parser){
    parsers.put(parser.getParses(), parser);
    ArrayParser<?> arrayParser = new ArrayParser<>(parser);
    parsers.put(arrayParser.getParses(), arrayParser);
  }

  public Parser<?> getParser(Class<?> cls) {
    return parsers.get(PrimitiveHelper.wrap(cls));
  }

  public void load() throws IOException {
    Properties properties = new Properties();

    boolean needsStore = false;
    if (configFile.exists()) {
      System.out.println("Loading sniper configuration...");
      try {
        FileReader reader = new FileReader(configFile);
        properties.load(reader);

        System.out.println("Configuration:");

        for (Field field : Config.class.getDeclaredFields()) {
          if(!field.isAccessible()) field.setAccessible(true);

          Parser parser = getParser(field.getType());
          if(parser == null) {
            System.out.println("No parser for type " + field.getType() + "!");
            continue;
          }

          if(!properties.containsKey(field.getName())) {
            if(!Modifier.isTransient(field.getModifiers())) {
              //noinspection unchecked
              String stringValue = parser.toString(field.get(null));
              properties.setProperty(field.getName(), stringValue);
              needsStore = true;
            }

            continue;
          }

          try {
            String prop = properties.getProperty(field.getName());

            Object value = parser.fromString(prop);
            if(value instanceof Number) {
              value = PrimitiveHelper.unbox(field.getType(), (Number) value);
              prop = NumberFormat.getInstance().format(value);
            }

            field.set(null, value);
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
    } else if (configFile.createNewFile()) {
      for (Field field : Config.class.getDeclaredFields()) {
        if (!field.isAccessible())
          field.setAccessible(true);

        if (Modifier.isTransient(field.getModifiers())) {
          continue;
        }

        Parser parser = getParser(field.getType());
        if(parser == null) {
          System.out.println("No parser for type " + field.getType() + "!");
          continue;
        }

        try {
          //noinspection unchecked
          properties.setProperty(field.getName(), parser.toString(field.get(null)));
        } catch (IllegalAccessException e) {
          e.printStackTrace();
          System.out.println("Failed to get default config value for '" + field + "'");
        }
      }

      System.out.println(
          "A Configuration file to edit sniper preferences has been created at '"
              + configFile.getAbsolutePath()
              + "'");
      needsStore = true;
    } else {
      System.out.println(
          "Couldn't create configuration file! You will be forced to use default sniper preferences.");
    }

    if(needsStore) {
      FileWriter writer = new FileWriter(configFile);
      properties.store(writer, "Skyblock BIN Sniper configuration data");
      writer.close();
    }
  }

}
