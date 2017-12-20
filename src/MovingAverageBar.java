import com.ib.controller.Bar;
import java.util.HashMap;

public class MovingAverageBar {

  // <Days, Moving Average>
  private HashMap<Integer, Double> movingAverage;
  private Bar bar;

  public MovingAverageBar(Bar bar) {
    this.bar = bar;
    movingAverage.put(20, 0.0);
    movingAverage.put(50, 0.0);
    movingAverage.put(200, 0.0);
  }
}
