import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Bar implements ExportableToCSV, ExportableToDB {

  private LocalDate date;
  private String symbol;
  private double open;
  private double high;
  private double low;
  private double close;
  private HashMap<String, Double> properties;
  private boolean isLastBar = false;

  public void setLastBar(boolean lastBar) {
    isLastBar = lastBar;
  }

  public boolean isLastBar() {

    return isLastBar;
  }

  public Bar() {
  }

  public Bar(com.ib.controller.Bar bar, Contract contract) {
    this.date = LocalDate.parse(bar.formattedTime(),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    properties = new HashMap<>();
    symbol = contract.getSymbol();
    open = bar.open();
    high = bar.high();
    low = bar.low();
    close = bar.close();
    properties.put("shouldBuy", 0.0);
    properties.put("shouldSell", 0.0);
  }

  public Bar(LocalDate date, String symbol, double open, double high, double low, double close) {
    this.date = date;
    this.symbol = symbol;
    this.open = open;
    this.high = high;
    this.low = low;
    this.close = close;
    properties = new HashMap<>();
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public LocalDate getDate() {
    return date;
  }

  public boolean shouldBuy() {
    return properties.get("shouldBuy") == 1;
  }

  public void setShouldBuy(boolean shouldBuy) {
    properties.put("shouldBuy", shouldBuy == true ? 1.0 : 0.0);
  }

  public boolean shouldSell() {
    return properties.get("shouldSell") == 1;
  }

  public void setShouldSell(boolean shouldSell) {
    properties.put("shouldSell", shouldSell == true ? 1.0 : 0.0);
  }

  public double getProperty(String key) {
    return properties.containsKey(key) ? properties.get(key) : 0;
  }

  public void setProperty(String key, double value) {
    properties.put(key, value);
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public void setOpen(double open) {
    this.open = open;
  }

  public void setHigh(double high) {
    this.high = high;
  }

  public void setLow(double low) {
    this.low = low;
  }

  public void setClose(double close) {
    this.close = close;
  }

  public double getOpen() {

    return open;
  }

  public double getHigh() {
    return high;
  }

  public double getLow() {
    return low;
  }

  public double getClose() {
    return close;
  }

  public HashMap<String, Double> getProperties() {
    return properties;
  }

  public void setProperties(HashMap<String, Double> properties) {
    this.properties = properties;
  }

  //  @Override
//  public String getCSVHeaders() {
//
////    new String[]{"open", "high", "low", "close", "20dma",
////        "50dma", "200dma", "criteria_1", "criteria_2", "criteria_3", "criteria_4",
////        "all_4", "position", "entry_price", "initial_stop_loss",
////        "trailing_stop_loss", "stop_out", "exit_price", "stocks_owned",
////        "stock_value", "cash", "aum";
//
//    return String.join(",",
//        new String[] {"date", "open", "high", "low", "close"}) + "\n" ;
//  }

  @Override
  public String toCSVLine(List<String> headers) {
    StringBuilder stringBuilder = new StringBuilder();
    for (String header : headers) {
      switch (header) {
        case "date":
          stringBuilder.append(date.toString() + ",");
          break;
        case "symbol":
          stringBuilder.append(symbol + ",");
          break;
        case "open":
          stringBuilder.append(open + ",");
          break;
        case "high":
          stringBuilder.append(high + ",");
          break;
        case "low":
          stringBuilder.append(low + ",");
          break;
        case "close":
          stringBuilder.append(close + ",");
          break;
        default:
          stringBuilder.append(properties.get(header) + ",");
      }
    }
    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
    stringBuilder.append("\n");
    return stringBuilder.toString();
  }

//  @Override
//  public PreparedStatement prepareExportSQLStatement(Connection connection) throws SQLException {
//    return null;
//  }

  public Bar copy() {
    Bar clone = new Bar(date, symbol, open, high, low, close);
    return clone;
  }

  @Override
  public PreparedStatement prepareExportSQLStatement(Connection connection, String...foreignKeys) throws
      SQLException {
    String query = "INSERT INTO Bar(" + String.join(",",
        new String[] {"date", "symbol", "open", "high", "low", "close"})
        + ") VALUE(?,?,?,?,?,?)";

    PreparedStatement statement = connection.prepareStatement(query);
    statement.setString(1,date.toString());
    statement.setString(2,symbol);
    statement.setDouble(3,open);
    statement.setDouble(4, high);
    statement.setDouble(5, low);
    statement.setDouble(6, close);
    return statement;
  }

}
