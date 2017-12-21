import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Utilities {

  public static LocalDate parseDate(String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    return LocalDate.parse(date, formatter);
  }

  public static String formatDate(LocalDate date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    return date.format(formatter);

  }

  public static LocalDate formatDate(long date){

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    return Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
