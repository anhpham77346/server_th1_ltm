package th1.server_th1;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Server_th1 {

    private static final String URL = "jdbc:mysql://localhost:3306/thuchanh1";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() {
        Connection connection = null;
        try {
            // Kết nối đến MySQL
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connect successful!");
        } catch (SQLException e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
        }
        return connection;
    }

    public static void main(String[] args) {
        // Thử kết nối đến CSDL
        Connection conn = getConnection();
        if (conn != null) {
            System.out.println("Server connect successful");
        }

        // Bước 2: Thiết lập ServerSocket để lắng nghe yêu cầu từ Client
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server đang lắng nghe trên cổng 12345...");

            while (true) {
                // Chấp nhận kết nối từ client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Kết nối từ client thành công!");

                // Xử lý yêu cầu từ client trong một luồng mới (để hỗ trợ nhiều client)
                new ClientHandler(clientSocket, conn).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

// Class xử lý yêu cầu từ client
class ClientHandler extends Thread {

    private Socket clientSocket;
    private Connection dbConnection;

    public ClientHandler(Socket socket, Connection connection) {
        this.clientSocket = socket;
        this.dbConnection = connection;
    }

    public void run() {
        try {
            // Thiết lập các luồng I/O để giao tiếp với client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Đọc yêu cầu từ client
            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Client nói: " + clientMessage);

                // Xử lý yêu cầu đăng nhập từ client
                if (clientMessage.equalsIgnoreCase("login")) {
                    String email = in.readLine(); // Đọc email từ client
                    String password = in.readLine(); // Đọc password từ client
                    handleLogin(out, email, password); // Kiểm tra đăng nhập
                }

                // Xử lý các yêu cầu từ client, ví dụ: lệnh "listUsers" để lấy danh sách người dùng
                if (clientMessage.equalsIgnoreCase("listUsers")) {
                    listUsers(out);  // Trả về danh sách người dùng
                }

                // Bạn có thể xử lý thêm các yêu cầu khác từ client tại đây
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Hàm để xử lý đăng nhập
    private void handleLogin(PrintWriter out, String email, String password) {
        try {
            // Truy vấn kiểm tra thông tin đăng nhập từ cơ sở dữ liệu sử dụng email và password
            String query = "SELECT * FROM User WHERE email = ? AND password = ?";
            var preparedStatement = dbConnection.prepareStatement(query);
            preparedStatement.setString(1, email); // Truyền email vào câu lệnh SQL
            preparedStatement.setString(2, password); // Truyền password vào câu lệnh SQL
            var resultSet = preparedStatement.executeQuery();

            // Kiểm tra kết quả truy vấn
            if (resultSet.next()) {
                out.println("Login successful!");  // Gửi thông báo đăng nhập thành công về client
            } else {
                out.println("Invalid email or password!");  // Thông báo đăng nhập thất bại
            }

        } catch (SQLException e) {
            e.printStackTrace();
            out.println("Lỗi khi đăng nhập.");
        }
    }

    // Hàm để lấy danh sách người dùng từ CSDL và gửi về cho client
    private void listUsers(PrintWriter out) {
        try {
            String query = "SELECT fullname, email FROM User";
            var statement = dbConnection.createStatement();
            var resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                String fullname = resultSet.getString("fullname");
                String email = resultSet.getString("email");
                out.println("User: " + fullname + " - Email: " + email);
            }

            out.println("End of user list");

        } catch (SQLException e) {
            e.printStackTrace();
            out.println("Lỗi khi lấy danh sách người dùng.");
        }
    }
}
