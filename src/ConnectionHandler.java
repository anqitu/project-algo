import com.ib.controller.ApiController.IConnectionHandler;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ConnectionHandler implements IConnectionHandler {

  private Semaphore semaphore = new Semaphore(0);

  public boolean isConnected() {
    return this.semaphore.availablePermits() != 0;
  }

  public void waitForConnection() {
    try {
      this.semaphore.acquire();
    } catch (InterruptedException ex) {
    }
  }

  @Override
  public void connected() {
    semaphore.release(Integer.MAX_VALUE);
  }

  @Override
  public void disconnected() {
    semaphore.drainPermits();
  }

  @Override
  public void accountList(List<String> list) {
    System.out.println("Accounts: " + String.join(", ", list));
  }

  @Override
  public void error(Exception e) {
    System.out.println("ERROR");
    e.printStackTrace();
  }

  @Override
  public void message(int i, int i1, String s) {
//    System.out.println("MESSAGE");
//    System.out.println(String.format("%d %d %s", i, i1, s));
  }

  @Override
  public void show(String s) {
    System.out.println("SHOW");
    System.out.println(s);
  }
}
