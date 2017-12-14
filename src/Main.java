import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws InterruptedException, ClassNotFoundException, SQLException {
        OrderManagement order = new OrderManagement();
        order.sendMarketOrder("AAPL", "BUY");

    }
}
