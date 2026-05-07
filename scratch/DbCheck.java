
import java.sql.*;
import java.util.Properties;

public class DbCheck {
    public static void main(String[] args) {
        String url = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/trustfundme_campaign_db?useSSL=true&requireSSL=true";
        Properties props = new Properties();
        props.setProperty("user", "4SjU1fFakh4jstj.root");
        props.setProperty("password", "cMiesG5HVLjA5u25");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("Connected to Campaign DB!");
            
            System.out.println("\n--- RECENT COMMITMENTS ---");
            String query = "SELECT * FROM campaign_commitments ORDER BY created_at DESC LIMIT 5";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                ResultSetMetaData md = rs.getMetaData();
                int columns = md.getColumnCount();
                while (rs.next()) {
                    for (int i = 1; i <= columns; i++) {
                        System.out.print(md.getColumnName(i) + ": " + rs.getObject(i) + " | ");
                    }
                    System.out.println();
                }
            }

            System.out.println("\n--- RECENT CAMPAIGNS ---");
            query = "SELECT id, title, fund_owner_id, status FROM campaigns ORDER BY created_at DESC LIMIT 5";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    System.out.println("ID: " + rs.getLong("id") + " | Title: " + rs.getString("title") + " | Owner: " + rs.getLong("fund_owner_id") + " | Status: " + rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
