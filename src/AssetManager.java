import com.ib.client.Contract;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.TreeMap;

public class AssetManager {

  private double residualAssets;
  private HashMap<Contract, Integer> stocksOwned;

  public AssetManager(double assetsUnderManagement) {
    residualAssets = assetsUnderManagement;
    stocksOwned = new HashMap<>();
  }

  public double getResidualAssets() {
    return residualAssets;
  }

  public Contract[] getContracts() {
    return stocksOwned.keySet().stream().toArray(Contract[]::new);
  }

  public int getOwnedStocks(Contract contract) {
    return stocksOwned.containsKey(contract) ? stocksOwned.get(contract) : 0;
  }

  public void setResidualAssets(double residualAssets) {
    this.residualAssets = residualAssets;
  }

  public double getTotalAssetValue(HashMap<Contract, TreeMap<LocalDate, Bar>> marketData) {
    return getResidualAssets() + stocksOwned.keySet().stream().mapToDouble(contract ->
            marketData.get(contract).lastEntry().getValue().getClose() *
                stocksOwned.get(contract)).sum();
  }

  public void setOwnedStocks(Contract contract, int amount) {
    if (amount == 0)
      stocksOwned.remove(contract);
    else
      stocksOwned.put(contract, amount);
  }
}
