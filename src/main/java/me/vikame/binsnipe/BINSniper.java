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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import me.doubledutch.lazyjson.LazyArray;
import me.doubledutch.lazyjson.LazyObject;
import me.vikame.binsnipe.util.AtomicPrice;
import me.vikame.binsnipe.util.ExpiringSet;
import me.vikame.binsnipe.util.SBHelper;

public class BINSniper {

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

    flipsAlreadyShown = new ExpiringSet<>(60000 * 30); // No need for a concurrent map we will only access this in a single thread.

    totalAuctions = new AtomicInteger();
    totalBins = new AtomicInteger();

    // Make an API request to get initial data before the sniper starts.
    getAuctions(0);

    Main.schedule(() -> {
      long lastUpdateTime = timeLastUpdated.get();
      if((System.currentTimeMillis()-lastUpdateTime) > 60000) {
        getAuctions(0);
        long newUpdateTime = timeLastUpdated.get();

        if (lastUpdateTime != newUpdateTime) {
          totalBins.set(0);
          binPrices.clear();

          AtomicInteger completedPages = new AtomicInteger(0);
          final int maxPages = totalPages.get();

          long start = System.currentTimeMillis();
          for(int page = 0; page < maxPages; page++){
            final int workingPage = page;
            Main.exec(() -> {
              if (workingPage < totalPages.get()) { // Ensure that this is still a valid page we need to look at.
                LazyArray auctionArray = getAuctions(workingPage);

                for(int i = 0; i < auctionArray.length(); i++) {
                  LazyObject binData = auctionArray.getJSONObject(i);
                  if (!SBHelper.isExistingBIN(binData))
                    continue;

                  String name = SBHelper.stripItemName(binData);

                  if(Config.IGNORE_FURNITURE && SBHelper.isFurniture(binData)) continue;
                  if(Config.IGNORE_COSMETICS && SBHelper.isCosmetic(binData)) continue;

                  name += " [" + binData.getString("tier") + (SBHelper.isRecombed(binData) ? ", RECOMB" : "") + "]";

                  String uuid = binData.getString("uuid");
                  int price = binData.getInt("starting_bid");

                  binPrices.computeIfAbsent(name, ign -> new AtomicPrice()).tryUpdatePrice(uuid, price);
                  totalBins.incrementAndGet();
                }
              }

              completedPages.incrementAndGet();
            });
          }

          int lastCompleted = 0;

          int completed;
          while ((completed = completedPages.get()) < maxPages) {
            if (lastCompleted != completed) {
              lastCompleted = completed;

              float progress = (float) completed / (float) maxPages;
              printLoadingBar(progress);
            }

            try {
              Thread.sleep(100);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }

          clearLoadingBar();

          Map.Entry<String, AtomicPrice> highestProfit = null;
          int maxDiff = -1;

          for (Map.Entry<String, AtomicPrice> entry : binPrices.entrySet()) {
            if(flipsAlreadyShown.contains(entry.getKey())) continue;

            AtomicPrice price = entry.getValue();
            int lowest = price.getLowestValue();
            int second = price.getSecondLowestValue();

            if (price.getTotalCount() > Config.MIN_ITEMS_ON_MARKET && second >= Config.MIN_BIN_PRICE && lowest <= Config.MAX_BIN_PRICE) {
              int secondWithTaxes = SBHelper.calculateWithTaxes(second);

              int diff = secondWithTaxes - lowest;
              float profitPercentage = ((float) secondWithTaxes / (float) lowest) * 100.0f;

              if (profitPercentage >= Config.MIN_PROFIT_PERCENTAGE || diff >= Config.MIN_PROFIT_AMOUNT) {
                if (diff > maxDiff) {
                  maxDiff = diff;
                  highestProfit = entry;
                }
              }
            }
          }

          if (highestProfit != null) {
            AtomicPrice price = highestProfit.getValue();
            flipsAlreadyShown.add(highestProfit.getKey());

            int lowest = price.getLowestValue();
            int second = price.getSecondLowestValue();

            int secondWithTaxes = SBHelper.calculateWithTaxes(second);

            int diff = secondWithTaxes - lowest;
            float profitPercentage = (((float) secondWithTaxes / (float) lowest) * 100.0f) - 100;

            System.out.println(price.getLowestKey() + " | Item: " + highestProfit.getKey()
                    + " | # BIN'd on AH: " + price.getTotalCount()
                    + " | Price: " + NumberFormat.getInstance().format(lowest)
                    + " | Second lowest: " + NumberFormat.getInstance().format(second)
                    + " | Profit (incl. taxes): " + NumberFormat.getInstance().format(diff) + " (+"
                    + profitPercentage + "%) (" + (System.currentTimeMillis()-start) + "ms)");

            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection("/viewauction " + price.getLowestKey()), null);
          } else {
            System.out.println("we were unable to find a flip after " + (System.currentTimeMillis() - timeLastUpdated.get()) + " ms.");
          }

          System.out.println();
        }
      }
    }, 0, 1000, TimeUnit.MILLISECONDS);
  }

  private LazyArray getAuctions(int page) {
    String targetURL = Constants.AUCTIONS_ENDPOINT + "?page=" + page + (Config.FORCE_NO_CACHE_API_REQUESTS ? "&_=" + System.currentTimeMillis() : "");
    URL apiURL;
    try{
      apiURL = new URL(targetURL);
    }catch(MalformedURLException e) {
      e.printStackTrace();
      System.err.println("Malformed URL '" + targetURL + "'");
      System.exit(1);
      return null;
    }

    try {
      HttpURLConnection connection = (HttpURLConnection) apiURL.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36");
      connection.setRequestProperty("Content-Type", "application/json");

      if(Config.USE_GZIP_COMPRESSION_ON_API_REQUESTS) {
        connection.setRequestProperty("Accept-Encoding", "gzip");
      }

      if(Config.FORCE_NO_CACHE_API_REQUESTS) {
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setUseCaches(false);
      }

      connection.connect();

      BufferedReader responseStreamReader;
      if(connection.getContentEncoding().equalsIgnoreCase("gzip")) {
        responseStreamReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
            connection.getInputStream())));
      } else {
        responseStreamReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      }

      StringBuilder response = new StringBuilder();

      int ch;
      while((ch = responseStreamReader.read()) != -1) response.append((char) ch);

      connection.disconnect();
      responseStreamReader.close();

      LazyObject responseJsonObject = new LazyObject(response.toString());
      if(!responseJsonObject.getBoolean("success")) return null;

      long timeLastUpdated = responseJsonObject.getLong("lastUpdated");

      if(timeLastUpdated > this.timeLastUpdated.get()) {
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

  public void printLoadingBar(float progress) {
    int segments = (int)Math.floor(progress * Config.LOADING_BAR_SEGMENTS);
    System.out.print("\r[");
    for(int i = 0; i < Config.LOADING_BAR_SEGMENTS; i++) {
      if(i < segments-1) {
        System.out.print('=');
      } else if (i == segments-1) {
        System.out.print('>');
      } else {
        System.out.print(' ');
      }
    }
    System.out.print("] " + (int)(Math.ceil(progress * 100.0F)) + "%");
  }

  public void clearLoadingBar() {
    System.out.print("\r ");
    for(int i = 0; i < Config.LOADING_BAR_SEGMENTS; i++) {
      System.out.print(' ');
    }
    System.out.print("      \r");
  }

}
