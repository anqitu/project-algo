import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

public class AutoTraderApp {

  private static final String host = "localhost";
  private static final int port = 4002;
  private static final int clientId = 1;

  interface Condition {
    boolean fulfills(TreeMap<LocalDate, Bar> contractMarketData, Bar bar);
  }


  public static void main(String[] args) {

    DataBaseHandler dataBaseHandler = new DataBaseHandler();
//    AutoTrader autoTrader = new AutoTrader(dataBaseHandler, host, port, clientId);
    AutoTrader autoTrader = new AutoTrader(dataBaseHandler);
//    autoTrader.run();


    // Read Contract information from SPX LIST
//    ArrayList<Contract> contractsInList = new ArrayList<>();
//    String spxFile = "spx900.txt";
//    Path spxFilePath = FileSystems.getDefault().getPath(spxFile);
//    try {
//      contractsInList.addAll(Files.lines(spxFilePath, StandardCharsets.UTF_8).map(Contract::new)
//          .collect(Collectors.toCollection(ArrayList::new)));
//    } catch (IOException e) { e.printStackTrace(); }


//     Read contracts from symbols
//    contractsInList.addAll(Stream.of("WYE").map(Contract::new).collect(
//        Collectors.toCollection(ArrayList::new)));


    // Read Contract information from algoDataBase
//    ArrayList<Contract> contractsFromDB = dataBaseHandler.readContractDataFromContractTable();
    ArrayList<Contract> contracts = dataBaseHandler.readContractDataFromBarTable();


////    EXPORT HISTORICAL DATA TO CSV
//    autoTrader.getHistoricalDataFromIB(contracts, 10, marketData -> {
//      for (Contract contract : contracts)
//        try {
//          autoTrader.exportData(marketData.get(contract), contract.getContract().symbol() + ".csv");
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//    });

    ///////////////////////////////// EXECUTE SIMULATION ///////////////////////////////////////////




    TradingConfig tradingConfig = new TradingConfig(0.1,
        1000000);
    // Set stop loss
    for (Contract contract : contracts) {
      tradingConfig.setStopLoss(contract, 0.20);
    }


////     Export contract information to algoDataBase
//    autoTrader.getContractsDetails(contractsInList, new ContractsDetailsReceiver() {
//      @Override
//      public void receive(ArrayList<Contract> contractsWithDetails) {
//        System.out.println("Received all contract details");
////        try {
////          autoTrader.exportData(contractsWithDetails.stream().map(contract ->
////                  (ExportableToCSV) contract).collect(Collectors.toCollection(ArrayList::new)),
////              "ContractDetails.csv",
////              Arrays.asList("symbol", "secType", "companyName", "industry", "category"));
////        } catch (IOException e) {
////          e.printStackTrace();
////        }
//        dataBaseHandler.insertData(contractsWithDetails.stream().map(contract ->
//            (ExportableToDB) contract).collect(Collectors.toCollection(ArrayList::new)));
//      }
//    });


//     Export bar information to algoDataBase
//    autoTrader.exportHistoricalData(17, 0,656);

    //    autoTrader.runSimulationWithIB(contracts, tradingConfig, 17, new RunSimulationWithDBPreparedBarStrategy() {
//
//          Condition condition1 = ((contractMarketData, bar) ->
//              bar.getProperty("20dma") > bar.getProperty("50dma"));
//
//          Condition condition2 = ((contractMarketData, bar) ->
//              bar.getProperty("20dma") > bar.getProperty("200dma"));
//
//          Condition condition3 = ((contractMarketData, bar) ->
//              bar.getProperty("50dma") > bar.getProperty("200dma"));
//
//          Condition condition4 = ((contractMarketData, bar) -> {
//            Bar lastEntry = contractMarketData.lastEntry().getValue();
//            return lastEntry.getOpen() > bar.getProperty("200dma") &&
//                lastEntry.getLow() > bar.getProperty("200dma");
//          });
//
//          @Override
//          public Bar prepare(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
//              Contract contract, Bar historicalBar) {
//
//            Bar preparedBar = null;
//
//            // If today has missing data, we assume to use yesterday's data.
//            if (historicalBar == null) {
//              preparedBar = marketData.get(contract).lastEntry().getValue().copy();
//              System.out.println("Missing: " + contract.getSymbol() + " " + preparedBar.getDate());
//            } else {
//              preparedBar = historicalBar;
//            }
//
//            TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);
//            int currentSize = contractMarketData.size();
//            if (currentSize >= 199) {
//
//              LocalDate[] contractMarketDates =
//                  contractMarketData.keySet().toArray(new LocalDate[currentSize]);
//              LocalDate dateAgo200 = contractMarketDates[currentSize - 199];
//              LocalDate dateAgo50 = contractMarketDates[currentSize - 49];
//              LocalDate dateAgo20 = contractMarketDates[currentSize - 19];
//
//              SortedMap<LocalDate, Bar> bars20 = marketData.get(contract).tailMap(dateAgo20,
//                  true);
//              double movingSum20 = bars20.entrySet().stream().mapToDouble(barEntry ->
//                  barEntry.getValue().getClose()).sum() + preparedBar.getClose();
//              preparedBar.setProperty("20dma", movingSum20 / 20);
//
//              SortedMap<LocalDate, Bar> bars50 = marketData.get(contract).tailMap(dateAgo50,
//                  true);
//              double movingSum50 = bars50.entrySet().stream().mapToDouble(barEntry ->
//                  barEntry.getValue().getClose()).sum() + preparedBar.getClose();;
//              preparedBar.setProperty("50dma", movingSum50 / 50);
//
//              SortedMap<LocalDate, Bar> bars200 = marketData.get(contract).tailMap(dateAgo200,
//                  true);
//              double movingSum200 = bars200.entrySet().stream().mapToDouble(barEntry ->
//                  barEntry.getValue().getClose()).sum() + preparedBar.getClose();;
//              preparedBar.setProperty("200dma", movingSum200 / 200);
//
//              String[] criteria = new String[]{"criteria_1", "criteria_2", "criteria_3",
//                  "criteria_4"};
//              preparedBar.setProperty(criteria[0], condition1.fulfills(contractMarketData, preparedBar) ? 1 : 0);
//              preparedBar.setProperty(criteria[1], condition2.fulfills(contractMarketData, preparedBar) ? 1 : 0);
//              preparedBar.setProperty(criteria[2], condition3.fulfills(contractMarketData, preparedBar) ? 1 : 0);
//              preparedBar.setProperty(criteria[3], condition4.fulfills(contractMarketData, preparedBar) ? 1 : 0);
//              preparedBar.setProperty("all_4", Arrays.stream(criteria).mapToDouble(preparedBar::getProperty).sum()
//                  == 4 ? 1 : 0);
//
//            }
//            return preparedBar;
//          }
//
//          @Override
//          public boolean shouldBuy(AssetManager am, Contract contract,
//              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//
//            TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);
//
//            if (contractMarketData.size() > 1) {
//              Bar previous = contractMarketData.lowerEntry(bar.getDate()).getValue();
//
//              boolean previousPosition = previous.getProperty("position") == 1;
//              boolean shouldBuy = !previousPosition && previous.getProperty("all_4") == 0 &&
//                  bar.getProperty("all_4") == 1;
//              bar.setProperty("position", shouldBuy ? 1 :
//                  (previousPosition && previous.getProperty("stop_out") == 0 ? 1 : 0));
//
//              if (previousPosition && bar.getProperty("position") ==1) {
//                bar.setProperty("entry_price", previous.getProperty("entry_price"));
//                bar.setProperty("initial_stop_loss", previous.getProperty("initial_stop_loss"));
//                bar.setProperty("trailing_stop_loss",
//                    Math.max(previous.getProperty("trailing_stop_loss"),
//                        (1 - tradingConfig.getStopLoss(contract)) * bar.getClose()));
//              }
//
//              if (shouldBuy) {
//                bar.setProperty("initial_stop_loss",
//                    bar.getHigh() * (1 - tradingConfig.getStopLoss(contract)));
//                bar.setProperty("trailing_stop_loss", bar.getProperty("initial_stop_loss"));
//              }
//
//              return shouldBuy;
//            }
//
//            return false;
//          }
//
//          @Override
//          public boolean shouldSell(AssetManager am, Contract contract,
//              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//            boolean shouldSell = bar.getProperty("position") == 1 &&
//                bar.getLow() < bar.getProperty("trailing_stop_loss");
//            bar.setProperty("stop_out", shouldSell ? 1 : 0);
//            return shouldSell;
//          }
//
//
//          @Override
//          // Need to rebalance when residual cash < AUM / split * (split - currentlyOwnedStocks)
//          public boolean shouldRebalance(AssetManager am,
//              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split) {
//            return am.getResidualAssets() <
//                am.getTotalAssetValue(marketData) / split * (split - am.getContracts().length);
//          }
//
//          @Override
//          public double getRequiredTotalCashOnRebalance(AssetManager am,
//              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split){
//            return am.getTotalAssetValue(marketData) / split * (split - am.getContracts().length) -
//                am.getResidualAssets();
//          }
//
//          @Override
//          public double getSellPriceOnRebalance(AssetManager am, Contract contract,
//              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//            return bar.getLow();
//          }
//
//          @Override
//          // StocksToSellOnRebalance = requiredIndividualCash / low
//          public int getStocksToSellOnRebalance(Bar bar, double requiredIndividualCash) {
//            return (int) Math.floor((requiredIndividualCash / bar.getLow()));
//          }
//
//          @Override
//          public double getEntryPrice(AssetManager am, Contract contract,
//              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//            double entryPrice = bar.getHigh();
//            bar.setProperty("entry_price", entryPrice);
//            return entryPrice;
//          }
//
//          @Override
//          public double getExitPrice(AssetManager am, Contract contract,
//              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//            double exitPrice = Math.min(Math.min(bar.getLow(),
//                bar.getProperty("trailing_stop_loss")), bar.getOpen());
//            bar.setProperty("exit_price", exitPrice);
//            return exitPrice;
//          }
//
//          @Override
//          public void postProcess(AssetManager am,
//              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
//              Contract contract, Bar bar) {
//            bar.setProperty("stocks_owned", am.getOwnedStocks(contract));
//            bar.setProperty("stock_value", am.getOwnedStocks(contract) * bar.getClose());
//            bar.setProperty("cash", am.getResidualAssets());
//            bar.setProperty("aum", am.getTotalAssetValue(marketData));
//          }
//
//        }, marketData -> {
//          for (Contract contract : contracts)
//            try {
//              autoTrader.exportData(marketData.get(contract).entrySet().stream().
//                      map(Entry::getValue).collect(Collectors.toCollection(LinkedHashSet::new)),
//                  contract.getSymbol().toString() + ".csv",
//                  Arrays.asList("date", "open", "high", "low", "close", "20dma", "50dma", "200dma",
//                      "criteria_1", "criteria_2", "criteria_3", "criteria_4", "all_4", "shouldBuy",
//                      "position", "entry_price", "initial_stop_loss", "trailing_stop_loss",
//                      "stop_out", "exit_price", "stocks_owned", "stock_value", "cash", "aum"));
//            } catch (IOException e) {
//              e.printStackTrace();
//            }
//        });

    System.out.println(contracts.size());


    ArrayList<Contract> contracts1 = new ArrayList<>(contracts.subList(0,1));

    autoTrader.prepareBarPropertyWithDB(contracts1, tradingConfig, new PrepareBarPropertyWithDBStrategy() {

          Condition condition1 = ((contractMarketData, bar) ->
              bar.getProperty("20dma") > bar.getProperty("50dma"));

          Condition condition2 = ((contractMarketData, bar) ->
              bar.getProperty("20dma") > bar.getProperty("200dma"));

          Condition condition3 = ((contractMarketData, bar) ->
              bar.getProperty("50dma") > bar.getProperty("200dma"));

          Condition condition4 = ((contractMarketData, bar) -> {
            Bar lastEntry = contractMarketData.lastEntry().getValue();
            return lastEntry.getOpen() > bar.getProperty("200dma") &&
                lastEntry.getLow() > bar.getProperty("200dma");
          });

          @Override
          public Bar prepare(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
              Contract contract, Bar historicalBar, LocalDate date, ArrayList<Bar> missingBars) {

            Bar preparedBar = null;

            // If today has missing data, we assume to use yesterday's data.
            if (historicalBar == null) {
              preparedBar = marketData.get(contract).lastEntry().getValue().copy();
              preparedBar.setDate(date);
              System.out.println("Missing: " + contract.getSymbol() + " " + preparedBar.getDate());
              missingBars.add(preparedBar);
            } else {
              preparedBar = historicalBar.copy();
            }

            TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);
            int currentSize = contractMarketData.size();
            if (currentSize >= 199) {

              LocalDate[] contractMarketDates =
                  contractMarketData.keySet().toArray(new LocalDate[currentSize]);
              LocalDate dateAgo200 = contractMarketDates[currentSize - 199];
              LocalDate dateAgo50 = contractMarketDates[currentSize - 49];
              LocalDate dateAgo20 = contractMarketDates[currentSize - 19];

              SortedMap<LocalDate, Bar> bars20 = marketData.get(contract).tailMap(dateAgo20,
                  true);
              double movingSum20 = bars20.entrySet().stream().mapToDouble(barEntry ->
                  barEntry.getValue().getClose()).sum() + preparedBar.getClose();
              preparedBar.setProperty("20dma", movingSum20 / 20);

              SortedMap<LocalDate, Bar> bars50 = marketData.get(contract).tailMap(dateAgo50,
                  true);
              double movingSum50 = bars50.entrySet().stream().mapToDouble(barEntry ->
                  barEntry.getValue().getClose()).sum() + preparedBar.getClose();;
              preparedBar.setProperty("50dma", movingSum50 / 50);

              SortedMap<LocalDate, Bar> bars200 = marketData.get(contract).tailMap(dateAgo200,
                  true);
              double movingSum200 = bars200.entrySet().stream().mapToDouble(barEntry ->
                  barEntry.getValue().getClose()).sum() + preparedBar.getClose();;
              preparedBar.setProperty("200dma", movingSum200 / 200);

              String[] criteria = new String[]{"criteria_1", "criteria_2", "criteria_3",
                  "criteria_4"};
              preparedBar.setProperty(criteria[0], condition1.fulfills(contractMarketData, preparedBar) ? 1 : 0);
              preparedBar.setProperty(criteria[1], condition2.fulfills(contractMarketData, preparedBar) ? 1 : 0);
              preparedBar.setProperty(criteria[2], condition3.fulfills(contractMarketData, preparedBar) ? 1 : 0);
              preparedBar.setProperty(criteria[3], condition4.fulfills(contractMarketData, preparedBar) ? 1 : 0);
              preparedBar.setProperty("all_4", Arrays.stream(criteria).mapToDouble(preparedBar::getProperty).sum()
                  == 4 ? 1 : 0);

            }
            return preparedBar;
          }

          @Override
          public boolean shouldBuy(AssetManager am, Contract contract,
              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {

            TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);

            if (contractMarketData.size() > 1) {
              Bar previous = contractMarketData.lowerEntry(bar.getDate()).getValue();

              boolean previousPosition = previous.getProperty("position") == 1;
              Boolean shouldBuy = !bar.isLastBar() &&
                  !previousPosition && previous.getProperty("all_4") == 0 && bar.getProperty("all_4") == 1;
              bar.setProperty("position", shouldBuy ? 1 :
                  (previousPosition && previous.getProperty("stop_out") == 0 ? 1 : 0));

              if (previousPosition && bar.getProperty("position") ==1) {
                bar.setProperty("entry_price", previous.getProperty("entry_price"));
                bar.setProperty("initial_stop_loss", previous.getProperty("initial_stop_loss"));
                bar.setProperty("trailing_stop_loss",
                    Math.max(previous.getProperty("trailing_stop_loss"),
                        (1 - tradingConfig.getStopLoss(contract)) * bar.getClose()));
              }

              if (shouldBuy) {
                bar.setProperty("initial_stop_loss",
                    bar.getHigh() * (1 - tradingConfig.getStopLoss(contract)));
                bar.setProperty("trailing_stop_loss", bar.getProperty("initial_stop_loss"));
              }

              return shouldBuy;
            }

            return false;
          }

          @Override
          public boolean shouldSell(AssetManager am, Contract contract,
              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
            boolean shouldSell = bar.isLastBar() || (bar.getProperty("position") == 1 &&
                bar.getLow() < bar.getProperty("trailing_stop_loss"));
            bar.setProperty("stop_out", shouldSell ? 1 : 0);
            return shouldSell;
          }

          @Override
          public double getExitPrice(AssetManager am, Contract contract,
              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
            double exitPrice = Math.min(Math.min(bar.getLow(),
                bar.getProperty("trailing_stop_loss")), bar.getOpen());
            bar.setProperty("exit_price", exitPrice);
            return exitPrice;
          }
        });


    autoTrader.runSimulationWithDBPreparedBar(contracts1, tradingConfig, new RunSimulationWithDBPreparedBarStrategy() {

      Condition condition1 = ((contractMarketData, bar) ->
          bar.getProperty("20dma") > bar.getProperty("50dma"));

      Condition condition2 = ((contractMarketData, bar) ->
          bar.getProperty("20dma") > bar.getProperty("200dma"));

      Condition condition3 = ((contractMarketData, bar) ->
          bar.getProperty("50dma") > bar.getProperty("200dma"));

      Condition condition4 = ((contractMarketData, bar) -> {
        Bar lastEntry = contractMarketData.lastEntry().getValue();
        return lastEntry.getOpen() > bar.getProperty("200dma") &&
            lastEntry.getLow() > bar.getProperty("200dma");
      });

      @Override
      public Bar prepare(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
          Contract contract, Bar historicalBar, LocalDate date, ArrayList<Bar> missingBars) {

        Bar preparedBar = null;

        // If today has missing data, we assume to use yesterday's data.
        if (historicalBar == null) {
          preparedBar = marketData.get(contract).lastEntry().getValue().copy();
          preparedBar.setDate(date);
          System.out.println("Missing: " + contract.getSymbol() + " " + preparedBar.getDate());
          missingBars.add(preparedBar);
        } else {
          preparedBar = historicalBar.copy();
        }

        TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);
        int currentSize = contractMarketData.size();
        if (currentSize >= 199) {

          LocalDate[] contractMarketDates =
              contractMarketData.keySet().toArray(new LocalDate[currentSize]);
          LocalDate dateAgo200 = contractMarketDates[currentSize - 199];
          LocalDate dateAgo50 = contractMarketDates[currentSize - 49];
          LocalDate dateAgo20 = contractMarketDates[currentSize - 19];

          SortedMap<LocalDate, Bar> bars20 = marketData.get(contract).tailMap(dateAgo20,
              true);
          double movingSum20 = bars20.entrySet().stream().mapToDouble(barEntry ->
              barEntry.getValue().getClose()).sum() + preparedBar.getClose();
          preparedBar.setProperty("20dma", movingSum20 / 20);

          SortedMap<LocalDate, Bar> bars50 = marketData.get(contract).tailMap(dateAgo50,
              true);
          double movingSum50 = bars50.entrySet().stream().mapToDouble(barEntry ->
              barEntry.getValue().getClose()).sum() + preparedBar.getClose();;
          preparedBar.setProperty("50dma", movingSum50 / 50);

          SortedMap<LocalDate, Bar> bars200 = marketData.get(contract).tailMap(dateAgo200,
              true);
          double movingSum200 = bars200.entrySet().stream().mapToDouble(barEntry ->
              barEntry.getValue().getClose()).sum() + preparedBar.getClose();;
          preparedBar.setProperty("200dma", movingSum200 / 200);

          String[] criteria = new String[]{"criteria_1", "criteria_2", "criteria_3",
              "criteria_4"};
          preparedBar.setProperty(criteria[0], condition1.fulfills(contractMarketData, preparedBar) ? 1 : 0);
          preparedBar.setProperty(criteria[1], condition2.fulfills(contractMarketData, preparedBar) ? 1 : 0);
          preparedBar.setProperty(criteria[2], condition3.fulfills(contractMarketData, preparedBar) ? 1 : 0);
          preparedBar.setProperty(criteria[3], condition4.fulfills(contractMarketData, preparedBar) ? 1 : 0);
          preparedBar.setProperty("all_4", Arrays.stream(criteria).mapToDouble(preparedBar::getProperty).sum()
              == 4 ? 1 : 0);

        }
        return preparedBar;
      }

      @Override
      public boolean shouldBuy(AssetManager am, Contract contract,
          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {

        TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);


        if (contractMarketData.size() >= 199) {
          Bar previous = contractMarketData.lowerEntry(bar.getDate()).getValue();

          boolean previousPosition = previous.getProperty("position") == 1;
          Boolean shouldBuy = !bar.isLastBar() &&
              !previousPosition && previous.getProperty("all_4") == 0 && bar.getProperty("all_4") == 1;
          bar.setProperty("position", shouldBuy ? 1 :
              (previousPosition && previous.getProperty("stop_out") == 0 ? 1 : 0));

          if (previousPosition && bar.getProperty("position") ==1) {
            bar.setProperty("entry_price", previous.getProperty("entry_price"));
            bar.setProperty("initial_stop_loss", previous.getProperty("initial_stop_loss"));
            bar.setProperty("trailing_stop_loss",
                Math.max(previous.getProperty("trailing_stop_loss"),
                    (1 - tradingConfig.getStopLoss(contract)) * bar.getClose()));
          }

          if (shouldBuy) {
            bar.setProperty("initial_stop_loss",
                bar.getHigh() * (1 - tradingConfig.getStopLoss(contract)));
            bar.setProperty("trailing_stop_loss", bar.getProperty("initial_stop_loss"));
          }

          return shouldBuy;
        }

        return false;
      }

      @Override
      public boolean shouldSell(AssetManager am, Contract contract,
          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
        if (marketData.get(contract).size() >= 199) {
          boolean shouldSell = bar.isLastBar() || (bar.getProperty("position") == 1 &&
              bar.getLow() < bar.getProperty("trailing_stop_loss"));
          bar.setProperty("stop_out", shouldSell ? 1 : 0);
          return shouldSell;
        }
        return false;
      }


      @Override
      // Need to rebalance when residual cash < AUM / split * (split - currentlyOwnedStocks)
      public boolean shouldRebalance(AssetManager am,
          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split) {
        return split != 0 && am.getResidualAssets() <
            am.getTotalAssetValue(marketData) / split * (split - am.getContracts().length);
      }

      @Override
      public double getRequiredTotalCashOnRebalance(AssetManager am,
          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split){
        return am.getTotalAssetValue(marketData) / split * (split - am.getContracts().length) -
            am.getResidualAssets();
      }

      @Override
      public double getSellPriceOnRebalance(AssetManager am, Contract contract,
          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
        return bar.getLow();
      }

      @Override
      // StocksToSellOnRebalance = requiredIndividualCash / low
      public int getStocksToSellOnRebalance(Bar bar, double requiredIndividualCash) {
        return (int) Math.floor((requiredIndividualCash / bar.getLow()));
      }

      @Override
      public double getEntryPrice(AssetManager am, Contract contract,
          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
        double entryPrice = bar.getHigh();
        bar.setProperty("entry_price", entryPrice);
        return entryPrice;
      }

      @Override
      public double getExitPrice(AssetManager am, Contract contract,
          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
        double exitPrice = Math.min(Math.min(bar.getLow(),
            bar.getProperty("trailing_stop_loss")), bar.getOpen());
        bar.setProperty("exit_price", exitPrice);
        return exitPrice;
      }

      @Override
      public void postProcess(AssetManager am,
          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
          Contract contract, Bar bar) {
        bar.setProperty("stocks_owned", am.getOwnedStocks(contract));
        bar.setProperty("stock_value", am.getOwnedStocks(contract) * bar.getClose());
        bar.setProperty("cash", am.getResidualAssets());
        bar.setProperty("aum", am.getTotalAssetValue(marketData));
      }

    });



    // run simulation with raw bar (original version)
//    autoTrader.runSimulationWithDBRawBar(contracts1, tradingConfig, new RunSimulationWithDBRawBarStrategy() {
//
//      Condition condition1 = ((contractMarketData, bar) ->
//          bar.getProperty("20dma") > bar.getProperty("50dma"));
//
//      Condition condition2 = ((contractMarketData, bar) ->
//          bar.getProperty("20dma") > bar.getProperty("200dma"));
//
//      Condition condition3 = ((contractMarketData, bar) ->
//          bar.getProperty("50dma") > bar.getProperty("200dma"));
//
//      Condition condition4 = ((contractMarketData, bar) -> {
//        Bar lastEntry = contractMarketData.lastEntry().getValue();
//        return lastEntry.getOpen() > bar.getProperty("200dma") &&
//            lastEntry.getLow() > bar.getProperty("200dma");
//      });
//
//      @Override
//      public Bar prepare(AssetManager am, HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
//          Contract contract, Bar historicalBar, LocalDate date) {
//
//        Bar preparedBar = null;
//
//        // If today has missing data, we assume to use yesterday's data.
//        if (historicalBar == null) {
//          preparedBar = marketData.get(contract).lastEntry().getValue().copy();
//          preparedBar.setDate(date);
//          System.out.println("Missing: " + contract.getSymbol() + " " + preparedBar.getDate());
//        } else {
//          preparedBar = historicalBar.copy();
//        }
//
//        TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);
//        int currentSize = contractMarketData.size();
//        if (currentSize >= 199) {
//
//          LocalDate[] contractMarketDates =
//              contractMarketData.keySet().toArray(new LocalDate[currentSize]);
//          LocalDate dateAgo200 = contractMarketDates[currentSize - 199];
//          LocalDate dateAgo50 = contractMarketDates[currentSize - 49];
//          LocalDate dateAgo20 = contractMarketDates[currentSize - 19];
//
//          SortedMap<LocalDate, Bar> bars20 = marketData.get(contract).tailMap(dateAgo20,
//              true);
//          double movingSum20 = bars20.entrySet().stream().mapToDouble(barEntry ->
//              barEntry.getValue().getClose()).sum() + preparedBar.getClose();
//          preparedBar.setProperty("20dma", movingSum20 / 20);
//
//          SortedMap<LocalDate, Bar> bars50 = marketData.get(contract).tailMap(dateAgo50,
//              true);
//          double movingSum50 = bars50.entrySet().stream().mapToDouble(barEntry ->
//              barEntry.getValue().getClose()).sum() + preparedBar.getClose();;
//          preparedBar.setProperty("50dma", movingSum50 / 50);
//
//          SortedMap<LocalDate, Bar> bars200 = marketData.get(contract).tailMap(dateAgo200,
//              true);
//          double movingSum200 = bars200.entrySet().stream().mapToDouble(barEntry ->
//              barEntry.getValue().getClose()).sum() + preparedBar.getClose();;
//          preparedBar.setProperty("200dma", movingSum200 / 200);
//
//          String[] criteria = new String[]{"criteria_1", "criteria_2", "criteria_3",
//              "criteria_4"};
//          preparedBar.setProperty(criteria[0], condition1.fulfills(contractMarketData, preparedBar) ? 1 : 0);
//          preparedBar.setProperty(criteria[1], condition2.fulfills(contractMarketData, preparedBar) ? 1 : 0);
//          preparedBar.setProperty(criteria[2], condition3.fulfills(contractMarketData, preparedBar) ? 1 : 0);
//          preparedBar.setProperty(criteria[3], condition4.fulfills(contractMarketData, preparedBar) ? 1 : 0);
//          preparedBar.setProperty("all_4", Arrays.stream(criteria).mapToDouble(preparedBar::getProperty).sum()
//              == 4 ? 1 : 0);
//
//        }
//        return preparedBar;
//      }
//
//      @Override
//      public boolean shouldBuy(AssetManager am, Contract contract,
//          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//
//        TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);
//
//
//        if (contractMarketData.size() > 1) {
//          Bar previous = contractMarketData.lowerEntry(bar.getDate()).getValue();
//
//          boolean previousPosition = previous.getProperty("position") == 1;
//          Boolean shouldBuy = !bar.isLastBar() &&
//              !previousPosition && previous.getProperty("all_4") == 0 && bar.getProperty("all_4") == 1;
//          bar.setProperty("position", shouldBuy ? 1 :
//              (previousPosition && previous.getProperty("stop_out") == 0 ? 1 : 0));
//
//          if (previousPosition && bar.getProperty("position") ==1) {
//            bar.setProperty("entry_price", previous.getProperty("entry_price"));
//            bar.setProperty("initial_stop_loss", previous.getProperty("initial_stop_loss"));
//            bar.setProperty("trailing_stop_loss",
//                Math.max(previous.getProperty("trailing_stop_loss"),
//                    (1 - tradingConfig.getStopLoss(contract)) * bar.getClose()));
//          }
//
//          if (shouldBuy) {
//            bar.setProperty("initial_stop_loss",
//                bar.getHigh() * (1 - tradingConfig.getStopLoss(contract)));
//            bar.setProperty("trailing_stop_loss", bar.getProperty("initial_stop_loss"));
//          }
//
//          return shouldBuy;
//        }
//
//        return false;
//      }
//
//      @Override
//      public boolean shouldSell(AssetManager am, Contract contract,
//          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//        boolean shouldSell = bar.isLastBar() || (bar.getProperty("position") == 1 &&
//            bar.getLow() < bar.getProperty("trailing_stop_loss"));
//        bar.setProperty("stop_out", shouldSell ? 1 : 0);
//        return shouldSell;
//      }
//
//
//      @Override
//      // Need to rebalance when residual cash < AUM / split * (split - currentlyOwnedStocks)
//      public boolean shouldRebalance(AssetManager am,
//          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split) {
//        return split != 0 && am.getResidualAssets() <
//            am.getTotalAssetValue(marketData) / split * (split - am.getContracts().length);
//      }
//
//      @Override
//      public double getRequiredTotalCashOnRebalance(AssetManager am,
//          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split){
//        return am.getTotalAssetValue(marketData) / split * (split - am.getContracts().length) -
//            am.getResidualAssets();
//      }
//
//      @Override
//      public double getSellPriceOnRebalance(AssetManager am, Contract contract,
//          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//        return bar.getLow();
//      }
//
//      @Override
//      // StocksToSellOnRebalance = requiredIndividualCash / low
//      public int getStocksToSellOnRebalance(Bar bar, double requiredIndividualCash) {
//        return (int) Math.floor((requiredIndividualCash / bar.getLow()));
//      }
//
//      @Override
//      public double getEntryPrice(AssetManager am, Contract contract,
//          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//        double entryPrice = bar.getHigh();
//        bar.setProperty("entry_price", entryPrice);
//        return entryPrice;
//      }
//
//      @Override
//      public double getExitPrice(AssetManager am, Contract contract,
//          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
//        double exitPrice = Math.min(Math.min(bar.getLow(),
//            bar.getProperty("trailing_stop_loss")), bar.getOpen());
//        bar.setProperty("exit_price", exitPrice);
//        return exitPrice;
//      }
//
//      @Override
//      public void postProcess(AssetManager am,
//          HashMap<Contract, TreeMap<LocalDate, Bar>> marketData,
//          Contract contract, Bar bar) {
//        bar.setProperty("stocks_owned", am.getOwnedStocks(contract));
//        bar.setProperty("stock_value", am.getOwnedStocks(contract) * bar.getClose());
//        bar.setProperty("cash", am.getResidualAssets());
//        bar.setProperty("aum", am.getTotalAssetValue(marketData));
//      }
//
//    });

//    Runtime.getRuntime().addShutdownHook(new Thread(autoTrader::shutdown));
  }


}