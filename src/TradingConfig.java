import com.ib.client.Contract;
import java.util.HashMap;

public class TradingConfig {

  private HashMap<Contract, Double> stopLoss;
  private double movingAverageRange;
  private double assetUnderManagement;

  public TradingConfig(double movingAverageRange,
      double assetUnderManagement) {

    if (movingAverageRange < 0 || movingAverageRange > 1.0 || assetUnderManagement < 0)
      throw new IllegalArgumentException();

    this.stopLoss = new HashMap<>();
    this.movingAverageRange = movingAverageRange;
    this.assetUnderManagement = assetUnderManagement;
  }

  public double getStopLoss(Contract contract) {
    return stopLoss.get(contract);
  }

  public double getMovingAverageRange() {
    return movingAverageRange;
  }

  public double getAssetUnderManagement() {
    return assetUnderManagement;
  }

  public void setStopLoss(Contract contract, double value) {
    if (contract == null || value > 1.0 || value < 0)
      throw new IllegalArgumentException();
    stopLoss.put(contract, value);
  }

  public void setMovingAverageRange(double movingAverageRange) {
    this.movingAverageRange = movingAverageRange;
  }

  public void setAssetUnderManagement(double assetUnderManagement) {
    this.assetUnderManagement = assetUnderManagement;
  }
}
