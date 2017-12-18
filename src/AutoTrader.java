import com.ib.client.Bar;
import com.ib.client.CommissionReport;
import com.ib.client.Contract;
import com.ib.client.ContractDescription;
import com.ib.client.ContractDetails;
import com.ib.client.DeltaNeutralContract;
import com.ib.client.DepthMktDataDescription;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReaderSignal;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.FamilyCode;
import com.ib.client.HistogramEntry;
import com.ib.client.HistoricalTick;
import com.ib.client.HistoricalTickBidAsk;
import com.ib.client.HistoricalTickLast;
import com.ib.client.NewsProvider;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.PriceIncrement;
import com.ib.client.SoftDollarTier;
import com.ib.client.TickAttr;

import javax.swing.text.DateFormatter;
import java.io.Console;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AutoTrader extends Thread implements EWrapper {

  // Connection Settings
  private final ConnectionConfig connectionConfig;

  // EWrapper Settings

  // Keep track of the next Order ID
  private int nextOrderID = 0;
  private int currentOrderId = 0;
  private double limitPrice;
  private int currentTick = 1000;
  private int currentReqId = 0;

  private final EClientSocket clientSocket;
  private final EReaderSignal readerSignal;
  private ConnectionListener connectionListener;
  private final Contract contract;
  private TraderConfig traderConfig;

  private TreeMap<LocalDate, MovingAverageBar> historicalBars = new TreeMap<>();
  private TreeMap<LocalDate, MovingAverageBar> realtimeBars = new TreeMap<>();

  // Each client deal with one contract, thus only has one order at any time
  private Order currentOrder;

  public AutoTrader(ConnectionConfig connectionConfig, TraderConfig traderConfig, Contract contract) {

      this.connectionConfig = connectionConfig;
      this.traderConfig = traderConfig;
      this.contract = contract;
      this.readerSignal = new EJavaSignal();

      clientSocket = new EClientSocket(this, readerSignal);
    }

  public EClientSocket getClientSocket() {
    return clientSocket;
  }

  public EReaderSignal getReaderSignal() {
    return readerSignal;
  }

  public Contract getContract() {
    return contract;
  }

  public void setConnectionListener(ConnectionListener connectionListener) {
    this.connectionListener = connectionListener;
  }

  public void connect() {
    System.out.println("Creating a connection...");
    int attempt = 1;
    try {
      while (!clientSocket.isConnected()) {
        System.out.println("Attempting to connect... (" + attempt + ")");
        clientSocket.eConnect(connectionConfig.getHost(), connectionConfig.getSocketPort(),
            connectionConfig.getClientId());
      }
      Thread.sleep(3000);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (connectionListener != null) {
      connectionListener.connectionEstablished();
    }
    System.out.println("Connected!");
  }

  public void initSimulation(String endDate, String duration) {

    if (!clientSocket.isConnected()) {
      throw new IllegalStateException();
    }

    System.out.println("Requesting historical data...");
    clientSocket.reqHistoricalData(currentTick++, contract, endDate, duration, "1 day",
        "TRADES", 1, 1, true, null);
  }

  public void simulate() {
//    client.reqHistoricalTicks();
  }

  public void sendMarketOrder(String action, double quantity) {

    if (!clientSocket.isConnected()) {
      throw new IllegalStateException();
    }

    // Create order
    currentOrder = new Order();
    currentOrder.action(action);
    currentOrder.orderId(currentOrderId);
    currentOrder.orderType("TRAIL LIMIT");
    currentOrder.totalQuantity(quantity);
    currentOrder.account(connectionConfig.getAccount());
    currentOrder.trailStopPrice(realtimeBars.lastEntry().getValue().close() * (1 - traderConfig.getStopLoss()));
    // Place order
    clientSocket.placeOrder(currentOrderId++, contract, currentOrder);
  }


  @Override
  public void tickPrice(int i, int i1, double v, TickAttr tickAttr) {

  }

  @Override
  public void tickSize(int i, int i1, int i2) {

  }

  @Override
  public void tickOptionComputation(int i, int i1, double v, double v1, double v2, double v3,
      double v4, double v5, double v6, double v7) {

  }

  @Override
  public void tickGeneric(int i, int i1, double v) {

  }

  @Override
  public void tickString(int i, int i1, String s) {

  }

  @Override
  public void tickEFP(int i, int i1, double v, String s, double v1, int i2, String s1, double v2,
      double v3) {

  }

  @Override
  public void orderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId,
      double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
    System.out.println("OrderStatus. Id: "+orderId+", Status: "+status+", Filled"+filled+", Remaining: "+remaining
        +", AvgFillPrice: "+avgFillPrice+", PermId: "+permId+", ParentId: "+parentId+", LastFillPrice: "+lastFillPrice+
        ", ClientId: "+clientId+", WhyHeld: "+whyHeld+", MktCapPrice: "+mktCapPrice);
  }

  @Override
  public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
    System.out.println("OpenOrder. ID: "+orderId+", "+contract.symbol()+", "+contract.secType()+" @ "
        +contract.exchange()+": "+ order.action()+", "+order.orderType()+" "
        +order.totalQuantity()+", "+orderState.status());

//    traderConfig.setAssetUnderManagement(order.totalQuantity() * realtimeBars.lastEntry().getValue().close());
//    traderConfig.addAssets(realtimeBars.lastKey(), traderConfig.getAssetUnderManagement());

    // Update trailing stop loss if current trailing price is smaller than the new one
    if (order.trailStopPrice() < realtimeBars.lastEntry().getValue().close() * (1 - traderConfig.getStopLoss()))
    currentOrder.trailStopPrice(realtimeBars.lastEntry().getValue().close() * (1 - traderConfig.getStopLoss()));
  }

  @Override
  public void openOrderEnd() {

  }

  @Override
  public void updateAccountValue(String s, String s1, String s2, String s3) {

  }

  @Override
  public void updatePortfolio(Contract contract, double v, double v1, double v2, double v3,
      double v4, double v5, String s) {

  }

  @Override
  public void updateAccountTime(String s) {

  }

  @Override
  public void accountDownloadEnd(String s) {

  }

  @Override
  public void nextValidId(int i) {
    System.out.println("Next Valid Id: "+ i);
    currentOrderId = i;
  }

  @Override
  public void contractDetails(int i, ContractDetails contractDetails) {

  }

  @Override
  public void bondContractDetails(int i, ContractDetails contractDetails) {

  }

  @Override
  public void contractDetailsEnd(int i) {

  }

  @Override
  public void execDetails(int i, Contract contract, Execution execution) {

  }

  @Override
  public void execDetailsEnd(int i) {

  }

  @Override
  public void updateMktDepth(int i, int i1, int i2, int i3, double v, int i4) {

  }

  @Override
  public void updateMktDepthL2(int i, int i1, String s, int i2, int i3, double v, int i4) {

  }

  @Override
  public void updateNewsBulletin(int i, int i1, String s, String s1) {

  }

  @Override
  public void managedAccounts(String s) {

  }

  @Override
  public void receiveFA(int i, String s) {

  }

  @Override
  public void historicalData(int requestId, Bar bar) {
    System.out.println("HistoricalData. "+ requestId +" - Date: "+bar.time()+", Open: "+bar.open()+
        ", High: "+bar.high()+", Low: "+bar.low()+", Close: "+bar.close()+", Volume: "+
        bar.volume()+", Count: "+bar.count()+", WAP: "+bar.wap());

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    LocalDate currentDate = LocalDate.parse(bar.time(),formatter);
    historicalBars.put(currentDate, new MovingAverageBar(bar.time(), bar.open(), bar.high(), bar.low(), bar.close(),
                    bar.volume(), bar.count(), bar.wap()));

  }

  @Override
  public void scannerParameters(String s) {

  }

  @Override
  public void scannerData(int i, int i1, ContractDetails contractDetails, String s, String s1,
      String s2, String s3) {

  }

  @Override
  public void scannerDataEnd(int i) {

  }

  @Override
  public void realtimeBar(int i, long l, double v, double v1, double v2, double v3, long l1,
      double v4, int i1) {

    int reqId = i, count = i1;
    double open = v, high = v1, low = v2, close = v3, wap = v4;
    long volume = l1;

    LocalDate currentDate = Instant.ofEpochMilli(l).atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate dateAgo200 = currentDate.minus(Period.ofDays(199));
    LocalDate dateAgo50 = currentDate.minus(Period.ofDays(49));
    LocalDate dateAgo20 = currentDate.minus(Period.ofDays(19));


    double movingSum20 = 0;
    SortedMap<LocalDate, MovingAverageBar> bars20 = realtimeBars.tailMap(dateAgo20, true);
    for (Map.Entry<LocalDate, MovingAverageBar> entry20 : bars20.entrySet()) {
      LocalDate key = entry20.getKey();
      movingSum20 = movingSum20 + entry20.getValue().close();
    }
    realtimeBars.get(currentDate).setMovingAvg20(movingSum20/20);

    double movingSum50 = 0;
    SortedMap<LocalDate, MovingAverageBar> bars50 = realtimeBars.tailMap(dateAgo50, true);
    for (Map.Entry<LocalDate, MovingAverageBar> entry50 : bars50.entrySet()) {
      LocalDate key = entry50.getKey();
      movingSum20 = movingSum20 + entry50.getValue().close();
    }
    realtimeBars.get(currentDate).setMovingAvg50(movingSum50/50);

    double movingSum200 = 0;
    SortedMap<LocalDate, MovingAverageBar> bars200 = realtimeBars.tailMap(dateAgo200, true);
    for (Map.Entry<LocalDate, MovingAverageBar> entry200 : bars200.entrySet()) {
      LocalDate key = entry200.getKey();
      movingSum200 = movingSum200 + entry200.getValue().close();
    }
    realtimeBars.get(currentDate).setMovingAvg200(movingSum200/200);

    // If there is an active order already, update trailing stop loss if necessary
    if (currentOrder != null){
      clientSocket.reqOpenOrders();
    }

    // If there is no active order, buy when all 4 conditions are met
    else if (movingSum20 > movingSum50 && movingSum50 > movingSum200 && open > movingSum200 && low > movingSum200){
      sendMarketOrder("BUY", traderConfig.getAssetUnderManagement()/high);

      // TODO: Might need to check the status of the order to ensure is is successfully submitted
//    traderConfig.setAssetUnderManagement(order.totalQuantity() * realtimeBars.lastEntry().getValue().close());
//    traderConfig.addAssets(realtimeBars.lastKey(), traderConfig.getAssetUnderManagement());

    }

    // Update AUM
    clientSocket.reqAccountSummary(currentReqId++, "All", "NetLiquidation");
  }

  @Override
  public void currentTime(long l) {

  }

  @Override
  public void fundamentalData(int i, String s) {

  }

  @Override
  public void deltaNeutralValidation(int i, DeltaNeutralContract deltaNeutralContract) {

  }

  @Override
  public void tickSnapshotEnd(int i) {

  }

  @Override
  public void marketDataType(int i, int i1) {

  }

  @Override
  public void commissionReport(CommissionReport commissionReport) {

  }

  @Override
  public void position(String s, Contract contract, double v, double v1) {

  }

  @Override
  public void positionEnd() {

  }

  @Override
  public void accountSummary(int i, String s, String s1, String s2, String s3) {
    System.out.println("ReqId: "+ i +" - account: "+ s + s1 + " : " +
            s2 + " " + s3);
    if (s1.equals("NetLiquidation")) {
      traderConfig.setAssetUnderManagement(Double.parseDouble(s2));
      traderConfig.addAssets(realtimeBars.lastKey(), traderConfig.getAssetUnderManagement());
    }

  }

  @Override
  public void accountSummaryEnd(int i) {

  }

  @Override
  public void verifyMessageAPI(String s) {

  }

  @Override
  public void verifyCompleted(boolean b, String s) {

  }

  @Override
  public void verifyAndAuthMessageAPI(String s, String s1) {

  }

  @Override
  public void verifyAndAuthCompleted(boolean b, String s) {

  }

  @Override
  public void displayGroupList(int i, String s) {

  }

  @Override
  public void displayGroupUpdated(int i, String s) {

  }

  @Override
  public void error(Exception e) {

  }

  @Override
  public void error(String s) {

  }

  @Override
  public void error(int i, int i1, String s) {

  }

  @Override
  public void connectionClosed() {

  }

  @Override
  public void connectAck() {

  }

  @Override
  public void positionMulti(int i, String s, String s1, Contract contract, double v, double v1) {

  }

  @Override
  public void positionMultiEnd(int i) {

  }

  @Override
  public void accountUpdateMulti(int i, String s, String s1, String s2, String s3, String s4) {

  }

  @Override
  public void accountUpdateMultiEnd(int i) {

  }

  @Override
  public void securityDefinitionOptionalParameter(int i, String s, int i1, String s1, String s2,
      Set<String> set, Set<Double> set1) {

  }

  @Override
  public void securityDefinitionOptionalParameterEnd(int i) {

  }

  @Override
  public void softDollarTiers(int i, SoftDollarTier[] softDollarTiers) {

  }

  @Override
  public void familyCodes(FamilyCode[] familyCodes) {

  }

  @Override
  public void symbolSamples(int i, ContractDescription[] contractDescriptions) {

  }

  @Override
  public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {


//    System.out.println("HistoricalDataEnd. "+reqId+" - Start Date: "+startDateStr+", End Date: "+endDateStr);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    LocalDate startDate = LocalDate.parse(startDateStr,formatter);
    LocalDate endDate = LocalDate.parse(endDateStr,formatter);
    LocalDate date200 = startDate.plus(Period.ofDays(199));
    SortedMap<LocalDate, MovingAverageBar> barsAfter200Days = historicalBars.tailMap(date200, true);

    // Start from the 200th day.
    for(Map.Entry<LocalDate, MovingAverageBar> entry : barsAfter200Days.entrySet()) {
      MovingAverageBar bar = entry.getValue();
      realtimeBars.put(entry.getKey(), bar);
      realtimeBar(reqId, entry.getKey().toEpochDay(), bar.open(),
              bar.high(), bar.low(), bar.close(), bar.volume(), bar.wap(), bar.count());
    }
  }

  @Override
  public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {

  }

  @Override
  public void tickNews(int i, long l, String s, String s1, String s2, String s3) {

  }

  @Override
  public void smartComponents(int i, Map<Integer, Map.Entry<String, Character>> map) {

  }

  @Override
  public void tickReqParams(int i, double v, String s, int i1) {

  }

  @Override
  public void newsProviders(NewsProvider[] newsProviders) {

  }

  @Override
  public void newsArticle(int i, int i1, String s) {

  }

  @Override
  public void historicalNews(int i, String s, String s1, String s2, String s3) {

  }

  @Override
  public void historicalNewsEnd(int i, boolean b) {

  }

  @Override
  public void headTimestamp(int i, String s) {

  }

  @Override
  public void histogramData(int i, List<HistogramEntry> list) {

  }

  @Override
  public void historicalDataUpdate(int i, Bar bar) {

  }

  @Override
  public void rerouteMktDataReq(int i, int i1, String s) {

  }

  @Override
  public void rerouteMktDepthReq(int i, int i1, String s) {

  }

  @Override
  public void marketRule(int i, PriceIncrement[] priceIncrements) {

  }

  @Override
  public void pnl(int i, double v, double v1, double v2) {

  }

  @Override
  public void pnlSingle(int i, int i1, double v, double v1, double v2, double v3) {

  }

  @Override
  public void historicalTicks(int i, List<HistoricalTick> list, boolean b) {

  }

  @Override
  public void historicalTicksBidAsk(int i, List<HistoricalTickBidAsk> list, boolean b) {

  }

  @Override
  public void historicalTicksLast(int i, List<HistoricalTickLast> list, boolean b) {

  }
}
