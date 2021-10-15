package me.vikame.binsnipe.config.parsers;

import java.text.NumberFormat;
import me.vikame.binsnipe.config.Parser;

public class DoubleParser extends Parser<Double> {

  public DoubleParser() {
    super(Double.class);
  }

  @Override
  public Double fromString(String string) {
    return Double.parseDouble(string);
  }

  @Override
  public String toString(Double object) {
    return NumberFormat.getInstance().format(object);
  }
}
