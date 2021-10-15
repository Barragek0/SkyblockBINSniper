package me.vikame.binsnipe.config.parsers;

import java.text.NumberFormat;
import me.vikame.binsnipe.config.Parser;

public class IntegerParser extends Parser<Integer> {

  public IntegerParser() {
    super(Integer.class);
  }

  @Override
  public Integer fromString(String string) {
    return Integer.parseInt(string.replace(",", ""));
  }

  @Override
  public String toString(Integer object) {
    return NumberFormat.getInstance().format(object);
  }
}
