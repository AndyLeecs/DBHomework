import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static int getMin(Date startTime, Date endTime) {
        return (int) (endTime.getTime() - startTime.getTime()) / (60 * 1000);
    }

    public static Date nextMonthFirstDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }
}
