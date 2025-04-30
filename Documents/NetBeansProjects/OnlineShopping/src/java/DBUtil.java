import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.sql.*;

@WebServlet(name = "DBUtil", urlPatterns = {"/DBUtil"})
public class DBUtil extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/kel?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static Connection getConnection() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
    
    

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (action == null) {
            out.println("<h3>Error: Missing 'action' parameter in request!</h3>");
            return;
        }

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            switch (action) {
                case "save":
                    insertUser(conn, username, email, password, response);
                    break;
                case "update":
                    updateUser(conn, username, email, out);
                    break;
                case "delete":
                    deleteUser(conn, email, out);
                    break;
                case "show":
                    selectUsers(conn, out);
                    break;
                case "login":
                    loginUser(conn, email, password, request, response); // <-- fixed
                    break;
                case "placeOrder":
                    placeOrder(conn, request, response);
                    break;
                case "fetchOrders":
                    fetchOrders(conn, request, response);
                    break;


                default:
                    out.println("<h3>Invalid action!</h3>");
            }

            conn.close();
        } catch (ClassNotFoundException e) {
            out.println("<h2>JDBC Driver Not Found!</h2>");
            e.printStackTrace(out);
        } catch (SQLException e) {
            out.println("<h2>Database Connection Failed!</h2>");
            e.printStackTrace(out);
        }
    }

private void loginUser(Connection conn, String email, String password, HttpServletRequest request, HttpServletResponse response)
        throws SQLException, IOException {

    String sql = "SELECT * FROM user_clickshop WHERE email = ? AND password = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, email);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();

        response.setContentType("text/plain");  
        PrintWriter out = response.getWriter();

       if (rs.next()) {
    String name = rs.getString("name");
    int userId = rs.getInt("id");

    // Store in session
    HttpSession session = request.getSession();
    session.setAttribute("username", name);
    session.setAttribute("userId", userId);
    session.setAttribute("email", email);

    // Return JSON string
    response.setContentType("application/json");
    out.print("{\"status\":\"success\",\"userId\":" + userId + ",\"username\":\"" + name + "\"}");
} else {
    response.setContentType("application/json");
    out.print("{\"status\":\"error\",\"message\":\"Invalid email or password\"}");
}

    }
}


    private void insertUser(Connection conn, String username, String email, String password, HttpServletResponse response)
            throws SQLException, IOException {
        String sql = "INSERT INTO user_clickshop (name, email, password) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, password);
            int rowsInserted = stmt.executeUpdate();

            PrintWriter out = response.getWriter();
            response.setContentType("text/html");

            if (rowsInserted > 0) {
                out.println("<script type='text/javascript'>");
                out.println("alert('Registered Successfully!');");
                out.println("window.location.href = 'SIGNIN.html';");
                out.println("</script>");
            } else {
                out.println("<script type='text/javascript'>");
                out.println("alert('Registration Failed!');");
                out.println("window.history.back();");
                out.println("</script>");
            }
        }
    }

public static void placeOrder(Connection conn, HttpServletRequest request, HttpServletResponse response) {
    try {
        String email = request.getParameter("email");
        String productName = request.getParameter("product_name");
        int userId = Integer.parseInt(request.getParameter("user_id"));
        int quantity = Integer.parseInt(request.getParameter("quantity"));
        double price = Double.parseDouble(request.getParameter("price"));
        double total = Double.parseDouble(request.getParameter("total"));



        String sql = "INSERT INTO orders (user_id, status, email, product_name, quantity, price, total) VALUES (?, 'Pending', ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, email);
            ps.setString(3, productName);
            ps.setInt(4, quantity);
            ps.setDouble(5, price);
            ps.setDouble(6, total);

            int rows = ps.executeUpdate();

            if (rows > 0) {
              
                response.sendRedirect("order-status.html");
            } else {
               
                response.setContentType("application/json");
                PrintWriter out = response.getWriter();
                out.print("{\"status\":\"error\",\"message\":\"Failed to place order.\"}");
                out.close();
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        try {
            response.setStatus(500);
            PrintWriter out = response.getWriter();
            out.print("{\"status\":\"error\",\"message\":\"Server error occurred!\"}");
            out.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}

private void fetchOrders(Connection conn, HttpServletRequest request, HttpServletResponse response)
        throws IOException {
    response.setContentType("application/json");
    PrintWriter out = response.getWriter();

    try {
        int userId = Integer.parseInt(request.getParameter("userId"));
        String sql = "SELECT * FROM orders WHERE user_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder();
            json.append("[");

            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                first = false;

                json.append("{")
                    .append("\"orderId\":").append(rs.getInt("id")).append(",")
                    .append("\"product_name\":\"").append(rs.getString("product_name")).append("\",")
                    .append("\"quantity\":").append(rs.getInt("quantity")).append(",")
                    .append("\"price\":").append(rs.getDouble("price")).append(",")
                    .append("\"total\":").append(rs.getDouble("total")).append(",")
                    .append("\"status\":\"").append(rs.getString("status")).append("\"")
                    .append("}");
            }

            json.append("]");
            out.print(json.toString());
        }
    } catch (Exception e) {
        e.printStackTrace();
        out.print("{\"status\":\"error\",\"message\":\"Failed to fetch orders.\"}");
    } finally {
        out.close();
    }
}





public static int getUserIdFromEmail(String email) {
    int userId = -1;

    String url = "jdbc:mysql://localhost:3306/kel?useSSL=false&serverTimezone=UTC";
    String user = "root";
    String password = "";

    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("id");
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    return userId; 
}





    private void updateUser(Connection conn, String username, String email, PrintWriter out) throws SQLException {
        String sql = "UPDATE user_clickshop SET name = ? WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            int rowsUpdated = stmt.executeUpdate();
            out.println("<p>Updated " + rowsUpdated + " user(s) successfully.</p>");
        }
    }

    private void deleteUser(Connection conn, String email, PrintWriter out) throws SQLException {
        String sql = "DELETE FROM user_clickshop WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            int rowsDeleted = stmt.executeUpdate();
            out.println("<p>Deleted " + rowsDeleted + " user(s) successfully.</p>");
        }
    }

    private void selectUsers(Connection conn, PrintWriter out) throws SQLException {
        String sql = "SELECT * FROM user_clickshop";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            out.println("<h3>User List</h3><ul>");
            while (rs.next()) {
                out.println("<li>" + rs.getString("name") + " - " + rs.getString("email") + "</li>");
            }
            out.println("</ul>");
        }
    }

    
}
