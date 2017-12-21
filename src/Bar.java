import java.time.LocalDate;
import java.util.HashMap;

public class Bar {

  private LocalDate date;
  private com.ib.controller.Bar bar;

  private HashMap<String, Double> properties;

  public Bar(com.ib.controller.Bar bar) {
    date = LocalDate.ofEpochDay(bar.time());
    this.bar = bar;
    properties = new HashMap<>();
    properties.put("high", bar.high());
    properties.put("low", bar.low());
    properties.put("open", bar.open());
    properties.put("close", bar.close());
  }

  public LocalDate getDate() {
    return date;
  }

  public double getHigh() {
    return bar.high();
  }

  public double getLow() {
    return bar.low();
  }

  public double getOpen() {
    return bar.open();
  }

  public double getClose() {
    return bar.close();
  }

  public com.ib.controller.Bar getBar() {
    return bar;
  }

  public double getProperty(String key) {
    return properties.containsKey(key) ? properties.get(key) : 0;
  }

  public void setProperty(String key, double value) {
    properties.put(key, value);
  }

}
