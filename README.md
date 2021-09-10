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
to control the number of threads used by the program, which by default is the number of processors that the JVM has available.

### Step 3
When a snipe has been found, the command to view the auction will be copied to your clipboard. Paste this into your chat in game, and purchase the item. Be careful of market manipulation.

## Configurability

Currently, other than the number of threads the sniper will use, there is no support for configuring the sniper outside compiling the project with your changes.
Almost all aspects of the sniper that I thought should be configurable can be modified in the ``me.vikame.binsnipe.Config`` class, and all of which should be documented.

If you wish to modify the sniper, you can do so by downloading the source code from https://github.com/Vikame/SkyblockBINSniper/releases