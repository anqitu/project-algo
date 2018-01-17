import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class BarProperty implements ExportableToDB {

  private String symbol;
  private LocalDate date;
  private String propertyName;
  private Double propertyValue;

  public BarProperty(String symbol, LocalDate date, String propertyName,
      Double propertyValue) {
    this.symbol = symbol;
    this.date = date;
    this.propertyName = propertyName;
    this.propertyValue = propertyValue;
  }

  @Override
  public PreparedStatement prepareExportSQLStatement(Connection connection, String... foreignKeys)
      throws SQLException {
    String query = "INSERT INTO BarProperty(" + String.join(",",
        new String[] { "symbol", "date", "barPropertyName", "barPropertyValue"})
        + ") VALUE(?,?,?,?)";

    PreparedStatement statement = connection.prepareStatement(query);
    statement.setString(1,symbol);
    statement.setString(2,date.toString());
    statement.setString(3,propertyName);
    statement.setDouble(4,propertyValue);
    return statement;
  }



}
