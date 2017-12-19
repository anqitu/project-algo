import java.time.LocalDate;
import java.util.TreeMap;

public class TradeConfig {

  private double stopLoss = 0.2;
  private double movingAvgRange = 0.1;
  private double assetUnderManagement = 1000000;
  private TreeMap<LocalDate, Double> assets = new TreeMap<>();
  private double stockOwned = 0;

  public TradeConfig() {

  }

  public TradeConfig(double stopLoss, double movingAvgRange, double assetUnderManagement,
      double stockOwned) {
    this.stopLoss = stopLoss;
    this.movingAvgRange = movingAvgRange;
    this.assetUnderManagement = assetUnderManagement;
    this.stockOwned = stockOwned;
    assets = null;
  }

  public double getStopLoss() {
    return stopLoss;
  }

  public void setStopLoss(double stopLoss) {
    this.stopLoss = stopLoss;
  }

  public double getAssetUnderManagement() {
    return assetUnderManagement;
  }

  public void setAssetUnderManagement(double assetUnderManagement) {
    this.assetUnderManagement = assetUnderManagement;
  }

  public double getStockOwned() {

    return stockOwned;
  }

  public void setStockOwned(double stockOwned) {
    this.stockOwned = stockOwned;
  }

  public double getMovingAvgRange() {

    return movingAvgRange;
  }

  public void setMovingAvgRange(double movingAvgRange) {
    this.movingAvgRange = movingAvgRange;
  }

  public TreeMap<LocalDate, Double> getAssets() {
    return assets;
  }

  public void addAssets(LocalDate localDate, Double assetUnderManagement) {

    this.assets.put(localDate, assetUnderManagement);
  }
}
