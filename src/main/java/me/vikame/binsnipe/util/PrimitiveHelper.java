package me.vikame.binsnipe.util;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class PrimitiveHelper {

  private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS;

  static {
    PRIMITIVES_TO_WRAPPERS = new HashMap<>();
    PRIMITIVES_TO_WRAPPERS.put(boolean.class, Boolean.class);
    PRIMITIVES_TO_WRAPPERS.put(byte.class, Byte.class);
    PRIMITIVES_TO_WRAPPERS.put(char.class, Character.class);
    PRIMITIVES_TO_WRAPPERS.put(double.class, Double.class);
    PRIMITIVES_TO_WRAPPERS.put(float.class, Float.class);
    PRIMITIVES_TO_WRAPPERS.put(int.class, Integer.class);
    PRIMITIVES_TO_WRAPPERS.put(long.class, Long.class);
    PRIMITIVES_TO_WRAPPERS.put(short.class, Short.class);
    PRIMITIVES_TO_WRAPPERS.put(void.class, Void.class);
  }

  public static <T> Class<T> wrap(Class<T> c) {
    if (c.isArray()) {
      Class<?> wrappedArrayType = wrap(c.getComponentType());

      return (Class<T>) Array.newInstance(wrappedArrayType, 0).getClass();
    }

    return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
  }

  public static <T> T unbox(Class<T> cls, Number number) {
    Object result = null;
    if(cls == byte.class) result = number.byteValue();
    else if(cls == double.class) result = number.doubleValue();
    else if(cls == float.class) result = number.floatValue();
    else if(cls == int.class) result = number.intValue();
    else if(cls == long.class) result = number.longValue();
    else if(cls == short.class) result = number.shortValue();

    return result != null ? (T) result : null;
  }
}
