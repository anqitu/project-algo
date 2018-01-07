import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class BarProperty implements ExportableToDB {

  private String symbol;
  private LocalDate date;
  private String propertyName;
  private String propertyValue;

  public BarProperty(String symbol, LocalDate date, String propertyName,
      String propertyValue) {
    this.symbol = symbol;
    this.date = date;
    this.propertyName = propertyName;
    this.propertyValue = propertyValue;
  }

  @Override
  public PreparedStatement prepareExportSQLStatement(Connection connection, String... foreignKeys)
      throws SQLException {
    return null;
  }



}
