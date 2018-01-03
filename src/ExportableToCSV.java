import java.util.List;

public interface ExportableToCSV {

  String toCSVLine(List<String> headers);
}
