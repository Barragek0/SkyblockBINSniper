package me.vikame.binsnipe.config.parsers;

import me.vikame.binsnipe.config.Parser;

public class BooleanParser extends Parser<Boolean> {

  public BooleanParser() {
    super(Boolean.class);
  }

  @Override
  public Boolean fromString(String string) {
    if(string.equalsIgnoreCase("true") || string.equalsIgnoreCase("yes")) return true;
    else if(string.equalsIgnoreCase("false") || string.equalsIgnoreCase("no")) return false;
    else throw new RuntimeException("Cannot parse string " + string + " to boolean.");
  }

  @Override
  public String toString(Boolean object) {
    return "" + object;
  }
}
