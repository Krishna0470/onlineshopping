import java.io.*;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import org.json.JSONArray;
import org.json.JSONObject; 

@WebServlet("/orders")
public class orders extends HttpServlet {
@Override
protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();

    String userIdParam = request.getParameter("userId");
    if (userIdParam == null) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print("{\"message\":\"Missing userId parameter\"}");
        return;
    }

    try {
        int userId = Integer.parseInt(userIdParam);

            Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/kel?useSSL=false&serverTimezone=UTC", "root", "");

        PreparedStatement ps = conn.prepareStatement("SELECT * FROM orders WHERE user_id = ?");
        ps.setInt(1, userId);

        ResultSet rs = ps.executeQuery();
        JSONArray orders = new JSONArray();

        while (rs.next()) {
    JSONObject order = new JSONObject();
    order.put("id", rs.getInt("id"));
    order.put("product_name", rs.getString("product_name"));
    order.put("price", rs.getDouble("price"));
    order.put("quantity", rs.getInt("quantity"));
    order.put("total", rs.getDouble("total"));
    order.put("status", rs.getString("status"));
    orders.put(order);
}


        out.print(orders.toString());
        conn.close();
    } catch (Exception e) {
        e.printStackTrace(); 
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.print("{\"message\":\"" + e.getMessage() + "\"}");
    }
}


}
