package me.vikame.binsnipe.config.parsers;

import me.vikame.binsnipe.config.Parser;

public class BooleanParser extends Parser<Boolean> {

  public BooleanParser() {
    super(Boolean.class);
  }

  @Override
  public Boolean fromString(String string) {
    return Boolean.parseBoolean(string);
  }

  @Override
  public String toString(Boolean object) {
    return "" + object;
  }
}
