package me.vikame.binsnipe.config.parsers;

import me.vikame.binsnipe.config.Parser;

public class StringParser extends Parser<String> {

  // NOTE: This class seems silly at first, but due to the way parsers are structured this is necessary.
  public StringParser() {
    super(String.class);
  }

  @Override
  public String fromString(String string) {
    return string;
  }

  @Override
  public String toString(String object) {
    return object;
  }
}
