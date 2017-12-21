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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

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
      Contract contract, com.ib.controller.Bar bar);

  boolean shouldBuy(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  boolean shouldSell(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getExitPrice(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getEquilibratePrice(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getEntryPrice(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  boolean shouldEquilibrate(AssetManager am,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData);

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
            historicalData.put(LocalDate.ofEpochDay(bar.time()), bar);
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

  public void exportHistoricalData(String[] columns, TreeMap<LocalDate, Bar> marketData,
      String fileName) throws IOException {

    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("date," + String.join(",", columns) + "\n");
    for (Entry<LocalDate, Bar> barEntry : marketData.entrySet()) {
      Bar bar = barEntry.getValue();
      stringBuilder.append(barEntry.getValue().getBar().formattedTime().split(" ")[0] + ",");
      for (String column : columns)
        stringBuilder.append(bar.getProperty(column) + ",");
      stringBuilder.append("\n");
    }

    PrintWriter printWriter = new PrintWriter(new File(fileName));
    printWriter.write(stringBuilder.toString());
    printWriter.close();
    System.out.println(String.format("Saved file: %s", fileName));
  }

  public void runSimulation(List<Contract> contracts, TradingConfig config, int years,
      int offsetDays, Strategy strategy, MarketDataReceiver marketDataReceiver) {

    System.out.println("Fetching historical data...");
    getHistoricalData(contracts, years, historicalMarketData -> {

      for (Contract contract : contracts) {
        System.out.println(contract.symbol() + " " + historicalMarketData.get(contract).firstEntry().getValue().formattedTime());
      }

      System.out.println("Simulation started!");
      AssetManager am = new AssetManager(config.getAssetUnderManagement());
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData = new HashMap<>();

      // Assuming all contracts have the same available dates
      TreeMap<LocalDate, com.ib.controller.Bar> contractHistoricalData =
          historicalMarketData.get(contracts.get(0));
      LocalDate offset = contractHistoricalData.keySet().
          toArray(new LocalDate[contractHistoricalData.size()])[offsetDays];

      for (Contract contract : contracts)
        marketData.put(contract, new TreeMap<>());

      for (LocalDate date : contractHistoricalData.tailMap(offset).keySet()) {

        // Prepare market bars
        for (Contract contract : contracts) {
          contractHistoricalData = historicalMarketData.get(contract);
          Bar bar = strategy.prepare(am, marketData, contract, contractHistoricalData.get(date));
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

        if (strategy.shouldEquilibrate(am, marketData)) {
          Contract[] activeContracts = am.getContracts();
          int split = (int) (am.getContracts().length + marketData.keySet().stream()
              .filter(contract -> marketData.get(contract).lastEntry().getValue().shouldBuy() &&
                  am.getOwnedStocks(contract) == 0).count());

          for (Contract contract : activeContracts) {
            Bar lastBar = marketData.get(contract).get(date);
            double closePrice = lastBar.getClose();
            int stocksToSell = (int) Math.floor((am.getOwnedStocks(contract) *
                closePrice)/ split / closePrice);

            double equilbratePrice = strategy.getEquilibratePrice(am, contract,
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
