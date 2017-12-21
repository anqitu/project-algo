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

interface MarketDataReceiver {
  void receive(HashMap<Contract, TreeMap<LocalDate, Bar>> marketData);
}

interface ContractMarketDataReceiver {
  void receive(TreeMap<LocalDate, com.ib.controller.Bar> historicalData);
}

interface Strategy {
  Bar prepare(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
      Contract contract, com.ib.controller.Bar bar);
  boolean shouldBuy(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar curentBar);
  boolean shouldSell(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar curentBar);
  double getExitPrice(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar curentBar);
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
      ContractMarketDataReceiver marketDataReceiver) {
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
            marketDataReceiver.receive(historicalData);
          }
        });
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

    AssetManager am = new AssetManager(config.getAssetUnderManagement());
    HashMap<Contract, TreeMap<LocalDate, Bar>> marketData = new HashMap<>();

    for (Contract contract : contracts) {
      marketData.put(contract, new TreeMap<>());
      getHistoricalData(contract, years, historicalData -> {

        LocalDate offset = historicalData.keySet().toArray(new LocalDate[historicalData.size()])[offsetDays];

        // Simulate daily bars
        for (LocalDate date: historicalData.tailMap(offset).keySet()) {
          Bar bar = strategy.prepare(am, marketData, contract, historicalData.get(date));
          marketData.get(contract).put(date, bar);

          if (strategy.shouldBuy(am, contract, marketData, bar)) {

            // Split total asset value by number of active assets
            int activeAssets = am.getContracts().length + 1;

            // Redistributed value per contract
            double redistAssetValue = am.getTotalAssetValue(marketData) / activeAssets;

            // Use the freed assets to purchase the new contract
            double freedAssets = am.getResidualAssets();

            // Execute the selling of owned stocks to redistribute assets
            for (Contract otherContract : am.getContracts()) {
              if (!otherContract.equals(contract)) {

                Bar currentBar = marketData.get(otherContract).lastEntry().getValue();
                int owned = am.getOwnedStocks(otherContract);

                // Stocks to sell = (closing * owned) - redistributed value / low
                int toSell = new Double((currentBar.getClose() * owned - redistAssetValue) /
                    currentBar.getLow()).intValue();

                // Update assets with new balance = owned - toSell
                am.setOwnedStocks(otherContract, owned - toSell);
                double gain = toSell * currentBar.getLow();
                freedAssets += gain;
              }
            }

            // simulate buy
            double buyPrice = marketData.get(contract).lastEntry().getValue().getHigh();
            int toBuy = new Double(freedAssets / buyPrice).intValue();
            am.setResidualAssets(freedAssets - buyPrice * toBuy);
            am.setOwnedStocks(contract, toBuy);

          }

          if (strategy.shouldSell(am, contract, marketData, bar)) {

            double sellPrice = strategy.getExitPrice(am, contract, marketData, bar);

            // simulate sell
            am.setResidualAssets(am.getResidualAssets() + sellPrice * am.getOwnedStocks(contract));
            am.setOwnedStocks(contract, 0);

          }

          bar.setProperty("aum", am.getTotalAssetValue(marketData));
          bar.setProperty("stocks_owned", am.getOwnedStocks(contract));
        }

        marketDataReceiver.receive(marketData);
      });
    }
  }
}
