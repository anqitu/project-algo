import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.OrderStatus;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.client.Types.WhatToShow;
import com.ib.contracts.StkContract;
import com.ib.controller.ApiController;
import com.ib.controller.ApiController.IHistoricalDataHandler;
import com.ib.controller.ApiController.ILiveOrderHandler;
import com.ib.controller.Bar;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.TreeMap;

public class AutoTraderApp {

  private static final String host = "localhost";
  private static final int port = 4002;
  private static final int clientId = 1;

  private double stopLoss = 0.2;
  private double movingAvgRange = 0.1;
  private double assetUnderManagement = 1000000;
  private TreeMap<LocalDate, Double> assets = new TreeMap<>();
  private double stockOwned = 0;

  public static void main(String[] args) {

    ConnectionHandler connectionHandler = new ConnectionHandler();

    ApiController gateway = new ApiController(connectionHandler, s -> {
    }, s -> {
    });
    gateway.connect(host, port, clientId, null);
    connectionHandler.waitForConnection();

    //////////////////// ALGORITHM IMPLEMENTATION STARTS HERE //////////////////////////////////////

    //    TradeConfig tradeConfig = new TradeConfig();
    //    tradeConfig.setStopLoss(0.2);
    //    tradeConfig.setAssetUnderManagement(1000000);
    //    tradeConfig.setMovingAvgRange(0.1);

    //  private TreeMap<LocalDate, MovingAverageBar> historicalBars = new TreeMap<>();
    //  private TreeMap<LocalDate, MovingAverageBar> realtimeBars = new TreeMap<>();

    HashMap<Contract, Order> orders = new HashMap<>();

    Contract nkeStock = new StkContract("NKE");

    gateway.reqContractDetails(nkeStock, list -> {
      System.out.println(list.size());
      for (ContractDetails contractDetails : list) {
        System.out.println(contractDetails);
      }
    });

    gateway.reqHistoricalData(nkeStock, "", 1, DurationUnit.YEAR, BarSize._1_day,
        WhatToShow.TRADES, true, false, new IHistoricalDataHandler() {

          @Override
          public void historicalData(Bar bar) {
            System.out.println(bar);
//            historicalBars.put(currentDate, new MovingAverageBar(bar.time(), bar.open(),
//                bar.high(), bar.low(), bar.close(), bar.volume(), bar.count(), bar.wap()));
          }

          @Override
          public void historicalDataEnd() {
            System.out.println("Received all historical data");
            //    LocalDate startDate = Utilities.parseDate(startDateStr);
            //    LocalDate date200 = startDate.plus(Period.ofDays(199));
            //    SortedMap<LocalDate, MovingAverageBar> barsAfter200Days =
            //        historicalBars.tailMap(date200, true);
            //
            //    // Start simulation from the 200th day
            //    for(Map.Entry<LocalDate, MovingAverageBar> entry : barsAfter200Days.entrySet()) {
            //
            //      // Simulate receiving of realtime bars
            //      MovingAverageBar bar = entry.getValue();
            //      realtimeBar(reqId, entry.getKey().toEpochDay(), bar.open(),
            //              bar.high(), bar.low(), bar.close(), bar.volume(), bar.wap(), bar.count());
            //    }
          }
        });

    gateway.reqRealTimeBars(nkeStock, WhatToShow.TRADES, true, bar -> {

      // Parse dates
      //    LocalDate barDate = Instant.ofEpochMilli(time)
      //        .atZone(ZoneId.systemDefault()).toLocalDate();
      //    LocalDate dateAgo200 = barDate.minus(Period.ofDays(199));
      //    LocalDate dateAgo50 = barDate.minus(Period.ofDays(49));
      //    LocalDate dateAgo20 = barDate.minus(Period.ofDays(19));
      //
      //    // Calculate moving sums
      //    SortedMap<LocalDate, MovingAverageBar> bars20 = realtimeBars.tailMap(dateAgo20, true);
      //    double movingSum20 = bars20.entrySet().stream()
      //        .mapToDouble(bar -> bar.getValue().close()).sum();
      //
      //    SortedMap<LocalDate, MovingAverageBar> bars50 = realtimeBars.tailMap(dateAgo50, true);
      //    double movingSum50 = bars50.entrySet().stream()
      //        .mapToDouble(bar -> bar.getValue().close()).sum();
      //
      //    SortedMap<LocalDate, MovingAverageBar> bars200 = realtimeBars.tailMap(dateAgo200, true);
      //    double movingSum200 = bars200.entrySet().stream()
      //        .mapToDouble(bar -> bar.getValue().close()).sum();
      //
      //    // Add bar to list
      //    realtimeBars.put(barDate, new MovingAverageBar(Utilities.formatDate(barDate),
      //        open, high, low, close, volume, count, wap, movingSum20/20,
      //        movingSum50/50, movingSum200/200));
      //
      //    // If there is an active order already, update trailing stop loss if necessary
      //    if (currentOrder != null)
      //      clientSocket.reqOpenOrders();
      //
      //    // If there is no active order, buy when all 4 conditions are met
      //    else if (movingSum20 > movingSum50 && movingSum50 > movingSum200 && open > movingSum200 && low > movingSum200){
      //      sendMarketOrder("BUY", tradeConfig.getAssetUnderManagement()/high);
      //
      //      // TODO: Might need to check the status of the order to ensure it is successfully submitted
      ////    tradeConfig.setAssetUnderManagement(order.totalQuantity() * realtimeBars.lastEntry().getValue().close());
      ////    tradeConfig.addAssets(realtimeBars.lastKey(), tradeConfig.getAssetUnderManagement());
      //
      //    }
      //
      //    // Update AUM
      //    clientSocket.reqAccountSummary(currentReqId++, "All", "NetLiquidation");
    });

    //    gateway.placeOrModifyOrder();
    //    currentOrder = new Order();
    //    currentOrder.action(action);
    //    currentOrder.orderId(currentOrderId);
    //    currentOrder.orderType("TRAIL LIMIT");
    //    currentOrder.totalQuantity(quantity);
    //    currentOrder.account(connectionConfig.getAccount());
    //    currentOrder.trailStopPrice(realtimeBars.lastEntry().getValue().close() * (1 - tradeConfig.getStopLoss()));
    //    clientSocket.placeOrder(currentOrderId++, contract, currentOrder);

    gateway.reqLiveOrders(new ILiveOrderHandler() {

      @Override
      public void openOrder(Contract contract, Order order, OrderState orderState) {
        //    System.out.println("OpenOrder. ID: "+order.orderId()+", "+contract.symbol()+", "+contract.secType()+" @ "
        //        +contract.exchange()+": "+ order.action()+", "+order.orderType()+" "
        //        +order.totalQuantity()+", "+orderState.status());
        //
        //    // Not sure if need to update AUM manually or via accountSummary()
        //
        //    //tradeConfig.setAssetUnderManagement(order.totalQuantity() * realtimeBars.lastEntry().getValue().close());
        //    //tradeConfig.addAssets(realtimeBars.lastKey(), tradeConfig.getAssetUnderManagement());
        //
        //    // Update trailing stop loss if current trailing price is smaller than the new one
        //
        //    double todayClosePrice = realtimeBars.lastEntry().getValue().close() * (1 - tradeConfig.getStopLoss());
        //    if (order.trailStopPrice() < todayClosePrice)
        //      currentOrder.trailStopPrice(todayClosePrice);
      }

      @Override
      public void openOrderEnd() {

      }

      @Override
      public void orderStatus(int i, OrderStatus orderStatus, double v, double v1, double v2,
          long l,
          int i1, double v3, int i2, String s, double v4) {

      }

      @Override
      public void handle(int i, int i1, String s) {

      }
    });

    // gateway.reqAccountSummary();
    //    if (s1.equals("NetLiquidation")) {
    //      tradeConfig.setAssetUnderManagement(Double.parseDouble(s2));
    //      tradeConfig.addAssets(realtimeBars.lastKey(), tradeConfig.getAssetUnderManagement());
    //    }

    // END

    Runtime.getRuntime().addShutdownHook(new Thread(gateway::disconnect));
  }
}