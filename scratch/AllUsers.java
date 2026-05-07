
import java.sql.*;
import java.util.Properties;

public class AllUsers {
    public static void main(String[] args) {
        String url = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/trustfundme_identity_db?useSSL=true&requireSSL=true";
        Properties props = new Properties();
        props.setProperty("user", "4SjU1fFakh4jstj.root");
        props.setProperty("password", "cMiesG5HVLjA5u25");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("--- ALL USERS ---");
            String query = "SELECT id, email, full_name, role FROM users";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getLong("id") + " | Email: " + rs.getString("email") + " | Name: " + rs.getString("full_name") + " | Role: " + rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
