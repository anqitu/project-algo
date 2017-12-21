import com.ib.client.Contract;
import com.ib.contracts.StkContract;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
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

    StkContract nkeContract = new StkContract("NKE");

    TradingConfig tradingConfig = new TradingConfig(0.1,
        1000000);
    tradingConfig.setStopLoss(nkeContract, 0.2);

    AutoTrader autoTrader = new AutoTrader(host, port, clientId);
    autoTrader.runSimulation(
        Arrays.asList(nkeContract), tradingConfig, 18,
        58, new Strategy() {

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
              bar.setProperty("position", shouldBuy ? 1 : (previousPosition ? 1 : 0));

              if (previousPosition) {
                bar.setProperty("entry_price", previous.getProperty("entry_price"));
                bar.setProperty("initial_stop_loss", previous.getProperty("initial_stop_loss"));
                bar.setProperty("trailing_stop_loss",
                    Math.max(previous.getProperty("trailing_stop_loss"),
                        (1 - tradingConfig.getStopLoss(contract)) * bar.getClose()));
              }

              if (shouldBuy) {
                bar.setProperty("entry_price", bar.getHigh());
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

            double low = bar.getLow();
            double trailingStopLoss = bar.getProperty("traling_stop_loss");

            boolean shouldSell = bar.getProperty("position") == 1 && low < trailingStopLoss;
            if (shouldSell) {
              bar.setProperty("stop_out", 1);
              bar.setProperty("exit_price",
                  Math.min(Math.min(low, trailingStopLoss), bar.getOpen()));
            }
            return shouldSell;
          }

          @Override
          public double getExitPrice(AssetManager am, Contract contract,
              HashMap<Contract, TreeMap<LocalDate, Bar>> marketData, Bar bar) {
            return bar.getProperty("exit_price");
          }
        }, marketData -> {
          try {
            autoTrader.exportHistoricalData(new String[]{"open", "high", "low", "close", "20dma",
                    "50dma", "200dma", "criteria_1", "criteria_2", "criteria_3", "criteria_4",
                    "all_4", "position", "entry_price", "initial_stop_loss",
                "trailing_stop_loss", "stop_out", "exit_price", "aum", "stocks_owned"},
                marketData.get(nkeContract), "sim.csv");
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

    Runtime.getRuntime().addShutdownHook(new Thread(autoTrader::shutdown));
  }
}