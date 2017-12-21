import com.ib.client.Contract;
import com.ib.contracts.StkContract;

public class AutoTraderApp {

  public static void main(String[] args) {
    AutoTrader autoTrader = new AutoTrader();
//    autoTrader.run();

    for (String stock : new String[]{"NKE", "AAPL", "JNJ", "WMT", "DWDP"})
      autoTrader.exportHistoricalData(new StkContract(stock), stock + ".csv");
  }

}