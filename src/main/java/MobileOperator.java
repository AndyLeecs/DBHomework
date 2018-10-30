import javax.management.Query;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.text.SimpleDateFormat;

public class MobileOperator {
    private Connection conn;

    public MobileOperator(){
        try {
            conn = DBConnector.getNoAutoCommitConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 1.1 对某个用户进行套餐的查询（包括历史记录）
     */
    public void findAllByPhoneNum(final String phoneNum) throws SQLException
    {

        //本月各项总额
        int call_mins_total = 0;
        int texts_total = 0;
        int local_data_total = 0;
        int nation_data_total = 0;

        String sql = "select plan.id, plan.name, call_mins, texts, local_data_gb, nation_data_gb, start_time, end_time " +
        "from user_plan_ref join plan on user_plan_ref.plan = plan.id where user = " + phoneNum
                + " order by end_time";
//        String sql = "select 5plan.name, start_time, end_time " +
//                "from user_plan_ref join plan on user_plan_ref.plan = plan.id where user = " + phoneNum
//                + " order by end_time";
        ResultSet rs = QueryUtil.executeQuery(conn, sql);
        System.out.println("---已订购订单情况查看---");
        ArrayList<Plan> currentPlans = new ArrayList<Plan>();
        while (rs.next())
        {
            System.out.print(rs.getString("plan.name")+" ");
            System.out.print(rs.getDate("start_time")+" "+rs.getTime("start_time")+" ");
            System.out.print(rs.getDate("end_time")+" " + rs.getTime("end_time")+" ");
            System.out.println();
//
            //加入本月订单列表
            Timestamp now = new Timestamp(System.currentTimeMillis());
            if(rs.getTimestamp("start_time").before(now)
                    && rs.getTimestamp("end_time").after(now)){
                Plan p = new Plan(rs.getInt("plan.id"),
                        rs.getString("plan.name"),
                        rs.getInt("call_mins"),
                        rs.getInt("texts"),
                        rs.getDouble("local_data_gb"),
                        rs.getDouble("nation_data_gb")
                );
                currentPlans.add(p);
                call_mins_total += p.call_mins;
                texts_total += p.texts;
                local_data_total += p.local_data;
                nation_data_total += p.nation_data;
            }
        }


        String monthConditionSql = "select call_mins, texts, local_data, nation_data from user where phone_num = " + phoneNum;
        rs = QueryUtil.executeQuery(conn, monthConditionSql);
        System.out.println("---当月各项剩余情况---");
        int call_mins_rem = 0;
        int texts_rem = 0;
        int local_data_rem = 0;
        int nation_data_rem = 0;
        while (rs.next())
        {
            call_mins_rem = rs.getInt("call_mins");
            System.out.print("通话时间" + call_mins_rem+"分钟 ");
            texts_rem = rs.getInt("texts");
            System.out.print("短信数量" +texts_rem +"条 ");
            local_data_rem = rs.getInt("local_data");
            System.out.print("本地流量" +local_data_rem+"KB ");
            nation_data_rem = rs.getInt("nation_data");
            System.out.print("全国流量" + nation_data_rem + "KB");
            System.out.println();
        }

        call_mins_total -= call_mins_rem;
        texts_total -= texts_rem;
        local_data_total -= local_data_rem;
        nation_data_total -= nation_data_rem;

       System.out.println("---当月套餐使用情况---"); //计算逻辑为优先使用该项数额小的套餐
        System.out.println("---通话时间使用情况---");
        currentPlans.sort(new Comparator<Plan>() {
            public int compare(Plan o1, Plan o2) {
                return o1.call_mins - o2.call_mins;
            }
        });

        for (Plan currentPlan:currentPlans) {
            int cur = currentPlan.call_mins;
            if(cur != 0)
            {
                System.out.print(currentPlan.name+" ");
                if(cur <= call_mins_total )
                {
                    System.out.println("100%");
                    call_mins_total -= cur;
                }
                else
                {
                    System.out.println(call_mins_total/cur + "%");
                }
            }
        }

        System.out.println("---短信数量使用情况---");
        currentPlans.sort(new Comparator<Plan>() {
            public int compare(Plan o1, Plan o2) {
                return o1.texts - o2.texts;
            }
        });

        for (Plan currentPlan:currentPlans) {
            int cur = currentPlan.texts;
            if(cur != 0)
            {
                System.out.print(currentPlan.name+" ");
                if(cur <= texts_total )
                {
                    System.out.println("100%");
                    texts_total -= cur;
                }
                else
                {
                    System.out.println(texts_total/cur + "%");
                }
            }
        }

        System.out.println("---本地流量使用情况---");
        currentPlans.sort(new Comparator<Plan>() {
            public int compare(Plan o1, Plan o2) {
                return o1.local_data - o2.local_data;
            }
        });

        for (Plan currentPlan:currentPlans) {
            int cur = currentPlan.local_data;
            if(cur != 0)
            {
                System.out.print(currentPlan.name+" ");
                if(cur <= local_data_total )
                {
                    System.out.println("100%");
                    local_data_total -= cur;
                }
                else
                {
                    System.out.println(local_data_total/cur + "%");
                }
            }
        }


        System.out.println("---国内流量使用情况---");
        currentPlans.sort(new Comparator<Plan>() {
            public int compare(Plan o1, Plan o2) {
                return o1.nation_data - o2.nation_data;
            }
        });

        for (Plan currentPlan:currentPlans) {
            int cur = currentPlan.nation_data;
            if(cur != 0)
            {
                System.out.print(currentPlan.name+" ");
                if(cur <= nation_data_total )
                {
                    System.out.println("100%");
                    nation_data_total -= cur;
                }
                else
                {
                    System.out.println(nation_data_total*100/cur + "%");
                }
            }
        }

    }

    /**
     * 1.2 某用户订购套餐
     * 默认start_time和end_time为某月月初
     */
    public void addPlan(String phoneNum, int planId , Date start_time, Date end_time) throws SQLException
    {
        String startDateTime = FormatUtil.format(start_time);
        String endDateTime = FormatUtil.format(end_time);
        assert start_time.before(end_time);
        //查看是否重复订购
        String selectSql = "select start_time, end_time from user_plan_ref where user = \""+phoneNum + "\" and plan = " + planId
                +" and (UNIX_TIMESTAMP(end_time) - UNIX_TIMESTAMP(\"" + startDateTime + "\")) > 0";
        ResultSet rs = QueryUtil.executeQuery(conn, selectSql);
        if(rs.next()){
            rs.previous();
            while(rs.next())
            {
                String start = rs.getDate("start_time")+" "+ rs.getTime("start_time");
                String end = rs.getDate("end_time")+" "+rs.getTime("end_time");
                System.out.println("之前已订购， 时间段为"+start+" 到 "+end);
            }
            return;
        }

//        //查看用户是否还有钱订购
//        double balance = 0;
//        String checkBalanceSql = "select balance from user where phone_num = "+phoneNum;
//        rs = executeQuery(checkBalanceSql);
//        assert rs.getFetchSize() == 1;
//        while(rs.next())
//        {
//            balance = rs.getDouble("balance");
//        }
//        double base_fee = 0;
//        String checkBaseFeeSql = "select base_fee from plan where id = "+planId;
//        rs = executeQuery(checkBaseFeeSql);
//        assert rs.getFetchSize() == 1;
//        while (rs.next())
//        {
//            base_fee = rs.getDouble("base_fee");
//        }
//        if(balance < base_fee)
//        {
//            System.out.println("请先充值， 您的余额为 "+balance+" 元， 套餐月功能费为 " + base_fee + " 元");
//            return;
//        }

        //未订购
        String addPlanSql = "insert into user_plan_ref (user, plan, start_time, end_time) value (" +
                "\"" + phoneNum + "\"," +
                "\"" + planId + "\"," +
                "\"" + startDateTime + "\"," +
                "\"" + endDateTime + "\"" +
                ")";
//        String minusMoneySql = "UPDATE user SET balance = " + balance +
//                " WHERE phone_num = " + phoneNum;
//        executeSQLs(new String[]{addPlanSql, minusMoneySql});
        QueryUtil.executeSQL(conn, addPlanSql);
        System.out.println("成功订购");
    }

    private Date nextMonthFirstDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    private Date afterNextMonthFirstDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, 2);
        return calendar.getTime();
    }

   //以下默认能被选择删除的planId都是用户已经订阅的plan，不再做check

    /**
     * 1.3 用户退订立即生效
     * 默认能够选择的planId都是用户正在用的plan
     */
    public void deletePlanImmediate(String phoneNum, int planId)
    {
//        String checkPlanDetailSql =
//        String addBalance =
//        Date newEndDate = new Date();
//        String deletePlanSql = "update user_plan_ref set end_time = " + newEndDate +" where user = "+phoneNum + " and plan = " + planId
//                +" and end_time > " + newEndDate;
    }

    /**
     * 1.4 用户退订次月生效
     * 默认能被选择的planId都是用户已经订阅的plan，且未生效的plan不能退订
     */
    public void deletePlanNextMonth(String phoneNum, int planId)
    {
        Date newEndDate = nextMonthFirstDate();
        String sql = "update user_plan_ref set end_time = \""
                + FormatUtil.format(newEndDate) +"\" where user = "+phoneNum + " and plan = " + planId
                +" and end_time > \"" + FormatUtil.format(newEndDate) + "\"";
        QueryUtil.executeSQL(conn, sql);
        System.out.println("已退订");
    }

    //对于以下资费生成，默认startTime和endTime在同一个月，如果出现跨月，应当在跨月前调用一次该过程，跨月后再调用一次该过程

    /**
     * 2 某个用户在通话情况下的资费生成
     */
    public void generateCallFee(final String phoneNum, final String contact, final Date startTime, final Date endTime, boolean isCaller)  throws SQLException
    {
//        System.out.println(format(endTime));
        //判断是否跨月
//        if(isDiffMonth(startTime, endTime) && !getFirstDateInMonth(endTime).equals(endTime))
//        {
//            generateCallFee(phoneNum, contact, startTime, getLastDateInMonth(startTime), isCaller);
//            generateCallFee(phoneNum, contact, getFirstDateInMonth(endTime), endTime, isCaller);
//        }
        double fee = 0;
        int call_mins_remain = 0;
        double balance = 0;
        if(isCaller)
        {
            //获取用户的剩余可用拨打时间
            int call_mins = getMin(startTime, endTime);
            ResultSet rs = QueryUtil.executeQuery(conn,"select call_mins, balance from user where phone_num = " + phoneNum);
            assert rs.getFetchSize() == 1;
            while (rs.next()) {
                call_mins_remain = rs.getInt("call_mins");
                balance = rs.getDouble("balance");
            }

//            System.out.println("call_mins" + call_mins);
//            System.out.println("call_mins_remain" + call_mins_remain);
//            System.out.println("balance" + balance);
            //检查剩余可用拨打时间是否足够
            if(call_mins < call_mins_remain)
            {
                //如果足够,扣时间
                call_mins_remain -= call_mins;
            }
            else
            {
                //如果不够，扣时间，扣钱
                call_mins_remain = 0;
                int mins_to_pay = call_mins - call_mins_remain;
                fee = getCallFee(mins_to_pay, Rule.CALL);
                balance = balance - fee;
//                System.out.println("fee"+fee);
            }
        }
//         System.out.println(format(startTime));
//         System.out.println(format(endTime));
            String addDetailSql = "insert into call_detail (user, fee, contact, start_time, end_time, iscaller) value ("+
                    "\"" + phoneNum + "\"," +
                    "\"" + fee + "\"," +
                    "\"" + contact + "\"," +
                    "\"" + FormatUtil.format(startTime) + "\"," +
                    "\"" + FormatUtil.format(endTime) + "\"," +
                    isCaller +
                    ")";
//            String minusTimeSql = "UPDATE user SET call_mins = " + call_mins_remain +
//                    " WHERE phone_num = " + phoneNum;
            String minusTimeAndMoneySql = "UPDATE user SET call_mins = " + call_mins_remain + " , balance = " + balance +
                    " WHERE phone_num = " + phoneNum;
        //如果是被叫，只加一条通话记录
        if(!isCaller) {
            QueryUtil.executeSQL(conn, addDetailSql);
            System.out.println("添加被叫记录");
        }
        //否则，更改用户的个人信息
        else {
            executeSQLs(new String[]{addDetailSql, minusTimeAndMoneySql});
            System.out.println("添加主叫记录并更改个人信息");
        }

        checkBalance(balance);
    }

    /**
     * 3 某个用户在使用流量情况下的资费生成
     */
    public void generateDataFee(final String phoneNum, final Date startTime, final Date endTime, final int data, final String fromWhere) throws SQLException
    {
//            //判断是否跨月
//            if(isDiffMonth(startTime, endTime) && !getFirstDateInMonth(endTime).equals(endTime))
//            {
//                generateDataFee(phoneNum, startTime, getLastDateInMonth(startTime), data, fromWhere);
//                generateDataFee(phoneNum, getFirstDateInMonth(endTime), endTime, data, fromWhere);
//            }

            int duration = getMin(startTime, endTime);
            double fee = 0;

            //获取用户个人信息
            double balance = 0;
            int local_data = 0;
            int nation_data = 0;
            String province = "";
            ResultSet rs = QueryUtil.executeQuery(conn, "select local_data, nation_data, balance, province " +
                    "from user where phone_num = " + phoneNum);
            assert rs.getFetchSize() == 1;
            while (rs.next()){
                local_data = rs.getInt("local_data");
                nation_data = rs.getInt("nation_data");
                balance = rs.getDouble("balance");
                province = rs.getString("province");
            }
//            System.out.println("balance_before_change" + balance);
            boolean isLocal = false;
            if(province.equals(fromWhere))
                isLocal = true;
            //如果使用全国流量
            if(!isLocal)
            {
                //检查剩余全国流量是否足够
                //如果足够，扣流量
                if(data < nation_data){
                    nation_data -= data;
                }
                //如果不够，扣流量，扣钱
                else
                {
                    int data_to_pay = data - nation_data;
                    nation_data = 0;
//                    System.out.println("data_to_pay"+data_to_pay);
                    fee = getDataFee(data_to_pay, Rule.NATION_DATA);
                    balance = balance - fee;
                }
            }
            //如果使用本地流量
            else
            {
                //检查剩余本地流量是否足够
                //如果足够，扣流量
                if(data < local_data){
                    local_data-=data;
                }
                //如果不够，扣本地流量，并检查剩余全国流量是否足够
                else
                {
                    int data_to_check_in_nation_data = data - local_data;
                    local_data = 0;
                    //如果剩余全国流量足够,扣全国流量
                    if(data_to_check_in_nation_data < nation_data)
                    {
                        nation_data -= data_to_check_in_nation_data;
                    }
                    //如果剩余全国流量不够,扣全国流量，扣钱
                    else
                    {
                        int data_to_pay = data_to_check_in_nation_data - nation_data;
                        nation_data = 0;
                        fee = getDataFee(data_to_pay, Rule.LOCAL_DATA);
                        balance = balance - fee;
                    }
                }
            }
//        System.out.println("balance_after_change" + format(balance));
        String addDetailSql = "insert into data_detail (user, fee, start_time, end_time, data, islocal) value (" +
                    "\"" + phoneNum + "\"," +
                    "\"" + fee + "\"," +
                    "\"" + FormatUtil.format(startTime) + "\"," +
                    "\"" + FormatUtil.format(endTime) + "\"," +
                    "\"" + data + "\","+
                     isLocal +
                    ")";
        String minusDataAndMoneySql = "UPDATE user SET local_data = " + local_data + " , nation_data = "
                +nation_data+" , balance = " + FormatUtil.format(balance) +
                " WHERE phone_num = " + phoneNum;
        executeSQLs(new String[]{addDetailSql, minusDataAndMoneySql});
        System.out.println("已添加流量记录");
        checkBalance(balance);
    }

    /**
     * 4 某个用户月账单的生成
     */
    public void generateBill(final String phoneNum) throws SQLException
    {
        double balance = 0;
        ResultSet rs = QueryUtil.executeQuery(conn,"select balance from user where phone_num = " + phoneNum);
        assert rs.getFetchSize() == 1;
        while (rs.next()){
            balance = rs.getDouble("balance");
        }
        System.out.println("余额 "+ balance);
        String sameMonthCriteria = " and date_format(start_time,'%Y-%m')=date_format(now(),'%Y-%m')";
        String whereCriteria = " where user = "+phoneNum + sameMonthCriteria + " order by start_time";
        System.out.println("---通话情况---");
        System.out.println("资费 联系人 开始时间 结束时间 是否为主叫");
        rs = QueryUtil.executeQuery(conn,"select fee, contact, start_time, end_time, iscaller from call_detail " + whereCriteria);
        while (rs.next())
        {
            System.out.print(rs.getDouble("fee") + " ");
            System.out.print(rs.getString("contact") + " ");
            System.out.print(rs.getTimestamp("start_time")+" ");
            System.out.print(rs.getTimestamp("end_time") + " ");
            System.out.print(rs.getBoolean("isCaller") + " ");
            System.out.println();
        }
        System.out.println("---短信情况---");
        System.out.println("资费 联系人 时间");
        rs = QueryUtil.executeQuery(conn, "select fee, contact, start_time from text_detail" + whereCriteria);
        while (rs.next())
        {
            System.out.print(rs.getDouble("fee" + " "));
            System.out.print(rs.getString("contact" + " "));
            System.out.print(rs.getDate("start_time")+" " +rs.getTime("start_time" + " "));
            System.out.println();
        }
        System.out.println("---流量情况---");
        System.out.println("资费 开始时间 结束时间 流量KB 是否为本地流量");
        rs = QueryUtil.executeQuery(conn, "select fee, start_time, end_time, data, islocal from data_detail" + whereCriteria);
        while (rs.next())
        {
            System.out.print(rs.getDouble("fee") + " ");
            System.out.print(rs.getTimestamp("start_time")+" ");
            System.out.print(rs.getTimestamp("end_time") + " ");
            System.out.print(rs.getInt("data") + " ");
            System.out.print(rs.getBoolean("islocal") + " ");
            System.out.println();
        }
    }


    private int getMin(Date startTime, Date endTime)
    {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int duration = (int)(endTime.getTime() - startTime.getTime())/(60 *1000);
        return duration;
    }

    private double getCallFee(int min, double rule)
    {
        return min * rule;
    }

    private double getDataFee(int data, double rule){
        double dataInMb = data/1024.0;
        return dataInMb * rule;
    }

    private int getMins(Date duration)
    {
        Long time = duration.getTime();
        int mins = (int)Math.ceil((double)time/1000.0*60);
        return mins;
    }


    /**
     * 执行一组 SQL 语句
     *
     * @param statements SQL 语句数组
     */
    private void executeSQLs(String[] statements) {
        Statement statement;
        try{
            statement = conn.createStatement();
            for (String s : statements) {
                statement.addBatch(s);
            }
            statement.executeBatch();
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }


//    private ResultSet executePrepareStatement(String sql, String s, int integer, Date time)
//    {
//        try {
//            PreparedStatement ps = conn.prepareStatement(sql);
//            ps.setString(1,s);
//            ps.setInt(2,integer);
//            String ss = "2019-01-01 00:00:00";
//            ps.setString(3, ss);
////            ps.setObject( 3,LocalDateTime.of(2019, 1, 1, 0, 0, 0));
//            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
////            ps.setTimestamp(3, new Timestamp(time.getTime()));
//            String sss = "select * from user_plan_ref";
//            ps = conn.prepareStatement(sss);
//            Statement statement = conn.createStatement();
//            statement.execute(sss);
//            conn.commit();
//            return statement.getResultSet();
//        } catch (SQLException e) {
//            e.printStackTrace();
//            try {
//                conn.rollback();
//            } catch (SQLException e1) {
//                e1.printStackTrace();
//            }
//        }
//        return null;
//    }

     private void checkBalance(double balance)
     {
         if(balance < Rule.BALANCE_ALARM)
         {
             System.out.println("余额不足，请尽快充值！");
         }
     }

//     private boolean isDiffMonth(Date date1, Date date2)
//     {
//         return !getMonth(date1).equals(getMonth(date2));
//     }
//
//     private Date getLastDateInMonth(Date date)
//     {
//         Date res = null;
//         SimpleDateFormat df = getDayFormat();
//         try {
//             res  = df.parse(getDay(date) + "23:59:59");
//         } catch (ParseException e) {
//             e.printStackTrace();
//         }
//         return res;
//     }
//
//     private Date getFirstDateInMonth(Date date)
//     {
//         Date res = null;
//         SimpleDateFormat df = getDayFormat();
//         try {
//             res  = df.parse(getDay(date) + "00:00:00");
//         } catch (ParseException e) {
//             e.printStackTrace();
//         }
//         return res;
//     }

//     private String getMonth(Date date){
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
//        return df.format(date);
//     }
//     private String getDay(Date date){
//         SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
//         return df.format(date);
//     }

//     private SimpleDateFormat getYearFormat()
//     {
//         return new SimpleDateFormat("yyyy-MM-dd");
//     }
//
//


     class Plan{
        int id;
        String name;
        int call_mins;
        int texts;
        int local_data;
        int nation_data;

        Plan(int id, String name, int call_mins, int texts, double local_data_gb, double nation_data_gb)
        {
            this.id = id;
            this.name = name;
            this.call_mins = call_mins;
            this.texts = texts;
            this.local_data = getDataInKb(local_data_gb);
            this.nation_data = getDataInKb(nation_data_gb);
        }

        private int getDataInKb(double data_gb)
        {
            return (int)(data_gb * 1024 *1024);
        }
     }
}

