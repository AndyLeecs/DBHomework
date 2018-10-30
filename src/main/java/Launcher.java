import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

public class Launcher {
    public static void main(String args[])
    {

        try {
            MobileOperator mo = new MobileOperator();
            long start;
            long end;
            System.out.println("1.1 对某个用户进行套餐的查询（包括历史记录）");
            start = System.currentTimeMillis();
            mo.findAllByPhoneNum("16161293774");
            end = System.currentTimeMillis();
            System.out.println("End time : " + (end - start) + " ms");
//
            System.out.println("1.2 某用户订购套餐");
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2018);
            cal.set(Calendar.MONTH, 10 );
            cal.set(Calendar.DATE, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            Date start_time = cal.getTime();
            cal.set(Calendar.MONTH, 11);
            cal.set(Calendar.DATE, 1);
            Date end_time = cal.getTime();
            start = System.currentTimeMillis();
            mo.addPlan("16124716550", 1, start_time, end_time);
            end = System.currentTimeMillis();
            System.out.println("End time : " + (end - start) + " ms");

            System.out.println("1.3 用户退订次月生效");
            start = System.currentTimeMillis();
            mo.deletePlanNextMonth("16124716550", 2);
            end = System.currentTimeMillis();
            System.out.println("End time : " + (end - start) + " ms");

            System.out.println("2 某个用户在通话情况下的资费生成");
            cal.set(Calendar.YEAR, 2018);
            cal.set(Calendar.MONTH, 10);
            cal.set(Calendar.DATE, 31);
            cal.set(Calendar.HOUR_OF_DAY, 15);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            start_time = cal.getTime();
            cal.set(Calendar.MINUTE, 35);
            end_time = cal.getTime();
            start = System.currentTimeMillis();
            mo.generateCallFee("16071003792", "17766102617",start_time, end_time,true);
            end = System.currentTimeMillis();
            System.out.println("End time : " + (end - start) + " ms");

            System.out.println("3 某个用户在使用流量情况下的资费生成");
            start = System.currentTimeMillis();
            mo.generateDataFee("16071003792", start_time, end_time ,80,"黑龙江省");
            end = System.currentTimeMillis();
            System.out.println("End time : " + (end - start) + " ms");

            System.out.println("4 某个用户月账单的生成");
            start = System.currentTimeMillis();
            mo.generateBill("16071003792");
            end = System.currentTimeMillis();
            System.out.println("End time : " + (end - start) + " ms");
        }catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
