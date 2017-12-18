import java.time.LocalDate;
import java.util.TreeMap;

public class TraderConfig {
    private double stopLoss = 0.2;
    private double movingAvgRange = 0.1;
    private double assetUnderManagement = 1000000;
    private TreeMap<LocalDate, Double> assets = new TreeMap<>();
    private double stockOwned =0;

    public TraderConfig() {
    }

    public TraderConfig(double stopLoss, double movingAvgRange, double assetUnderManagement, double stockOwned) {
        this.stopLoss = stopLoss;
        this.movingAvgRange = movingAvgRange;
        this.assetUnderManagement = assetUnderManagement;
        this.stockOwned = stockOwned;
        assets = null;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public void setAssetUnderManagement(double assetUnderManagement) {
        this.assetUnderManagement = assetUnderManagement;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public double getAssetUnderManagement() {
        return assetUnderManagement;
    }

    public void setMovingAvgRange(double movingAvgRange) {
        this.movingAvgRange = movingAvgRange;
    }

    public void setStockOwned(double stockOwned) {
        this.stockOwned = stockOwned;
    }

    public double getStockOwned() {

        return stockOwned;
    }

    public double getMovingAvgRange() {

        return movingAvgRange;
    }

    public TreeMap<LocalDate, Double> getAssets() {
        return assets;
    }

    public void addAssets(LocalDate localDate, Double assetUnderManagement) {

        this.assets.put(localDate, assetUnderManagement);
    }
}
