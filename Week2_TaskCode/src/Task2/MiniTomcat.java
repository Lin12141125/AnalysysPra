package Task2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class MiniTomcat {
    private final RouteScanner routeScanner;
    private final int port;
    private final MyIoC myIoC; //IoC容器

    public MiniTomcat(int port) throws Exception {
        this.routeScanner = new RouteScanner();
        this.port = port;
        // 1. 初始化IoC容器，扫描Task2包，实例化所有@MyComponent类，并完成依赖注入
        this.myIoC = new MyIoC("Task2");
        System.out.println("\nIoC 容器初始化完成，共管理 " + myIoC.getAllBeans().size() + " 个 Bean");
        for (Object bean : myIoC.getAllBeans()) {
            System.out.println("Bean: " + bean.getClass().getSimpleName());
        }

        // 2. 创建路由扫描器，从容其中获取所有Controller实例，注册路由
        for (Object bean : myIoC.getAllBeans()) {
            // 检查该bean是否有方法是控制器方法（即带@RequestMapping注解的方法）
            boolean isController = false;
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(RequestMapping.class)) {
                    isController = true;
                    break;
                }
            }
            if (isController) {
                routeScanner.registerController(bean);
            }
        }
        System.out.println("MiniTomcat 初始化完成，所有 Controller 已注册");
    }

    public void start() throws Exception {
        try(ServerSocket serverSocket=new ServerSocket(port)) {
            System.out.println("MiniTomcat 已启动，监听端口: " + port);
            System.out.println("访问 http://localhost:" + port + "/hello 或 /greet 或 /user 进行测试");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // 每个请求使用一个虚拟线程处理
                Thread.ofVirtual().start(() -> {
                    try {
                        handleRequest(clientSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public void handleRequest(Socket socket) throws Exception {
        // 使用 try-with-resources 自动关闭 socket
        try (socket) {
            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            // 解析HTTP请求，获取请求方法和路径
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return; // 无效请求
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return; // 无效请求 --> 方法 路径
            String method = parts[0];
            String fullPath = parts[1];
    
            String path;
            String queryString = null;
            int questionIdx = fullPath.indexOf('?');
            if (questionIdx != -1) {
                path = fullPath.substring(0, questionIdx);
                queryString = fullPath.substring(questionIdx + 1); //保存query参数
            } else {
                path = fullPath;
            }

            System.out.println("[" + Thread.currentThread() + "] " + method + " " + path);

            String line;
            int contentLength = 0;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) { // 解析Content-Length头，获取请求体长度
                    contentLength = Integer.parseInt(line.substring(16).trim());
                }
            }

            // 如果是POST，读取并打印body
            if ("POST".equalsIgnoreCase(method) && contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                String body = new String(bodyChars);
                System.out.println("POST body: " + body);
            }

            // 路由处理，获取响应体
            String responseBody=routeScanner.execute(path);
            // 判断是否404
            if ("404 Not Found".equals(responseBody)) {
                sendErrorResponse(output, 404, responseBody);
            } else{
                sendSuccessResponse(output, responseBody); // 发送成功
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendErrorResponse(OutputStream output, int statusCode, String message) throws IOException {
        String statusLine;
        if (statusCode == 404) {
            statusLine = "HTTP/1.1 404 Not Found\r\n";
        } else {
            statusLine = "HTTP/1.1 " + statusCode + " Error\r\n";
        }
        String body = "<html><body><h1>" + message + "</h1></body></html>";
        byte[] bodyBytes = body.getBytes("UTF-8");
        String contentType = "Content-Type: text/html; charset=UTF-8\r\n";
        String contentLength = "Content-Length: " + bodyBytes.length + "\r\n";
        String emptyLine = "\r\n";
        String response = statusLine + contentType + contentLength + emptyLine;
        output.write(response.getBytes("UTF-8"));
        output.write(bodyBytes);
        output.flush();
    }

    private void sendSuccessResponse(OutputStream output, String body) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        String statusLine = "HTTP/1.1 200 OK\r\n";
        String contentType = "Content-Type: text/html; charset=UTF-8\r\n";
        String contentLength = "Content-Length: " + bodyBytes.length + "\r\n";
        String emptyLine = "\r\n";
        String response = statusLine + contentType + contentLength + emptyLine;
        output.write(response.getBytes("UTF-8"));
        output.write(bodyBytes);
        output.flush();
    }

    public static void main(String[] args) throws Exception {
        MiniTomcat server = new MiniTomcat(8080);
        server.start();
    }

}
