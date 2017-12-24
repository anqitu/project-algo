import java.util.HashMap;

public class Contract {

  private String symbol;
  private com.ib.client.Contract rawContract;
  private HashMap<ContractAttribute, String> properties;

  public Contract(com.ib.client.Contract rawcontract){
    this.rawContract = rawcontract;
    symbol = rawcontract.symbol();
    properties = new HashMap<>();
    properties.put(ContractAttribute.CONTRACT_SYMBOL, rawcontract.symbol());
  }

  public void setContract(com.ib.client.Contract contract) {
    this.rawContract = contract;
  }

  public void setProperties(HashMap<ContractAttribute, String> properties) {
    this.properties = properties;
  }

  public com.ib.client.Contract getContract() {

    return rawContract;
  }

  public HashMap<ContractAttribute, String> getProperties() {
    return properties;
  }

  public String getProperty(ContractAttribute key) {
    return properties.containsKey(key) ? properties.get(key) : null;
  }

  public void setProperty(ContractAttribute key, String value) {
    properties.put(key, value);
  }

}
