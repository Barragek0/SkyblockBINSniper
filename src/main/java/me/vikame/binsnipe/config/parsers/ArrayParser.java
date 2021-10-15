package me.vikame.binsnipe.config.parsers;

import java.lang.reflect.Array;
import me.vikame.binsnipe.config.Parser;

public class ArrayParser<T> extends Parser<T[]> {

  // NOTE: This class uses some really ugly hacks to achieve a generic array parser for any given type.
  private final Parser<T> baseParser;

  public ArrayParser(Parser<T> baseParser) {
    super((Class<T[]>) Array.newInstance(baseParser.getParses(), 0).getClass());
    this.baseParser = baseParser;
  }

  @Override
  public T[] fromString(String string) {
    String[] parts = string.split(",");

    T[] array = (T[])Array.newInstance(baseParser.getParses(), parts.length);
    for(int i = 0; i < array.length; i++) {
      array[i] = baseParser.fromString(parts[i]);
    }

    return array;
  }

  @Override
  public String toString(T[] object) {
    StringBuilder builder = new StringBuilder();

    for(T t : object) {
      builder.append(baseParser.toString(t)).append(',');
    }

    return builder.deleteCharAt(builder.length()-1).toString();
  }
}
