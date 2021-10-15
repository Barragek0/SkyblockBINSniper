package me.vikame.binsnipe.command.impl;

import java.io.IOException;
import java.lang.reflect.Field;
import me.vikame.binsnipe.Config;
import me.vikame.binsnipe.Main;
import me.vikame.binsnipe.command.Command;
import me.vikame.binsnipe.config.Parser;

public class SetCommand extends Command {

  @Override
  public void execute(String command, String[] args) {
    if(args.length < 2) {
      System.out.println("Usage: set <config_key> <value>");
      return;
    }

    StringBuilder keyBuilder = new StringBuilder();
    for(int i = 0; i < args.length-1; i++) {
      keyBuilder.append(args[i]).append(' ');
    }

    String key = keyBuilder.toString().trim();
    String fieldName = key.toUpperCase().replace(" ", "_");
    String value = args[args.length-1];

    if(fieldName.equals("POOLED_THREAD_COUNT") || fieldName.equals("UPDATE_CHECK_RATE")) {
      System.out.println("You cannot change " + fieldName + " at runtime.");
      System.out.println("Please change this value in the configuration file!");
      return;
    }

    Field field;
    try {
      field = Config.class.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      System.out.println("No config key named " + key + ".");
      return;
    }

    if(!field.isAccessible())
      field.setAccessible(true);

    Parser parser = Main.config.getParser(field.getType());
    if(parser == null) {
      System.out.println("No parser for config type " + field.getType() + ".");
      return;
    }

    Object parsed;
    try{
      parsed = parser.fromString(value);
    }catch(Exception e) {
      parsed = null;
    }

    if(parsed == null) {
      System.out.println("Invalid value for key " + fieldName + ". Value must be a(n) " + field.getType().getSimpleName() + ".");
      return;
    }

    try {
      field.set(null, parsed);
    } catch (IllegalAccessException e) {
      if(Config.OUTPUT_ERRORS) e.printStackTrace();
      return;
    }

    try {
      Main.config.save();
    } catch (IOException e) {
      if(Config.OUTPUT_ERRORS) e.printStackTrace();
      return;
    }

    System.out.println();
    System.out.println("Successfully set " + fieldName + " to " + value);
  }

}
