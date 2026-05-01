import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDbConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/trustfundme_identity_db?useSSL=true&requireSSL=true";
        String user = "4SjU1fFakh4jstj.root";
        String password = "cMiesG5HVLjA5u25";

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT user_id, bank_code, account_number, webhook_key FROM bank_account");
            while (rs.next()) {
                System.out.println("UserID: " + rs.getLong("user_id") + " | Bank: " + rs.getString("bank_code") + " | WebhookKey: " + rs.getString("webhook_key"));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
