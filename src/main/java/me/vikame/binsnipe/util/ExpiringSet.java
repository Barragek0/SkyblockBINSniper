package me.vikame.binsnipe.util;

import java.util.*;

public class ExpiringSet<T> implements Set<T> {

  private final Map<T, Long> entries;
  private final long timer;

  public ExpiringSet(long timer) {
    entries = new HashMap<>();
    this.timer = timer;
  }

  private void expire() {
    entries.entrySet().removeIf(entry -> entry.getValue() - System.currentTimeMillis() <= 0L);
  }

  @Override
  public int size() {
    expire();
    return entries.size();
  }

  @Override
  public boolean isEmpty() {
    expire();
    return entries.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    expire();
    return entries.containsKey(o);
  }

  @Override
  public Iterator<T> iterator() {
    expire();
    return entries.keySet().iterator();
  }

  @Override
  public Object[] toArray() {
    expire();
    return entries.keySet().toArray();
  }

  @Override
  public <T1> T1[] toArray(T1[] a) {
    expire();
    return entries.keySet().toArray(a);
  }

  @Override
  public boolean add(T t) {
    expire();
    entries.put(t, System.currentTimeMillis() + timer);
    return true;
  }

  @Override
  public boolean remove(Object o) {
    expire();
    return entries.remove(o) != null;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    expire();
    for (Object o : c) {
      if (!entries.containsKey(o)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    expire();
    for (T t : c) {
      add(t);
    }

    return true;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    expire();
    return entries.entrySet().removeIf(entry -> !c.contains(entry));
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    expire();
    return entries.entrySet().removeIf(c::contains);
  }

  @Override
  public void clear() {
    entries.clear();
    expire();
  }
}
