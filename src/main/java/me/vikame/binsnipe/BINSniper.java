package me.vikame.binsnipe;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import me.doubledutch.lazyjson.LazyArray;
import me.doubledutch.lazyjson.LazyObject;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.vikame.binsnipe.util.AtomicPrice;
import me.vikame.binsnipe.util.ExpiringSet;
import me.vikame.binsnipe.util.SBHelper;

public class BINSniper {

  // An object used for synchronization during loading bar printing.
  private final Object lock = new Object();

  // Important information required by the BIN sniper.
  private final AtomicInteger totalPages;
  private final AtomicLong timeLastUpdated;
  private final Map<String, AtomicPrice> binPrices;

  // We don't want to show the same flip multiple times, so we store a map of already-shown flips and the timestamp in which it should be shown again.
  private final ExpiringSet<String> flipsAlreadyShown;

  // Statistics to be shown when a flip is found.
  private final AtomicInteger totalAuctions;
  private final AtomicInteger totalBins;

  public BINSniper() {
    System.out.println("Starting BIN sniper...");

    totalPages = new AtomicInteger(-1);
    timeLastUpdated = new AtomicLong(-1);
    binPrices = new ConcurrentHashMap<>();

    flipsAlreadyShown = new ExpiringSet<>(
        60000 * 5);

    totalAuctions = new AtomicInteger();
    totalBins = new AtomicInteger();

    // Make an API request to get initial data before the sniper starts.
    getAuctions(0);

    Main.schedule(() -> {
      long lastUpdateTime = timeLastUpdated.get();
      if ((System.currentTimeMillis() - lastUpdateTime) > 60000) {
        getAuctions(0);
        long newUpdateTime = timeLastUpdated.get();
        long actualDelay = System.currentTimeMillis() - newUpdateTime;

        if (lastUpdateTime != newUpdateTime) {
          AtomicInteger completed = new AtomicInteger(0);
          final int maxPages = totalPages.get();

          printLoadingBar(0, maxPages);

          List<CompletableFuture<Void>> futures = new LinkedList<>();

          long start = System.currentTimeMillis();
          for (int page = 0; page < maxPages; page++) {
            final int workingPage = page;
            CompletableFuture<Void> future = Main.exec(() -> {
              if (workingPage
                  < totalPages.get()) { // Ensure that this is still a valid page we need to look at.
                LazyArray auctionArray = getAuctions(workingPage);
                if (auctionArray != null) {
                  for (int i = 0; i < auctionArray.length(); i++) {
                    LazyObject binData = auctionArray.getJSONObject(i);
                    if (!SBHelper.isExistingBIN(binData)) {
                      continue;
                    }

                    if (Config.IGNORE_FURNITURE && SBHelper.isFurniture(binData)) {
                      continue;
                    }
                    if (Config.IGNORE_COSMETICS && SBHelper.isCosmetic(binData)) {
                      continue;
                    }
                    if (Config.IGNORE_USED_CAKE_SOULS && SBHelper.isUsedCakeSoul(binData)) {
                      continue;
                    }

                    String itemId;
                    StringBuilder itemName = new StringBuilder(
                        SBHelper.stripInvalidChars(binData.getString("item_name")));

                    try {
                      NBTCompound itemData = NBTReader.readBase64(binData.getString("item_bytes"))
                          .getList("i")
                          .stream().findFirst()
                          .map(o -> o instanceof NBTCompound ? (NBTCompound) o : null)
                          .orElseThrow(() -> new RuntimeException("Failed to parse NBT data."))
                          .getCompound("tag");

                      NBTCompound skyblockAttributes = itemData.getCompound("ExtraAttributes");

                      String reforge = skyblockAttributes.getString("modifier");
                      String id = skyblockAttributes.getString("id");
                      boolean recombed = skyblockAttributes.getInt("rarity_upgrades", 0) > 0;
                      int hotPotatoBooks = skyblockAttributes.getInt("hot_potato_count", 0);
                      int stars = skyblockAttributes.getInt("dungeon_item_level", 0);
                      String skin = skyblockAttributes.getString("skin");

                      if (Config.IGNORE_STARS && id.startsWith("STARRED_")) {
                        id = id.substring(8);
                      }

                      if (id.equalsIgnoreCase("PET")) {
                        LazyObject petInfo = new LazyObject(
                            skyblockAttributes.getString("petInfo"));

                        String type = petInfo.getString("type");
                        id = "PET_" + type;
                        skin = petInfo.has("skin") ? petInfo.getString("skin") : null;
                      }

                      itemId = id
                          + (Config.IGNORE_RECOMB ? "" : "|" + recombed)
                          + (Config.IGNORE_REFORGES ? "" : "|" + reforge)
                          + (Config.IGNORE_HOT_POTATO ? "" : "|" + hotPotatoBooks)
                          + (Config.IGNORE_STARS ? "" : "|" + stars)
                          + (Config.IGNORE_SKINS ? "" : "|" + skin);

                      for (int star = 0; star < stars; star++) {
                        itemName.append("*");
                      }

                      itemName.append(" [").append(binData.getString("tier"))
                          .append(recombed ? ", RECOMB" : "").append("]");

                      if (skin != null) {
                        itemName.append(" [").append(skin).append("]");
                      }
                    } catch (Exception e) {
                      e.printStackTrace();
                      continue;
                    }

                    String filteredName = itemName.toString()
                        .replace("✪", "")
                        .replace(" ✦", "")
                        .replace("⚚", "Fragged");

                    binPrices.computeIfAbsent(itemId, ign -> new AtomicPrice())
                        .tryUpdatePrice(filteredName, binData);
                    totalBins.incrementAndGet();
                  }
                }
              }
            });

            future.thenRun(() -> {
              printLoadingBar(completed.incrementAndGet(), maxPages);
            });
            futures.add(future);
          }

          for (CompletableFuture<Void> future : futures) {
            if (!future.isDone()) {
              try {
                future.get(Config.TIMEOUT, TimeUnit.MILLISECONDS);
              } catch (InterruptedException | ExecutionException | TimeoutException e) {
                clearLoadingBar();
                System.out.println("Flips took too long to process, and timed out.");
                return;
              }
            }
          }

          clearLoadingBar();

          TreeSet<Map.Entry<String, AtomicPrice>> flips = new TreeSet<>(
              Comparator.comparingInt(o -> o.getValue().getProjectedProfit()));

          for (Map.Entry<String, AtomicPrice> entry : binPrices.entrySet()) {
            if (flipsAlreadyShown.contains(entry.getKey())) {
              continue;
            }

            AtomicPrice price = entry.getValue();
            int lowest = price.getLowestValue();
            int second = price.getSecondLowestValue();

            if (price.getTotalCount() >= Config.MIN_ITEMS_ON_MARKET
                && second >= Config.MIN_BIN_PRICE
                && lowest <= Config.MAX_BIN_PRICE
                && price.getLowestElapsedTime() <= Config.OLD_THRESHOLD) {
              int secondWithTaxes = SBHelper.calculateWithTaxes(second);

              int diff = secondWithTaxes - lowest;
              float profitPercentage = ((float) secondWithTaxes / (float) lowest) * 100.0f;

              if (profitPercentage >= Config.MIN_PROFIT_PERCENTAGE
                  || diff >= Config.MIN_PROFIT_AMOUNT) {
                Main.printDebug("Found flippable item '" + entry.getKey() + "' (" + entry.getValue()
                    .getLowestItemName() + ")");
                if (flips.size() < Config.MAX_FLIPS_TO_SHOW) {
                  flips.add(entry);
                } else {
                  Map.Entry<String, AtomicPrice> first = flips.first();
                  AtomicPrice firstPrice = first.getValue();

                  if (firstPrice.getProjectedProfit() < price.getProjectedProfit()) {
                    flips.pollFirst();
                    flips.add(entry);
                  }
                }
              }
            }
          }

          long timeTaken = System.currentTimeMillis() - start;
          if (!flips.isEmpty()) {
            for (Map.Entry<String, AtomicPrice> entry : flips) {
              flipsAlreadyShown.add(entry.getKey());

              AtomicPrice price = entry.getValue();

              int lowest = price.getLowestValue();
              int second = price.getSecondLowestValue();

              int secondWithTaxes = SBHelper.calculateWithTaxes(second);

              int diff = secondWithTaxes - lowest;
              float profitPercentage = (((float) secondWithTaxes / (float) lowest) * 100.0f) - 100;

              System.out.println("/viewauction " + price.getLowestKey()
                  + " | Item: " + price.getLowestItemName()
                  + " | # BIN'd on AH: " + price.getTotalCount()
                  + " | Price: " + NumberFormat.getInstance().format(lowest)
                  + " | Second lowest: " + NumberFormat.getInstance().format(second)
                  + " | Profit (incl. taxes): " + NumberFormat.getInstance().format(diff) + " (+"
                  + profitPercentage + "%) (" + timeTaken + "ms)");
            }

            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(
                    new StringSelection("/viewauction " + flips.last().getValue().getLowestKey()),
                    null);

            Main.printDebug("Flip timing information:");
            Main.printDebug(" > " + timeTaken + "ms for page processing");
            Main.printDebug(" > " + actualDelay + "ms for page update");
            Main.printDebug(
                " > In total, " + (System.currentTimeMillis() - newUpdateTime) + "ms late");

            if (Config.SOUND_WHEN_FLIP_FOUND) {
              Toolkit.getDefaultToolkit().beep();
            }
          } else {
            System.out.println("Unable to find a flip after " + timeTaken + " ms.");
          }

          binPrices.clear();
          totalBins.set(0);
          System.out.println();

          if (Config.EXPLICIT_GC_AFTER_FLIP) {
            System.gc();
          }
        }
      }
    }, 0, 1000, TimeUnit.MILLISECONDS);
  }

  private LazyArray getAuctions(int page) {
    String targetURL =
        Constants.AUCTIONS_ENDPOINT + "?page=" + page + (Config.FORCE_NO_CACHE_API_REQUESTS ? "&_="
            + System.currentTimeMillis() : "");
    URL apiURL;
    try {
      apiURL = new URL(targetURL);
    } catch (MalformedURLException e) {
      e.printStackTrace();
      System.err.println("Malformed URL '" + targetURL + "'");
      System.exit(1);
      return null;
    }

    try {
      HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent",
          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36");
      connection.setRequestProperty("Content-Type", "application/json");

      if (Config.USE_GZIP_COMPRESSION_ON_API_REQUESTS) {
        connection.setRequestProperty("Accept-Encoding", "gzip");
      }

      if (Config.FORCE_NO_CACHE_API_REQUESTS) {
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setUseCaches(false);
      }

      try {
        connection.connect();
      } catch (IOException e) {
        e.printStackTrace();
        System.err.println("Failed to connect to " + targetURL + ": " + e.getMessage());
        System.err.println("This may be due to your firewall, or anti-virus software.");
        System.err.println(
            "Please ensure that the Java Virtual Machine is able to access the internet.");
        return null;
      }

      if (connection.getResponseCode() != 200) {
        connection.disconnect();
        return null; // As per https://api.hypixel.net/#tag/SkyBlock/paths/~1skyblock~1auctions/get, all responses other than 200 indicate failure.
      }

      BufferedReader responseStreamReader;
      if (connection.getContentEncoding().equalsIgnoreCase("gzip")) {
        responseStreamReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
            connection.getInputStream())));
      } else {
        responseStreamReader = new BufferedReader(
            new InputStreamReader(connection.getInputStream()));
      }

      StringBuilder response = new StringBuilder();

      int ch;
      while ((ch = responseStreamReader.read()) != -1) {
        response.append((char) ch);
      }

      responseStreamReader.close();
      connection.disconnect();

      LazyObject responseJsonObject = new LazyObject(response.toString());
      if (!responseJsonObject.getBoolean("success")) {
        return null;
      }

      long timeLastUpdated = responseJsonObject.getLong("lastUpdated");

      if (timeLastUpdated > this.timeLastUpdated.get()) {
        this.timeLastUpdated.set(timeLastUpdated);
        this.totalPages.set(responseJsonObject.getInt("totalPages"));
        this.totalAuctions.set(responseJsonObject.getInt("totalAuctions"));
      }

      return responseJsonObject.getJSONArray("auctions");
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    return null;
  }

  public void printLoadingBar(int current, int max) {
    synchronized (lock) {
      float progress = (float) current / (float) max;

      StringBuilder output = new StringBuilder("\r[");

      int segments = (int) Math.floor(progress * Config.LOADING_BAR_SEGMENTS);
      for (int i = 0; i < Config.LOADING_BAR_SEGMENTS; i++) {
        if (i < segments - 1) {
          output.append('=');
        } else if (i == segments - 1) {
          output.append('>');
        } else {
          output.append(' ');
        }
      }

      output.append("] ")
          .append((int) (Math.ceil(progress * 100.0F)))
          .append("% (").append(current)
          .append("/").append(max).append(")");

      System.out.print(output);
    }
  }

  public void clearLoadingBar() {
    synchronized (lock) {
      StringBuilder output = new StringBuilder("\r ");
      for (int i = 0; i < Config.LOADING_BAR_SEGMENTS; i++) {
        output.append(' ');
      }
      output.append("                 \r");

      System.out.print(output);
    }
  }

}
