import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryUtil {
    /**
     * 执行一句 SQL 语句
     *
     * @param sql
     */
    public static void executeSQL(Connection conn, String sql){
        Statement statement = null;
        try{
            statement = conn.createStatement();
            statement.execute(sql);
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
    /**
     * 执行一句 SQL 语句
     *
     * @param sql
     */
    public static ResultSet executeQuery(Connection conn, String sql){
        Statement statement;
        ResultSet rs = null;
        try{
            statement = conn.createStatement();
            rs = statement.executeQuery(sql);
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        return rs;
    }
}
