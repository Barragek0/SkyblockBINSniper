package me.vikame.binsnipe.config.parsers;

import java.text.NumberFormat;
import me.vikame.binsnipe.config.Parser;

public class FloatParser extends Parser<Float> {

  public FloatParser() {
    super(Float.class);
  }

  @Override
  public Float fromString(String string) {
    return Float.parseFloat(string);
  }

  @Override
  public String toString(Float object) {
    return NumberFormat.getInstance().format(object);
  }
}
