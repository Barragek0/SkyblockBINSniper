package me.vikame.binsnipe.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AtomicPrice {

  private final AtomicReference<String> lowestItemName;
  private final AtomicReference<String> lowestKey;
  private final AtomicInteger lowestValue;
  private final AtomicInteger secondLowestValue;
  private final AtomicInteger totalCount;

  public AtomicPrice() {
    this.lowestItemName = new AtomicReference<>();
    this.lowestKey = new AtomicReference<>();
    this.lowestValue = new AtomicInteger(-1);
    this.secondLowestValue = new AtomicInteger(-1);
    this.totalCount = new AtomicInteger(0);
  }

  public void tryUpdatePrice(String itemName, String id, int newPrice) {
    int lowest = lowestValue.get();
    int secondLowest = secondLowestValue.get();

    if (lowest == -1 || newPrice < lowest) {
      secondLowestValue.set(lowest);

      lowestItemName.set(itemName);
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

  public String getLowestKey() {
    return lowestKey.get();
  }

  public int getLowestValue() {
    return lowestValue.get();
  }

  public int getSecondLowestValue() {
    return secondLowestValue.get();
  }

  public int getProjectedProfit() {
    return secondLowestValue.get() - lowestValue.get();
  }

}
