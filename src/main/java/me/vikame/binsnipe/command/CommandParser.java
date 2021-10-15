package me.vikame.binsnipe.command;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import me.vikame.binsnipe.command.impl.ExitCommand;
import me.vikame.binsnipe.command.impl.SetCommand;

public class CommandParser {

  private final Console console;
  private final Map<String, Command> commandMap;

  public CommandParser(Console console) {
    this.console = console;
    this.commandMap = new HashMap<>();

    registerCommand("exit", new ExitCommand());
    registerCommand("set", new SetCommand());
  }

  public void registerCommand(String name, Command command) {
    this.commandMap.put(name.toLowerCase(), command);
  }

  public void start() {
    String in;
    while ((in = console.readLine()) != null){
      in = in.toLowerCase().trim();

      String command = in;
      String[] parts;
      if(command.contains(" ")){
        String[] split = command.split(" ");
        parts = new String[split.length-1];
        System.arraycopy(split, 1, parts, 0, parts.length);
        command = split[0];
      } else {
        parts = new String[0];
      }

      Command cmd = commandMap.get(command.toLowerCase());
      if(cmd != null) {
        cmd.execute(command, parts);
      }
    }
  }

}
