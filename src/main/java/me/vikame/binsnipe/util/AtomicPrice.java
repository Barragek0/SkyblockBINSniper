package me.vikame.binsnipe.util;

import me.doubledutch.lazyjson.LazyObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class AtomicPrice {

  /*
   * I don't love how I've implemented this class, or more specifically the Atomic field spam.
   *
   * It's clearly the simplest option for a thread-safe, non-blocking object for the held
   * information, but I believe there is plenty of room for improvement.
   */

  private final AtomicReference<String> lowestItemNameFormatted;
  private final AtomicReference<String> lowestItemNameOriginal;
  private final AtomicReference<String> lowestKey;
  private final AtomicInteger lowestValue;
  private final AtomicLong lowestElapsedTime;
  private final AtomicInteger secondLowestValue;
  private final AtomicInteger totalCount;

  public AtomicPrice() {
    lowestItemNameFormatted = new AtomicReference<>();
    lowestItemNameOriginal = new AtomicReference<>();
    lowestKey = new AtomicReference<>();
    lowestValue = new AtomicInteger(-1);
    lowestElapsedTime = new AtomicLong(-1);
    secondLowestValue = new AtomicInteger(-1);
    totalCount = new AtomicInteger(0);
  }

  void reset() {
    lowestItemNameFormatted.lazySet(null);
    lowestItemNameOriginal.lazySet(null);
    lowestKey.lazySet(null);
    lowestValue.lazySet(-1);
    lowestElapsedTime.lazySet(-1);
    secondLowestValue.lazySet(-1);
    totalCount.lazySet(0);
  }

  public void tryUpdatePrice(String itemName, String itemNameOriginal, LazyObject binData) {
    long elapsed = System.currentTimeMillis() - binData.getLong("start");
    String id = binData.getString("uuid");
    int newPrice = binData.getInt("starting_bid");

    int lowest = lowestValue.get();
    int secondLowest = secondLowestValue.get();

    if (lowest == -1 || newPrice < lowest) {
      secondLowestValue.set(lowest);

      lowestItemNameFormatted.set(itemName);
      lowestItemNameOriginal.set(itemNameOriginal);
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

  public String getLowestItemNameFormatted() {
    return lowestItemNameFormatted.get();
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
