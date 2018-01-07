import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DataBaseHandler {

  private String driver = "com.mysql.cj.jdbc.Driver";
  private String url = "jdbc:mysql://localhost/algoDataBase";
  private String username = "root";
  private String password = "";
  private Connection connection;

  public DataBaseHandler() {
    setConnection();
  }

  public DataBaseHandler(String driver, String url, String username, String password) {
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
    setConnection();
  }

  public void setConnection() {
    try
    {
      // Step 1: Load the JDBC ODBC driver
      Class.forName(driver);

      // Step 2: Establish the connection to the database
      this.connection = DriverManager.getConnection(url,username,password);
      System.out.println("Database is successfully connected");
    }
    catch (Exception e)
    {
      System.err.println("Got an exception! ");
      System.err.println(e.getMessage());
    }
  }


  public void insertData(ArrayList<ExportableToDB> exportableToDBs, String...foreignKey) {
    try{
      for (ExportableToDB exportableToDB : exportableToDBs) {
        PreparedStatement statement = exportableToDB.prepareExportSQLStatement(connection, foreignKey);
        statement.execute();
//        System.out.println("Data is successfully added into the table");
      }
//      connection.close();
      System.out.println("Data is successfully added into the table");
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public ArrayList<Contract> readContractDataFromContractTable() {
    System.out.println("Getting contract(companyName not NULL) information from Contract Table...");
    try{
      Statement statement = connection.createStatement();
      String query = "SELECT * FROM Contract WHERE companyName IS NOT NULL";
      ResultSet resultSet = statement.executeQuery(query);
      ArrayList<Contract> contracts= new ArrayList<>();
      while (resultSet.next()){
        Contract contract = new Contract(resultSet.getString("symbol"),
            resultSet.getString("secType"), resultSet.getString("companyName"),
            resultSet.getString("industry"), resultSet.getString("category"));
        contracts.add(contract);
      }
      System.out.println("Finished reading contract information from Contract Table");
      return contracts;
    } catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public ArrayList<Bar> readBarDataFromBarTable() {
    System.out.println("Getting historical bar information from Bar Table...");
    try{
      Statement statement = connection.createStatement();
      String query = "SELECT * FROM Bar";
      ResultSet resultSet = statement.executeQuery(query);
      ArrayList<Bar> bars= new ArrayList<>();
      while (resultSet.next()){
        Bar bar = new Bar(Utilities.parseDate(resultSet.getString("date")),
            resultSet.getString("symbol"),
            resultSet.getDouble("open"), resultSet.getDouble("high"),
            resultSet.getDouble("low"), resultSet.getDouble("close"));
        bars.add(bar);
      }
      System.out.println("Finished reading historical bar information from Bar Table");
      return bars;
    } catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public ArrayList<Bar> readBarDataFromBarTableForContracts(ArrayList<Contract> contracts) {
    System.out.println("Getting historical bar information from Bar Table...");
    try{
      Statement statement = connection.createStatement();
      ArrayList<String> symbols = contracts.stream().map(contract -> contract.getSymbol()).collect(
          Collectors.toCollection(ArrayList::new));
      String joined = "'" + String.join("','", symbols) +"'";
      String query = "SELECT * FROM Bar WHERE symbol IN ( " + joined +" )";
      ResultSet resultSet = statement.executeQuery(query);
      ArrayList<Bar> bars= new ArrayList<>();
      while (resultSet.next()){
        Bar bar = new Bar(Utilities.parseDate(resultSet.getString("date")),
            resultSet.getString("symbol"),
            resultSet.getDouble("open"), resultSet.getDouble("high"),
            resultSet.getDouble("low"), resultSet.getDouble("close"));
        bars.add(bar);
      }
      System.out.println("Finished reading historical bar information from Bar Table");
      return bars;
    } catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public ArrayList<Contract> readContractDataFromBarTable() {
    System.out.println("Getting contract information from Bar Table...");
    ArrayList<String> symbols = new ArrayList<>();
    try {
      Statement statement = connection.createStatement();
      String query = "SELECT DISTINCT symbol FROM Bar";
      ResultSet resultSet = statement.executeQuery(query);
      while (resultSet.next()) {
        symbols.add(resultSet.getString("symbol"));
      }
    } catch (Exception e){
      e.printStackTrace();
    }

    ArrayList<Contract> contracts= new ArrayList<>();
    try{
      Statement statement1 = connection.createStatement();
      String joined = "'" + String.join("','", symbols) +"'";
      String query1 = "SELECT * FROM Contract WHERE symbol IN (" + joined +")";
      ResultSet resultSet1 = statement1.executeQuery(query1);
      while (resultSet1.next()) {
        Contract contract = new Contract(resultSet1.getString("symbol"),
            resultSet1.getString("secType"), resultSet1.getString("companyName"),
            resultSet1.getString("industry"), resultSet1.getString("category"));
        contracts.add(contract);
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    System.out.println("Finished reading contract information from Bar Table");
    return contracts;
  }

  public HashMap<String, LocalDate> getFirstBarDate(){
    HashMap<String, LocalDate> contractEarliestBarDates = new HashMap<>();
    try{
      Statement statement = connection.createStatement();
      String query = "SELECT MIN(date), symbol FROM Bar GROUP BY symbol";
      ResultSet resultSet = statement.executeQuery(query);
      while (resultSet.next()){
        contractEarliestBarDates.put(resultSet.getString("symbol"),
            LocalDate.parse(resultSet.getString(1), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    System.out.println("Finished reading contract first bar date information");
    return contractEarliestBarDates;
  }

  public HashMap<String, LocalDate> getLastBarDate(){
    HashMap<String, LocalDate> contractLastBarDates = new HashMap<>();
    try{
      Statement statement = connection.createStatement();
      String query = "SELECT MAX(date), symbol FROM Bar GROUP BY symbol";
      ResultSet resultSet = statement.executeQuery(query);
      while (resultSet.next()){
        contractLastBarDates.put(resultSet.getString("symbol"),
            LocalDate.parse(resultSet.getString(1), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    System.out.println("Finished reading contract last bar date information");
    return contractLastBarDates;
  }

  public ArrayList<LocalDate> getAllDates() {
    ArrayList<LocalDate> dates = new ArrayList<>();
    try{
      Statement statement = connection.createStatement();
      String query = "SELECT DISTINCT date FROM Bar";
      ResultSet resultSet = statement.executeQuery(query);
      while (resultSet.next()){
        dates.add(LocalDate.parse(resultSet.getString(1), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    System.out.println("Finished reading all dates information");
    return dates;
  }
}
