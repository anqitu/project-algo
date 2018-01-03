import com.ib.client.ContractDetails;
import com.ib.client.Types.SecType;
import com.ib.contracts.StkContract;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Contract implements ExportableToCSV, ExportableToDB {

  private String symbol;
  private String secType;
  private String companyName;
  private String industry;
  private String category;
//  private String validExchanges;
//  private String underSymbol;
//  private String underConID;
//  private String contractMonth;
//  private String realExpirationDate;

  public Contract() {
  }

//  public Contract(com.ib.client.Contract rawcontract){
//    symbol = rawcontract.symbol();
//    this.secType = rawcontract.getSecType();
//  }

  public Contract(String symbol){
    this.symbol = symbol;
    this.secType = SecType.STK.toString();
  }

  public Contract(String symbol, String secType, String companyName, String industry,
      String category) {
    this.symbol = symbol;
    this.secType = secType;
    this.companyName = companyName;
    this.industry = industry;
    this.category = category;
  }

  public com.ib.client.Contract getRawContract(){
    return new StkContract(symbol);
  }

  public void setContractDetails(ContractDetails contractDetails) {
    if(contractDetails != null){
      this.companyName = contractDetails.longName().replace(" ", "_");
      this.industry = contractDetails.industry() == null? null: contractDetails.industry().split(",")[0];
      this.category = contractDetails.category();
//      this.validExchanges = contractDetails.validExchanges();
//      this.underSymbol = contractDetails.underSymbol();
//      this.underConID = String.valueOf(contractDetails.underConid());
//      this.contractMonth = contractDetails.contractMonth();
//      this.realExpirationDate = contractDetails.realExpirationDate();
    }
  }


  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public void setSecType(String secType) {
    this.secType = secType;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public void setIndustry(String industry1) {
    this.industry = industry1;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getSymbol() {
    return symbol;
  }

  public String getSecType() {
    return secType;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getIndustry() {
    return industry;
  }

  public String getCategory() {
    return category;
  }

//  @Override
//  public String getCSVHeaders() {
//    return String.join(",",
//        new String[] {"symbol", "secType", "companyName", "industry", "category"}) + "\n" ;
//  }

//  @Override
//  public String toCSVLine() {
//    return String.join(",",
//        new String[] {symbol, String.valueOf(secType), companyName, industry, category}) + "\n";
//  }

  @Override
  public String toCSVLine(List<String> headers) {
    StringBuilder stringBuilder = new StringBuilder();
    for (String header : headers) {
      switch (header) {
        case "symbol":
          stringBuilder.append(symbol + ",");
          break;
        case "secType":
          stringBuilder.append(secType + ",");
          break;
        case "companyName":
          stringBuilder.append(industry + ",");
          break;
        case "industry":
          stringBuilder.append(industry + ",");
          break;
        case "category":
          stringBuilder.append(category + ",");
          break;
        default:
          System.out.println("Wrong Header: " + header);
      }
    }
    stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
    stringBuilder.append("\n");

    return stringBuilder.toString();
  }

  public Contract copy() {
    Contract copy =  new Contract();
    copy.setSymbol(symbol);
    copy.setSecType(secType);
    copy.setCompanyName(companyName);
    copy.setIndustry(industry);
    copy.setCategory(category);
    return copy;
  }

  @Override
  public PreparedStatement prepareExportSQLStatement(Connection connection, String...foreignKeys) throws SQLException {
    String query = "INSERT INTO Contract(" + String.join(",",
        new String[] {"symbol", "secType", "companyName", "industry", "category"})
        + ") VALUE(?,?,?,?,?)";

    PreparedStatement statement = connection.prepareStatement(query);
    statement.setString(1,symbol);
    statement.setString(2,String.valueOf(secType));
    statement.setString(3,companyName);
    statement.setString(4,industry);
    statement.setString(5,category);
    return statement;
  }

  public Contract getExportableToDBs(ResultSet resultSet) throws SQLException {
    Contract contract = new Contract();
    contract.setSymbol(resultSet.getString(1));
    contract.setSecType(resultSet.getString(2));
    contract.setCompanyName(resultSet.getString(3));
    contract.setIndustry(resultSet.getString(4));
    contract.setCategory(resultSet.getString(5));
    return contract;
  }

  @Override
  public boolean equals(Object obj) {
    Contract contract = (Contract) obj;
    return contract.symbol.equals(this.symbol);
  }
}
