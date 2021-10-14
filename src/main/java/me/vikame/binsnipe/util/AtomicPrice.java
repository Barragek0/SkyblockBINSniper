package me.vikame.binsnipe.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import me.doubledutch.lazyjson.LazyObject;

public class AtomicPrice {

  /*
   * I don't love how I've implemented this class, or more specifically the Atomic field spam.
   *
   * It's clearly the simplest option for a thread-safe, non-blocking object for the held
   * information, but I believe there is plenty of room for improvement.
   */

  private final AtomicReference<String> lowestItemName;
  private final AtomicReference<String> lowestItemNameOriginal;
  private final AtomicReference<String> lowestKey;
  private final AtomicInteger lowestValue;
  private final AtomicLong lowestElapsedTime;
  private final AtomicInteger secondLowestValue;
  private final AtomicInteger totalCount;

  public AtomicPrice() {
    this.lowestItemName = new AtomicReference<>();
    this.lowestItemNameOriginal = new AtomicReference<>();
    this.lowestKey = new AtomicReference<>();
    this.lowestValue = new AtomicInteger(-1);
    this.lowestElapsedTime = new AtomicLong(-1);
    this.secondLowestValue = new AtomicInteger(-1);
    this.totalCount = new AtomicInteger(0);
  }

  void reset() {
    this.lowestItemName.lazySet(null);
    this.lowestItemNameOriginal.lazySet(null);
    this.lowestKey.lazySet(null);
    this.lowestValue.lazySet(-1);
    this.lowestElapsedTime.lazySet(-1);
    this.secondLowestValue.lazySet(-1);
    this.totalCount.lazySet(0);
  }

  public void tryUpdatePrice(String itemName, LazyObject binData) {
    long elapsed = System.currentTimeMillis() - binData.getLong("start");
    String id = binData.getString("uuid");
    int newPrice = binData.getInt("starting_bid");

    int lowest = lowestValue.get();
    int secondLowest = secondLowestValue.get();

    if (lowest == -1 || newPrice < lowest) {
      secondLowestValue.set(lowest);

      lowestItemName.set(itemName);
      lowestElapsedTime.set(elapsed);
      lowestKey.set(id);
      lowestValue.set(newPrice);
    } else if (secondLowest == -1 || newPrice < secondLowest) {
      secondLowestValue.set(newPrice);
    }

    totalCount.incrementAndGet();
  }

  public int getTotalCount() {
    return totalCount.get();
  }

  public String getLowestItemName() {
    return lowestItemName.get();
  }

  public String getLowestItemNameOriginal() {
    return lowestItemNameOriginal.get();
  }

  public String getLowestKey() {
    return lowestKey.get();
  }

  public int getLowestValue() {
    return lowestValue.get();
  }

  public int getSecondLowestValue() {
    return secondLowestValue.get();
  }

  public long getLowestElapsedTime() {
    return lowestElapsedTime.get();
  }

  public int getProjectedProfit() {
    return secondLowestValue.get() - lowestValue.get();
  }

  public static class UnboundedAtomicPricePool extends UnboundedObjectPool<AtomicPrice> {

    public UnboundedAtomicPricePool(int startSize) {
      super(startSize);
    }

    @Override
    public void offer(AtomicPrice object) {
      object.reset();
      super.offer(object);
    }

    @Override
    protected AtomicPrice create() {
      return new AtomicPrice();
    }
  }
}
