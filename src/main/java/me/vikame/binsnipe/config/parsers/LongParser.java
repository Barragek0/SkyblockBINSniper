package me.vikame.binsnipe.config.parsers;

import java.text.NumberFormat;
import me.vikame.binsnipe.config.Parser;

public class LongParser extends Parser<Long> {

  public LongParser() {
    super(Long.class);
  }

  @Override
  public Long fromString(String string) {
    return Long.parseLong(string.replace(",", ""));
  }

  @Override
  public String toString(Long object) {
    return NumberFormat.getInstance().format(object);
  }
}
