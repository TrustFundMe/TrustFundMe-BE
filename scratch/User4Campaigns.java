
import java.sql.*;
import java.util.Properties;

public class User4Campaigns {
    public static void main(String[] args) {
        String url = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/trustfundme_campaign_db?useSSL=true&requireSSL=true";
        Properties props = new Properties();
        props.setProperty("user", "4SjU1fFakh4jstj.root");
        props.setProperty("password", "cMiesG5HVLjA5u25");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("--- CAMPAIGNS FOR USER 4 ---");
            String query = "SELECT id, title, status FROM campaigns WHERE fund_owner_id = 4";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getLong("id") + " | Title: " + rs.getString("title") + " | Status: " + rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
