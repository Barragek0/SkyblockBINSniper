package me.vikame.binsnipe.config;

public abstract class Parser<T> {

  private final Class<T> parses;

  public Parser(Class<T> parses) {
    this.parses = parses;
  }

  public Class<T> getParses() {
    return parses;
  }

  public abstract T fromString(String string);
  public abstract String toString(T object);

}
