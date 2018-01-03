import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public interface ExportableToDB {
  PreparedStatement prepareExportSQLStatement(Connection connection, String...foreignKeys) throws SQLException;
//  ExportableToDB getExportableToDBs(ResultSet resultSet) throws SQLException;

}
