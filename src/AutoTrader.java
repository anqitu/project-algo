import com.ib.client.Contract;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

interface MarketDataReceiver {
  void receive(HashMap<Contract, TreeMap<LocalDate, Bar>> marketData);
}

interface HistoricalContractMarketDataReceiver {
  void receive(TreeMap<LocalDate, com.ib.controller.Bar> historicalData);
}

interface HistoricalMarketDataReceiver {
  void receive(HashMap<Contract, TreeMap<LocalDate, com.ib.controller.Bar>> marketData);
}

interface Strategy {

  Bar prepare(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
      Contract contract, com.ib.controller.Bar bar, LocalDate date);

  boolean shouldBuy(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  boolean shouldSell(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getExitPrice(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getSellPriceOnRebalance(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getEntryPrice(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  boolean shouldRebalance(AssetManager am,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split);

  int getStocksToSellOnRebalance(AssetManager am, Contract contract, Bar bar,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split);

  void postProcess(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
      Contract contract, Bar bar);
}

public class AutoTrader {

  private ApiController gateway;

  public AutoTrader(String host, int port, int clientId) {
    ConnectionHandler connectionHandler = new ConnectionHandler();
    ILogger logger = s -> { };
    gateway = new ApiController(connectionHandler, logger , logger);
    gateway.connect(host, port, clientId, null);
    connectionHandler.waitForConnection();
  }

  public void shutdown() {
    gateway.disconnect();
  }

  public void getHistoricalData(Contract contract, int years,
      HistoricalContractMarketDataReceiver marketDataReceiver) {
    TreeMap<LocalDate, com.ib.controller.Bar> historicalData = new TreeMap<>();

    System.out.println(String.format("Requesting (%d) years worth of daily historical data for %s",
        years, contract.symbol()));
    gateway.reqHistoricalData(contract, "", years, DurationUnit.YEAR, BarSize._1_day,
        WhatToShow.ADJUSTED_LAST, true, false, new IHistoricalDataHandler() {

          @Override
          public void historicalData(com.ib.controller.Bar bar) {
            historicalData.put(LocalDate.parse(bar.formattedTime(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), bar);
          }

          @Override
          public void historicalDataEnd() {
            System.out.println("Received data for " + contract.symbol());
            marketDataReceiver.receive(historicalData);
          }
        });
  }

  // SYNC
//  public void getHistoricalData(List<Contract> contracts, int years,
//      HistoricalMarketDataReceiver marketDataReceiver) {
//    HashMap<Contract, TreeMap<LocalDate, com.ib.controller.Bar>> marketData = new HashMap<>();
//    Semaphore semaphore = new Semaphore(0);
//    for (Contract contract : contracts) {
//      getHistoricalData(contract, years, historicalData -> {
//        semaphore.release(1);
//        marketData.put(contract, historicalData);
//        if (marketData.size() == contracts.size()) {
//          semaphore.drainPermits();
//          marketDataReceiver.receive(marketData);
//        }
//      });
//
//      // wait for release
//      try { semaphore.acquire(); } catch (InterruptedException ex) { }
//    }
//  }

  public void getHistoricalData(List<Contract> contracts, int years,
      HistoricalMarketDataReceiver marketDataReceiver) {
    HashMap<Contract, TreeMap<LocalDate, com.ib.controller.Bar>> marketData = new HashMap<>();
    for (Contract contract : contracts) {
      getHistoricalData(contract, years, historicalData -> {
        marketData.put(contract, historicalData);
        if (marketData.size() == contracts.size())
          marketDataReceiver.receive(marketData);
      });
    }
  }

  public void exportData(TreeMap<LocalDate, Bar> marketData, String fileName,
      String[] columns) throws IOException {

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("date," + String.join(",", columns) + "\n");
    for (Entry<LocalDate, Bar> barEntry : marketData.entrySet()) {
      Bar bar = barEntry.getValue();
      stringBuilder.append(bar.getBar().formattedTime().split(" ")[0] + ",");
      for (String column : columns)
        stringBuilder.append(bar.getProperty(column) + ",");
      stringBuilder.append("\n");
    }

    PrintWriter printWriter = new PrintWriter(new File(fileName));
    printWriter.write(stringBuilder.toString());
    printWriter.close();
    System.out.println(String.format("Saved file: %s", fileName));
  }

  public void exportData(TreeMap<LocalDate, com.ib.controller.Bar> marketData, String fileName)
      throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(String.join(",",
        new String[] {"date", "open", "high", "low", "close"}) + "\n");
    for (Entry<LocalDate, com.ib.controller.Bar> barEntry : marketData.entrySet()) {
      com.ib.controller.Bar bar = barEntry.getValue();
      stringBuilder.append(String.join(",",
          new String[] {bar.formattedTime().split(" ")[0],
              String.valueOf(bar.open()),
              String.valueOf(bar.high()), String.valueOf(bar.low()),
              String.valueOf(bar.close())}) + "\n");
    }

    PrintWriter printWriter = new PrintWriter(new File(fileName));
    printWriter.write(stringBuilder.toString());
    printWriter.close();
    System.out.println(String.format("Saved file: %s", fileName));
  }

  public void runSimulation(List<Contract> contracts, TradingConfig config, int years,
      Strategy strategy, MarketDataReceiver marketDataReceiver) {

    System.out.println("Fetching historical data...");
    getHistoricalData(contracts, years, historicalMarketData -> {

      System.out.println("Simulation started!");
      AssetManager am = new AssetManager(config.getAssetUnderManagement());
      TreeSet<LocalDate> dates = new TreeSet<>();
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData = new HashMap<>();

      for (Contract contract : contracts) {
        marketData.put(contract, new TreeMap<>());
        dates.addAll(historicalMarketData.get(contract).keySet());
      }

      for (LocalDate date : dates) {

        // Prepare market bars
        for (Contract contract : contracts) {
          Bar bar = strategy.prepare(am, marketData, contract,
              historicalMarketData.get(contract).get(date), date);
          marketData.get(contract).put(date, bar);
        }

        // Determine action and execute sell if needed
        for (Contract contract : contracts) {
          Bar bar = marketData.get(contract).get(date);
          bar.setShouldBuy(strategy.shouldBuy(am, contract, marketData, bar));
          bar.setShouldSell(strategy.shouldSell(am, contract, marketData, bar));

          if (bar.shouldSell()) {
            double sellPrice = strategy.getExitPrice(am, contract, marketData, bar);
            am.setResidualAssets(am.getResidualAssets() + sellPrice * am.getOwnedStocks(contract));
            am.setOwnedStocks(contract, 0);
          }
        }

        int split = (int) (am.getContracts().length + marketData.keySet().stream()
            .filter(contract -> marketData.get(contract).lastEntry().getValue().shouldBuy() &&
                am.getOwnedStocks(contract) == 0).count());

        // Rebalance
        if (strategy.shouldRebalance(am, marketData, split)) {
          Contract[] activeContracts = am.getContracts();

          for (Contract contract : activeContracts) {
            Bar lastBar = marketData.get(contract).get(date);
            int stocksToSell = strategy.getStocksToSellOnRebalance(am, contract, lastBar,
                marketData, split);

            double equilbratePrice = strategy.getSellPriceOnRebalance(am, contract,
                marketData, lastBar);
            am.setResidualAssets(am.getResidualAssets() + equilbratePrice * stocksToSell);
            am.setOwnedStocks(contract, am.getOwnedStocks(contract) - stocksToSell);
          }
        }

        // Execute buy
        Contract[] contractsToBuy = contracts.stream().filter(contract ->
            marketData.get(contract).get(date).shouldBuy()).toArray(Contract[]::new);
        int contractsBought = 0;
        for (Contract contract : contractsToBuy) {
          Bar bar = marketData.get(contract).get(date);
          double buyPrice = strategy.getEntryPrice(am, contract, marketData, bar);
          int stocksToBuy = (int) Math.floor((am.getResidualAssets()/
              (contractsToBuy.length - contractsBought++))/buyPrice);

          am.setResidualAssets(am.getResidualAssets() - buyPrice * stocksToBuy);
          am.setOwnedStocks(contract, stocksToBuy);
        }

        // Post process bars
        for (Contract contract : contracts)
          strategy.postProcess(am, marketData, contract, marketData.get(contract).get(date));

      }

      marketDataReceiver.receive(marketData);
    });
  }
}
