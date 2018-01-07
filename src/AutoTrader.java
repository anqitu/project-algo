import com.ib.client.ContractDetails;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IContractDetailsHandler;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

interface MarketDataReceiver {
  void receive(HashMap<Contract, TreeMap<LocalDate, Bar>> marketData);
}

interface HistoricalContractMarketDataReceiver {
  void receive(TreeMap<LocalDate, Bar> historicalData);
}

interface HistoricalMarketDataReceiver {
  void receive(HashMap<Contract, TreeMap<LocalDate, Bar>> marketData);
}

interface ContractDetailsReceiver {
  void receive(ContractDetails contractDetails);
}

interface ContractsDetailsReceiver {
  void receive(ArrayList<Contract> contracts);
}



interface Strategy {

  Bar prepare(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
      Contract contract, Bar bar, LocalDate date);

  boolean shouldBuy(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  boolean shouldSell(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getExitPrice(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getSellPriceOnRebalance(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  double getRequiredTotalCashOnRebalance(AssetManager am,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split);

  double getEntryPrice(AssetManager am, Contract contract,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar);

  boolean shouldRebalance(AssetManager am,
      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split);

  int getStocksToSellOnRebalance(Bar bar, double requiredIndividualCash);

  void postProcess(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
      Contract contract, Bar bar);
}

public class AutoTrader {

  private ApiController gateway;
  private DataBaseHandler dataBaseHandler;

  public AutoTrader(DataBaseHandler dataBaseHandler, String host, int port, int clientId) {
    ConnectionHandler connectionHandler = new ConnectionHandler();
    ILogger logger = s -> { };
    gateway = new ApiController(connectionHandler, logger , logger);
    gateway.connect(host, port, clientId, null);
    connectionHandler.waitForConnection();
    this.dataBaseHandler = dataBaseHandler;
  }

  public AutoTrader(DataBaseHandler dataBaseHandler) {
    this.dataBaseHandler = dataBaseHandler;
  }

  public DataBaseHandler getDataBaseHandler() {
    return dataBaseHandler;
  }

  public void shutdown() {
    gateway.disconnect();
  }

  public void getHistoricalDataFromIB(Contract contract, int years,
    HistoricalContractMarketDataReceiver marketDataReceiver) {
    TreeMap<LocalDate, Bar> historicalData = new TreeMap<>();
//    ArrayList<Bar> historicalData = new ArrayList<>();
    System.out.println(String.format("Requesting (%d) years worth of daily historical data for %s",
        years, contract.getSymbol()));
    gateway.reqHistoricalData(contract.getRawContract(), "", years, DurationUnit.YEAR, BarSize._1_day,
        WhatToShow.ADJUSTED_LAST, true, false, new IHistoricalDataHandler() {

          @Override
          public void historicalData(com.ib.controller.Bar rawBar) {
            Bar bar = new Bar(rawBar, contract);
            historicalData.put(bar.getDate(), bar);
          }

          @Override
          public void historicalDataEnd() {
            System.out.println("Received data for " + contract.getSymbol());
            marketDataReceiver.receive(historicalData);
          }
        });
  }


  // SYNC
//  public void getHistoricalDataFromIB(List<Contract> contracts, int years,
//      HistoricalMarketDataReceiver marketDataReceiver) {
//    HashMap<Contract, TreeMap<LocalDate, com.ib.controller.Bar>> marketData = new HashMap<>();
//    Semaphore semaphore = new Semaphore(0);
//    for (Contract contract : contracts) {
//      getHistoricalDataFromIB(contract, years, historicalData -> {
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

  public void getHistoricalDataFromIB(List<Contract> contracts, int years,
      HistoricalMarketDataReceiver marketDataReceiver) {
    HashMap<Contract, TreeMap<LocalDate, Bar>> marketData = new HashMap<>();
    for (Contract contract : contracts) {
      getHistoricalDataFromIB(contract, years, historicalData -> {
        marketData.put(contract, historicalData);
        if (marketData.size() == contracts.size())
          marketDataReceiver.receive(marketData);
      });
    }
  }

  // Export data to CSV
  public void exportData(Collection<ExportableToCSV> data, String fileName, List<String> headers)
      throws IOException {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(String.join(",", headers)+ "\n" );
    for (ExportableToCSV exportable : data)
      stringBuilder.append(exportable.toCSVLine(headers));
    PrintWriter printWriter = new PrintWriter(new File(fileName));
    printWriter.write(stringBuilder.toString());
    printWriter.close();
    System.out.println(String.format("Saved file: %s", fileName));
  }

  // Export bar data to database
  public void exportHistoricalData(int years, int contractIndexStart, int contractIndexEnd){
    ArrayList<Contract> contractsFromContractTable = dataBaseHandler.readContractDataFromContractTable();
    ArrayList<Contract> contractsFromBarTable = dataBaseHandler.readContractDataFromBarTable();
    if (contractIndexStart == contractIndexEnd) return;
    if (contractsFromBarTable.contains(contractsFromContractTable.get(contractIndexStart))) {
      System.out.println(contractIndexStart);
      System.out.println(contractsFromContractTable.get(contractIndexStart).getSymbol());
      exportHistoricalData(years, contractIndexStart+1, contractIndexEnd);
      return;
    }
    TreeMap<LocalDate, Bar> historicalData = new TreeMap<>();
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Date date = new Date();
    System.out.println(dateFormat.format(date));
    System.out.println(contractIndexStart);
    System.out.println(String.format("Requesting (%d) years worth of daily historical data for %s",
        years, contractsFromContractTable.get(contractIndexStart).getSymbol()));
    System.out.println();
    gateway.reqHistoricalData(contractsFromContractTable.get(contractIndexStart).getRawContract(), "", years, DurationUnit.YEAR, BarSize._1_day,
        WhatToShow.ADJUSTED_LAST, true, false, new IHistoricalDataHandler() {

          @Override
          public void historicalData(com.ib.controller.Bar rawBar) {
            Bar bar = new Bar(rawBar, contractsFromContractTable.get(contractIndexStart));
            historicalData.put(bar.getDate(), bar);
          }

          @Override
          public void historicalDataEnd() {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            System.out.println(dateFormat.format(date));
            System.out.println("Received data for " + contractsFromContractTable.get(contractIndexStart).getSymbol());
            dataBaseHandler.insertData(historicalData.values().stream().map(bar1 ->
                (ExportableToDB) bar1).collect(Collectors.toCollection(ArrayList::new)));
            System.out.println();
            try
            {
              Thread.sleep(7000);
            }
            catch(InterruptedException ex)
            {
              Thread.currentThread().interrupt();
            }
            exportHistoricalData(years, contractIndexStart+1, contractIndexEnd);
          }
        });
  }


//
//  public void runSimulationWithIB(List<Contract> contracts, TradingConfig config, int years,
//      Strategy strategy, MarketDataReceiver marketDataReceiver) {
//
//    System.out.println("Fetching historical data...");
//    getHistoricalDataFromIB(contracts, years, historicalMarketData -> {
//
//      System.out.println("Simulation started!");
//      AssetManager am = new AssetManager(config.getAssetUnderManagement());
//      TreeSet<LocalDate> dates = new TreeSet<>();
//      HashMap<Contract, TreeMap<LocalDate, Bar>> marketData = new HashMap<>();
//
//      for (Contract contract : contracts) {
//        marketData.put(contract, new TreeMap<>());
//        dates.addAll(historicalMarketData.get(contract).keySet());
//      }
//
//      for (LocalDate date : dates) {
//
//        // Prepare market bars
//        for (Contract contract : contracts) {
//          Bar bar = strategy.prepare(am, marketData, contract, historicalMarketData.get(contract).get(date));
//          marketData.get(contract).put(date, bar);
//        }
//
//        // Determine action and execute sell first if needed
//        for (Contract contract : contracts) {
//          Bar bar = marketData.get(contract).get(date);
//          bar.setShouldBuy(strategy.shouldBuy(am, contract, marketData, bar));
//          bar.setShouldSell(strategy.shouldSell(am, contract, marketData, bar));
//
//          if (bar.shouldSell()) {
//            double sellPrice = strategy.getExitPrice(am, contract, marketData, bar);
//            am.setResidualAssets(am.getResidualAssets() + sellPrice * am.getOwnedStocks(contract));
//            am.setOwnedStocks(contract, 0);
//          }
//        }
//
//        // Number of contracts supposed to be owned for today =
//        // currently active contracts + newly shouldBuy contracts
//        int split = (int) (am.getContracts().length + marketData.keySet().stream()
//            .filter(contract -> marketData.get(contract).lastEntry().getValue().shouldBuy() &&
//                am.getOwnedStocks(contract) == 0).count());
//
//
//        // Rebalance
//        if (strategy.shouldRebalance(am, marketData, split)) {
//
//          double requiredTotalCash = strategy.getRequiredTotalCashOnRebalance(am, marketData, split);
//          double requiredIndividualCash = requiredTotalCash / am.getContracts().length;
//          Contract[] activeContracts = am.getContracts();
//
//          for (Contract contract : activeContracts) {
//            Bar lastBar = marketData.get(contract).get(date);
//            int stocksToSell = strategy.getStocksToSellOnRebalance(lastBar,requiredIndividualCash);
//
//            double equilbratePrice = strategy.getSellPriceOnRebalance(am, contract,
//                marketData, lastBar);
//            am.setResidualAssets(am.getResidualAssets() + equilbratePrice * stocksToSell);
//            am.setOwnedStocks(contract, am.getOwnedStocks(contract) - stocksToSell);
//          }
//        }
//
//        // Execute buy
//        Contract[] contractsToBuy = contracts.stream().filter(contract ->
//            marketData.get(contract).get(date).shouldBuy()).toArray(Contract[]::new);
//        int contractsBought = 0;
//        for (Contract contract : contractsToBuy) {
//          Bar bar = marketData.get(contract).get(date);
//          double buyPrice = strategy.getEntryPrice(am, contract, marketData, bar);
//          int stocksToBuy = (int) Math.floor((am.getResidualAssets()/
//              (contractsToBuy.length - contractsBought++))/buyPrice);
//
//          am.setResidualAssets(am.getResidualAssets() - buyPrice * stocksToBuy);
//          am.setOwnedStocks(contract, stocksToBuy);
//        }
//
//        // Post process bars
//        for (Contract contract : contracts)
//          strategy.postProcess(am, marketData, contract, marketData.get(contract).get(date));
//
//      }
//
//      marketDataReceiver.receive(marketData);
//    });
//  }


  public void runSimulationWithDB(ArrayList<Contract> contracts, TradingConfig config, Strategy strategy) {
    ArrayList<Bar> bars = dataBaseHandler.readBarDataFromBarTable();
    HashMap<String, LocalDate> contractFirstBarDates = dataBaseHandler.getFirstBarDate();
    HashMap<String, LocalDate> contractLastBarDates = dataBaseHandler.getLastBarDate();
    TreeSet<LocalDate> dates = dataBaseHandler.getAllDates();

    System.out.println("Simulation started!");
    AssetManager am = new AssetManager(config.getAssetUnderManagement());
    HashMap<Contract, TreeMap<LocalDate, Bar>> historicalData = new HashMap<>();
    HashMap<Contract, TreeMap<LocalDate, Bar>> simulatedData = new HashMap<>();

    for (Contract contract : contracts) {
      historicalData.put(contract, new TreeMap<>());
      for (Bar bar : bars){
        if (bar.getSymbol().equals(contract.getSymbol())){
          historicalData.get(contract).put(bar.getDate(), bar);
        }
      }
    }

    for (LocalDate date : dates) {

      System.out.println(date.toString());

      ArrayList<Contract> contractsAvailable = new ArrayList<>();
      for (Contract contract : contracts) {
        if (!contractFirstBarDates.get(contract.getSymbol()).isAfter(date) &&
            !contractLastBarDates.get(contract.getSymbol()).isBefore(date))
          contractsAvailable.add(contract);
      }

      // Prepare market bars if the contract is available on the market for this date
      for (Contract contract : contractsAvailable) {
        if (!simulatedData.containsKey(contract)) simulatedData.put(contract, new TreeMap<>());
        Bar bar = strategy.prepare(am, simulatedData, contract, historicalData.get(contract).get(date), date);
        simulatedData.get(contract).put(date, bar);

        // Remove bar to clear space
        if (historicalData.get(contract).get(date) != null) historicalData.get(contract).remove(date);
      }

      // Determine action and execute sell first if needed
      for (Contract contract : contractsAvailable) {
        Bar bar = simulatedData.get(contract).get(date);

        // Sell the stock on the day of last bar
        if (historicalData.get(contract).size() == 0){
          bar.setShouldSell(true);
          bar.setShouldBuy(false);
        } else {
          bar.setShouldBuy(strategy.shouldBuy(am, contract, simulatedData, bar));
          bar.setShouldSell(strategy.shouldSell(am, contract, simulatedData, bar));
        }

        if (bar.shouldSell()) {
          double sellPrice = strategy.getExitPrice(am, contract, simulatedData, bar);
          am.setResidualAssets(am.getResidualAssets() + sellPrice * am.getOwnedStocks(contract));
          am.setOwnedStocks(contract, 0);
        }
      }

      // Number of contracts supposed to be owned for today =
      // currently active contracts + newly shouldBuy contracts
      int split = (int) (am.getContracts().length + contractsAvailable.stream()
          .filter(contract -> simulatedData.get(contract).lastEntry().getValue().shouldBuy() &&
              am.getOwnedStocks(contract) == 0).count());


      // Rebalance
      if (strategy.shouldRebalance(am, simulatedData, split)) {

        double requiredTotalCash = strategy.getRequiredTotalCashOnRebalance(am, simulatedData, split);
        double requiredIndividualCash = requiredTotalCash / am.getContracts().length;
        Contract[] activeContracts = am.getContracts();

        for (Contract contract : activeContracts) {
          Bar lastBar = simulatedData.get(contract).get(date);
          int stocksToSell = strategy.getStocksToSellOnRebalance(lastBar,requiredIndividualCash);

          double equilbratePrice = strategy.getSellPriceOnRebalance(am, contract,
              simulatedData, lastBar);
          am.setResidualAssets(am.getResidualAssets() + equilbratePrice * stocksToSell);
          am.setOwnedStocks(contract, am.getOwnedStocks(contract) - stocksToSell);
        }
      }

      // Execute buy
      Contract[] contractsToBuy = contractsAvailable.stream().filter(contract ->
          simulatedData.get(contract).get(date).shouldBuy()).toArray(Contract[]::new);
      int contractsBought = 0;
      for (Contract contract : contractsToBuy) {
        Bar bar = simulatedData.get(contract).get(date);
        double buyPrice = strategy.getEntryPrice(am, contract, simulatedData, bar);
        int stocksToBuy = (int) Math.floor((am.getResidualAssets()/
            (contractsToBuy.length - contractsBought++))/buyPrice);

        am.setResidualAssets(am.getResidualAssets() - buyPrice * stocksToBuy);
        am.setOwnedStocks(contract, stocksToBuy);
      }

      // Post process bars
      for (Contract contract : contractsAvailable)
        strategy.postProcess(am, simulatedData, contract, simulatedData.get(contract).get(date));

      System.out.println(simulatedData.get(contracts.get(0)).lastEntry().getValue().getProperty("aum"));
      }

      // Export simulated data to csv
    for (Contract contract : contracts)
      try {
        exportData(simulatedData.get(contract).entrySet().stream().
                map(Map.Entry::getValue).collect(Collectors.toCollection(LinkedHashSet::new)),
            contract.getSymbol().toString() + ".csv",
            Arrays.asList("date", "open", "high", "low", "close", "20dma", "50dma", "200dma",
                "criteria_1", "criteria_2", "criteria_3", "criteria_4", "all_4", "shouldBuy",
                "position", "entry_price", "initial_stop_loss", "trailing_stop_loss",
                "stop_out", "exit_price", "stocks_owned", "stock_value", "cash", "aum"));
      } catch (IOException e) {
        e.printStackTrace();
      }
  }

  public void getContractDetails (Contract contract, ContractDetailsReceiver contractDetailsReceiver ){

      gateway.reqContractDetails(contract.getRawContract(), new IContractDetailsHandler() {
        @Override
        public void contractDetails(List<ContractDetails> list) {
          contractDetailsReceiver.receive(list.size() == 1? list.get(0) : null);
        }
      });
  }

  // Check whether the contract is already in the database first
  public void getContractsDetails (List<Contract> contractsInList, ContractsDetailsReceiver contractsDetailsReceiver) {
    ArrayList<Contract> contractsFromDB = dataBaseHandler.readContractDataFromContractTable();
    ArrayList<Contract> contractsWIthDetails = new ArrayList<>();
    ArrayList<Contract> contractsNotInDB = new ArrayList<>();
    for (Contract contractInList : contractsInList) {
      if (!contractsFromDB.contains(contractInList)){
        System.out.println(contractInList.getSymbol());
        contractsNotInDB.add(contractInList);
      }
    }
    for (Contract contractNotInDB : contractsNotInDB){
      getContractDetails(contractNotInDB, new ContractDetailsReceiver() {
        @Override
        public void receive(ContractDetails contractDetails) {
          Contract contractWithDetails = contractNotInDB;
          contractWithDetails.setContractDetails(contractDetails);
          if(!contractsWIthDetails.contains(contractWithDetails)){
            contractsWIthDetails.add(contractWithDetails);
            if (contractsWIthDetails.size() == contractsNotInDB.size())
              contractsDetailsReceiver.receive(contractsWIthDetails);
          }
        }
      });
    }
  }
}
