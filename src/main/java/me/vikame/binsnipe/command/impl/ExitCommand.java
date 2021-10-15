package me.vikame.binsnipe.command.impl;

import me.vikame.binsnipe.command.Command;

public class ExitCommand extends Command {

  @Override
  public void execute(String command, String[] args) {
    System.runFinalization();
    System.exit(0);
  }

}
