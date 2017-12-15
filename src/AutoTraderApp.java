import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EReader;
import com.ib.client.EReaderSignal;

public class AutoTraderApp implements ConnectionListener {

  private AutoTrader autoTrader;

  @Override
  public void connectionEstablished() {
    autoTrader.initSimulation("", "1 D");
  }

  public AutoTraderApp(ConnectionConfig connectionConfig, Contract contract) {
    autoTrader = new AutoTrader(connectionConfig, contract);
    autoTrader.setConnectionListener(this);
  }

  public void start() {

    try {

      EClientSocket clientSocket = autoTrader.getClientSocket();
      EReaderSignal readerSignal = autoTrader.getReaderSignal();
      EReader reader = new EReader(clientSocket, readerSignal);

      // Establish connection
      autoTrader.connect();

      // Keep client alive
      new Thread(() -> {
        while (clientSocket.isConnected()) {
          readerSignal.waitForSignal();
          try {
            reader.processMsgs();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }).start();

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void shutdown() {
    System.out.println("Disconnecting...");
    autoTrader.getClientSocket().eDisconnect();
  }

  public static void main(String[] args) {

    ConnectionConfig connectionConfig = new ConnectionConfig();
    connectionConfig.setAccount("ccas0000");
    connectionConfig.setClientId(1);
    connectionConfig.setHost("127.0.0.1");
    connectionConfig.setSocketPort(4002);

    Contract nkeContract = new Contract();
    nkeContract.symbol("NKE");
    nkeContract.secType("STK");
    nkeContract.currency("USD");
    nkeContract.exchange("SMART");

    AutoTraderApp autoTraderApp = new AutoTraderApp(connectionConfig, nkeContract);
    autoTraderApp.start();

    // Shutdown gracefully
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      autoTraderApp.shutdown();
    }));
  }
}
