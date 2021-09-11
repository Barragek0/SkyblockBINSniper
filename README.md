# Skyblock BIN Sniper

A (hopefully) easy to use Skyblock BIN Sniper made in Java.

By default, the sniper will ignore commonly market-manipulated items such as Furniture, or Skins.
It will also ignore items worth less than 1,000,000 Coins, or items with 2 or fewer results on the Auction House.

For the sniper to consider an item "flippable," by default, the flip must result in *either* a 10% ROI, or a 1,000,000 Coin profit, but not both.

Currently, I am only supporting Windows, but that may change.

Heavily inspired by 'csjh' and their SkyblockSniper python script (https://github.com/csjh/SkyblockSniper)

## Usage

### Step 1
Visit https://github.com/Vikame/SkyblockBINSniper/releases and download the latest .jar file

### Step 2
Double-click the JAR to run the program. It will open a command prompt with the sniper output.

You may also run the JAR file via the command line using ``java -jar <PATH_TO_JAR> [thread_count]``
to control the number of threads used by the program, which by default is the number of processors
that the JVM has available.

### Step 3

When a snipe has been found, the command to view the auction will be copied to your clipboard. Paste
this into your chat in game, and purchase the item. Be careful of market manipulation.

## Configurability

All configuration data for the sniper can be found in the same folder you placed the JAR. All core
aspects of the sniper can be modified via this file, including profit margins, whether you wish to
filter skins or furniture, and more. The two main values you should be interested in
are ``MIN_PROFIT_AMOUNT`` and ``MIN_PROFIT_PERCENTAGE``.

The default values within this file are my personal recommendations. If you do not know what a
setting does, you can consult the documentation within ``me.vikame.binsnipe.Config`` or simply leave
it alone.

## Other

Due to the sheer size of the JSON response from the Hypixel API, any auction sniper will put quite a
load on your network connection, and my sniper is no different. If the sniper consumes *too* much of
your overall bandwith, you can consider changing the ``POOLED_THREAD_COUNT`` to be lower within your
sniper configuration, which will slow down the overall speed of the sniper but should reduce the
load on your network.

If the sniper is still too much of a load, you should make
sure ``USE_GZIP_COMPRESSION_ON_API_REQUESTS`` is set to ``true`` and
that ``FORCE_NO_CACHE_API_REQUESTS`` is set to ``false`` within your sniper configuration.

If you wish to modify the sniper, you can do so by downloading the source code
from https://github.com/Vikame/SkyblockBINSniper/releases