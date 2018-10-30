import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {
    static final String DB_URL = "jdbc:mysql://localhost:3306/mobile_operator_db"+"?useUnicode=true&characterEncoding=UTF-8&serverTimezone=GMT%2B8";
    static final String USER = "root";
    static final String PASS = "root";

    public static Connection getNoAutoCommitConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(false);
        return connection;
    }
}
