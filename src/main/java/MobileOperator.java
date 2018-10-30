import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

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

        System.out.println("---已订购订单情况查看---");
        String sql = "select plan.id, plan.name, call_mins, texts, local_data_gb, nation_data_gb, start_time, end_time " +
        "from user_plan_ref join plan on user_plan_ref.plan = plan.id where user = " + phoneNum
                + " order by end_time";
        ResultSet rs = QueryUtil.executeQuery(conn, sql);
        ArrayList<Plan> currentPlans = new ArrayList<>();
        while (rs.next())
        {
            System.out.print(rs.getString("plan.name")+" ");
            System.out.print(rs.getDate("start_time")+" "+rs.getTime("start_time")+" ");
            System.out.print(rs.getDate("end_time")+" " + rs.getTime("end_time")+" ");
            System.out.println();

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

        System.out.println("---当月各项剩余情况---");
        String monthConditionSql = "select call_mins, texts, local_data, nation_data from user where phone_num = " + phoneNum;
        rs = QueryUtil.executeQuery(conn, monthConditionSql);
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

        //如果未订购过，订购该套餐
        String addPlanSql = "insert into user_plan_ref (user, plan, start_time, end_time) value (" +
                "\"" + phoneNum + "\"," +
                "\"" + planId + "\"," +
                "\"" + startDateTime + "\"," +
                "\"" + endDateTime + "\"" +
                ")";
        QueryUtil.executeSQL(conn, addPlanSql);
        System.out.println("成功订购");
    }

    /**
     * 1.3 用户退订次月生效
     * 默认能被选择的planId都是用户已经订阅的plan，且未生效的plan不能退订
     */
    public void deletePlanNextMonth(String phoneNum, int planId)
    {
        Date newEndDate = DateUtil.nextMonthFirstDate();
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
        double fee = 0;
        int call_mins_remain = 0;
        double balance = 0;
        if(isCaller)
        {
            //获取用户的剩余可用拨打时间
            int call_mins = DateUtil.getMin(startTime, endTime);
            ResultSet rs = QueryUtil.executeQuery(conn,"select call_mins, balance from user where phone_num = " + phoneNum);
            assert rs.getFetchSize() == 1;
            while (rs.next()) {
                call_mins_remain = rs.getInt("call_mins");
                balance = rs.getDouble("balance");
            }

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
            }
        }
            String addDetailSql = "insert into call_detail (user, fee, contact, start_time, end_time, iscaller) value ("+
                    "\"" + phoneNum + "\"," +
                    "\"" + fee + "\"," +
                    "\"" + contact + "\"," +
                    "\"" + FormatUtil.format(startTime) + "\"," +
                    "\"" + FormatUtil.format(endTime) + "\"," +
                    isCaller +
                    ")";
            String minusTimeAndMoneySql = "UPDATE user SET call_mins = " + call_mins_remain + " , balance = " + balance +
                    " WHERE phone_num = " + phoneNum;
        //如果是被叫，只加一条通话记录
        if(!isCaller) {
            QueryUtil.executeSQL(conn, addDetailSql);
            System.out.println("添加被叫记录");
        }
        //否则，更改用户的个人信息
        else {
            QueryUtil.executeSQLs(conn, new String[]{addDetailSql, minusTimeAndMoneySql});
            System.out.println("添加主叫记录并更改个人信息");
        }

        checkBalance(balance);
    }

    /**
     * 3 某个用户在使用流量情况下的资费生成
     */
    public void generateDataFee(final String phoneNum, final Date startTime, final Date endTime, final int data, final String fromWhere) throws SQLException
    {
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
        QueryUtil.executeSQLs(conn, new String[]{addDetailSql, minusDataAndMoneySql});
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

    private double getCallFee(int min, double rule)
    {
        return min * rule;
    }

    private double getDataFee(int data, double rule){
        double dataInMb = data/1024.0;
        return dataInMb * rule;
    }

    private void checkBalance(double balance)
     {
         if(balance < Rule.BALANCE_ALARM)
         {
             System.out.println("余额不足，请尽快充值！");
         }
     }

    /**
     * 套餐
     */
    class Plan {
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

