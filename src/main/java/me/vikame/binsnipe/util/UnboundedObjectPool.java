package me.vikame.binsnipe.util;

import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class UnboundedObjectPool<T> {

  private ConcurrentLinkedQueue<T> pool;

  public UnboundedObjectPool(int startSize) {
    pool = new ConcurrentLinkedQueue<>();

    for (int i = 0; i < startSize; i++) {
      pool.add(create());
    }
  }

  public T borrow() {
    T object;
    if ((object = pool.poll()) == null) {
      object = create();
    }

    return object;
  }

  public void offer(T object) {
    pool.offer(object);
  }

  protected abstract T create();

}
