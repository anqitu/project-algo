import com.ib.client.Contract;
import com.ib.contracts.StkContract;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class AutoTraderApp {

  private static final String host = "localhost";
  private static final int port = 4002;
  private static final int clientId = 1;

  interface Condition {
    boolean fulfills(TreeMap<LocalDate, Bar> contractMarketData, Bar bar);
  }

  public static void main(String[] args) {

    AutoTrader autoTrader = new AutoTrader(host, port, clientId);
//    autoTrader.run();

    StkContract nkeContract = new StkContract("NKE");
    StkContract jnjContract = new StkContract("JNJ");
    StkContract wmtContract = new StkContract("WMT");
//    EXPORT HISTORICAL DATA TO CSV
//    autoTrader.getHistoricalData(contracts, 10, marketData -> {
//      for (Contract contract : contracts)
//        try {
//          autoTrader.exportData(marketData.get(contract), contract.symbol() + ".csv");
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//    });

    TradingConfig tradingConfig = new TradingConfig(0.1,
        1000000);
    tradingConfig.setStopLoss(nkeContract, 0.2);
    tradingConfig.setStopLoss(jnjContract, 0.2);
    tradingConfig.setStopLoss(wmtContract, 0.2);

    ArrayList<Contract> contracts = new ArrayList<>(Arrays.asList(nkeContract, jnjContract, wmtContract));
    autoTrader.runSimulation(contracts, tradingConfig, 17, new Strategy() {

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
              Contract contract, com.ib.controller.Bar rawBar) {

//            if (rawBar == null)
//              rawBar = marketData.get(contract).lastEntry().getValue().getBar();

            Bar preparedBar = new Bar(rawBar);

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

            }

            return preparedBar;
          }

          @Override
          public boolean shouldBuy(AssetManager am, Contract contract,
              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {

            TreeMap<LocalDate, Bar> contractMarketData = marketData.get(contract);

            if (contractMarketData.size() > 1) {
              Bar previous = contractMarketData.lowerEntry(bar.getDate()).getValue();

              String[] criteria = new String[]{"criteria_1", "criteria_2", "criteria_3",
                  "criteria_4"};
              bar.setProperty(criteria[0], condition1.fulfills(contractMarketData, bar) ? 1 : 0);
              bar.setProperty(criteria[1], condition2.fulfills(contractMarketData, bar) ? 1 : 0);
              bar.setProperty(criteria[2], condition3.fulfills(contractMarketData, bar) ? 1 : 0);
              bar.setProperty(criteria[3], condition4.fulfills(contractMarketData, bar) ? 1 : 0);
              bar.setProperty("all_4", Arrays.stream(criteria).mapToDouble(bar::getProperty).sum()
                  == 4 ? 1 : 0);

              boolean previousPosition = previous.getProperty("position") == 1;
              boolean shouldBuy = !previousPosition && previous.getProperty("all_4") == 0 &&
                  bar.getProperty("all_4") == 1;
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
            boolean shouldSell = bar.getProperty("position") == 1 &&
                bar.getLow() < bar.getProperty("trailing_stop_loss");
            bar.setProperty("stop_out", shouldSell ? 1 : 0);
            return shouldSell;
          }

          @Override
          public boolean shouldRebalance(AssetManager am,
              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split) {
            return split > am.getContracts().length &&
                am.getResidualAssets() < am.getTotalAssetValue(marketData) / split;
          }

          @Override
          public double getSellPriceOnRebalance(AssetManager am, Contract contract,
              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
            return bar.getLow();
          }

          @Override
          public int getStocksToSellOnRebalance(AssetManager am, Contract contract, Bar bar,
              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, int split) {

//            double splitRatio = 1 - (am.getContracts().length / split);
//            return (int) Math.floor(am.getOwnedStocks(contract) * splitRatio);

            double closePrice = bar.getClose();
            return (int) Math.floor((am.getOwnedStocks(contract) *
                closePrice)/ split / closePrice);
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

        }, marketData -> {
          for (Contract contract : contracts)
            try {
              autoTrader.exportData(marketData.get(contract), contract.symbol() + ".csv",
                  new String[]{"open", "high", "low", "close", "20dma",
                      "50dma", "200dma", "criteria_1", "criteria_2", "criteria_3", "criteria_4",
                      "all_4", "position", "entry_price", "initial_stop_loss",
                      "trailing_stop_loss", "stop_out", "exit_price", "stocks_owned",
                      "stock_value", "cash", "aum"});
            } catch (IOException e) {
              e.printStackTrace();
            }
        });

    Runtime.getRuntime().addShutdownHook(new Thread(autoTrader::shutdown));
  }

}