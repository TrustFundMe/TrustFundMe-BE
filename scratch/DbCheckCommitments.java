
import java.sql.*;
import java.util.Properties;

public class DbCheckCommitments {
    public static void main(String[] args) {
        String url = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/trustfundme_campaign_db?useSSL=true&requireSSL=true";
        Properties props = new Properties();
        props.setProperty("user", "4SjU1fFakh4jstj.root");
        props.setProperty("password", "cMiesG5HVLjA5u25");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("--- COMMITMENTS IN DB ---");
            String query = "SELECT id, campaign_id, user_id, status, full_name FROM campaign_commitments";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getLong("id") + " | CampaignID: " + rs.getLong("campaign_id") + " | UserID: " + rs.getLong("user_id") + " | Status: " + rs.getString("status") + " | Name: " + rs.getString("full_name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
