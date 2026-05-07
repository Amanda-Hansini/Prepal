import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Test {
    public static void main(String[] args) throws Exception {
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Date parsedTime = timeFormat.parse("05:00 PM");
        Calendar timeCalendar = Calendar.getInstance();
        timeCalendar.setTime(parsedTime);
        System.out.println("Hour: " + timeCalendar.get(Calendar.HOUR_OF_DAY));
        System.out.println("Minute: " + timeCalendar.get(Calendar.MINUTE));
    }
}
