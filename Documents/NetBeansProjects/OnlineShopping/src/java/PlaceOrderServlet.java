import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.sql.*;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/place-order")
public class PlaceOrderServlet extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/kel?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }


            JSONObject json = new JSONObject(sb.toString());
            int userId = json.getInt("userId");
            JSONArray cart = json.getJSONArray("cart");

            if (userId <= 0 || cart.length() == 0) {
                response.setStatus(400);
                out.print("{\"status\":\"error\",\"message\":\"Invalid user ID or empty cart.\"}");
                return;
            }


            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                String sql = "INSERT INTO orders (user_id, items_json, status) VALUES (?, ?, 'Pending')";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, cart.toString());

                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        out.print("{\"status\":\"success\",\"message\":\"Order placed successfully!\"}");
                    } else {
                        response.setStatus(500);
                        out.print("{\"status\":\"error\",\"message\":\"Failed to place order.\"}");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"status\":\"error\",\"message\":\"Server error occurred!\"}");
        }
    }
}
