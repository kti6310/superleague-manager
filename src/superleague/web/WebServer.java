package superleague.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.swing.JOptionPane;

public final class WebServer {
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8", "js", "text/javascript", "jar", "application/java-archive",
            "csv", "text/csv; charset=utf-8", "ttf", "font/ttf", "class", "application/java-vm");

    private final Path root;

    private WebServer(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public static void main(String[] args) throws IOException {
        start(Path.of(""), 8765, true);
    }

    public static HttpServer start(Path root, int port, boolean interactive) throws IOException {
        WebServer web = new WebServer(root);
        HttpServer server = null;
        for (int p = port; server == null && p < port + 20; p++) {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", p), 0);
            } catch (BindException e) {
                server = null;
            }
        }
        if (server == null) {
            throw new IOException("빈 포트를 찾지 못했습니다 (" + port + "~" + (port + 19) + ")");
        }
        server.createContext("/", web::handle);
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        System.out.println("Super League Manager 웹 버전: " + url);
        if (interactive && !GraphicsEnvironment.isHeadless()) {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
            JOptionPane.showMessageDialog(null,
                    "브라우저에서 게임이 열립니다.\n주소: " + url + "\n\n이 창의 [확인]을 누르면 웹 서버가 종료됩니다.",
                    "Super League Manager 웹 서버", JOptionPane.INFORMATION_MESSAGE);
            server.stop(0);
            System.exit(0);
        }
        return server;
    }

    Path fileFor(String uriPath) {
        String rel = uriPath.equals("/") ? "web/index.html" : uriPath.substring(1);
        Path file = root.resolve(rel).normalize();
        if (!file.startsWith(root)) {
            return null;
        }
        if (rel.equals("web/font.ttf") && !Files.exists(file)) {
            Path malgun = Path.of(System.getenv().getOrDefault("WINDIR", "C:\\Windows"), "Fonts", "malgun.ttf");
            return Files.exists(malgun) ? malgun : null;
        }
        return Files.isRegularFile(file) ? file : null;
    }

    private void handle(HttpExchange ex) throws IOException {
        try (ex) {
            Path file = ex.getRequestMethod().equals("GET") || ex.getRequestMethod().equals("HEAD")
                    ? fileFor(URI.create(ex.getRequestURI().getRawPath()).getPath()) : null;
            if (file == null) {
                ex.sendResponseHeaders(404, -1);
                return;
            }
            long size = Files.size(file);
            long start = 0;
            long end = size - 1;
            int status = 200;
            String range = ex.getRequestHeaders().getFirst("Range");
            if (range != null && range.startsWith("bytes=") && !range.contains(",")) {
                String[] parts = range.substring(6).split("-", -1);
                try {
                    if (parts[0].isEmpty()) {
                        start = Math.max(0, size - Long.parseLong(parts[1]));
                    } else {
                        start = Long.parseLong(parts[0]);
                        if (!parts[1].isEmpty()) {
                            end = Math.min(end, Long.parseLong(parts[1]));
                        }
                    }
                } catch (NumberFormatException e) {
                    ex.sendResponseHeaders(416, -1);
                    return;
                }
                if (start > end || start >= size) {
                    ex.getResponseHeaders().set("Content-Range", "bytes */" + size);
                    ex.sendResponseHeaders(416, -1);
                    return;
                }
                status = 206;
                ex.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + size);
            }
            String name = file.getFileName().toString();
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            ex.getResponseHeaders().set("Content-Type", TYPES.getOrDefault(ext, "application/octet-stream"));
            ex.getResponseHeaders().set("Accept-Ranges", "bytes");
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            long length = end - start + 1;
            if (ex.getRequestMethod().equals("HEAD")) {
                ex.getResponseHeaders().set("Content-Length", String.valueOf(length));
                ex.sendResponseHeaders(status, -1);
                return;
            }
            ex.sendResponseHeaders(status, length);
            try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r"); OutputStream out = ex.getResponseBody()) {
                in.seek(start);
                byte[] buf = new byte[64 * 1024];
                long left = length;
                while (left > 0) {
                    int n = in.read(buf, 0, (int) Math.min(buf.length, left));
                    if (n < 0) {
                        break;
                    }
                    out.write(buf, 0, n);
                    left -= n;
                }
            }
        }
    }
}
