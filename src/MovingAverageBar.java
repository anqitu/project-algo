import com.ib.client.Contract;

import java.util.Date;

public class MovingAverageBar extends com.ib.client.Bar {
    private double movingAvg20;
    private double movingAvg50;
    private double movingAvg200;
    private double movingAvgRange;

    public MovingAverageBar(String s, double v, double v1, double v2, double v3, long l, int i, double v4) {
        super(s, v, v1, v2, v3, l, i, v4);
    }

    public MovingAverageBar(String s, double v, double v1, double v2, double v3, long l, int i, double v4,
                            double movingAvg20, double movingAvg50, double movingAvg200, double movingAvgRange) {
        super(s, v, v1, v2, v3, l, i, v4);
        this.movingAvg20 = movingAvg20;
        this.movingAvg50 = movingAvg50;
        this.movingAvg200 = movingAvg200;
        this.movingAvgRange = movingAvgRange;
    }

    public void setMovingAvg20(double movingAvg20) {
        this.movingAvg20 = movingAvg20;
    }

    public void setMovingAvg50(double movingAvg50) {
        this.movingAvg50 = movingAvg50;
    }

    public void setMovingAvg200(double movingAvg200) {
        this.movingAvg200 = movingAvg200;
    }

    public double getMovingAvg20() {
        return movingAvg20;
    }

    public double getMovingAvg50() {
        return movingAvg50;
    }

    public double getMovingAvg200() {
        return movingAvg200;
    }

    public double getMovingAvgRange() {
        return movingAvgRange;
    }

    public void setMovingAvgRange(double movingAvgRange) {

        this.movingAvgRange = movingAvgRange;
    }
}
