import com.ib.client.Contract;

public class Stock {

    private int stockId;
    private String symbol;
    Contract contract = new Contract();

    public Stock() {
    }

    public Stock(int stockId, String symbol){
        this.stockId = stockId;
        this.symbol = symbol;
    }

    public int getStockId() {
        return stockId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setStockId(int stockId) {
        this.stockId = stockId;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void createContract(String ticker){
        contract.symbol(ticker);
        contract.exchange("ARCA");
        contract.secType("STK");
        contract.currency("SGD");
    }


}
