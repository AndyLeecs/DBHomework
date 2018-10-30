import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatUtil {
    public static String format(Date date)
    {
        SimpleDateFormat df = getDayFormat();
        return df.format(date);
    }

    public static String format(double d) {
        DecimalFormat df = new DecimalFormat("#.00");
        return df.format(d);
    }

    private static SimpleDateFormat getDayFormat()
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
