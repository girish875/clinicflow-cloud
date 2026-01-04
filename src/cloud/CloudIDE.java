package cloud;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class CloudIDE {

    public static void main(String[] args) throws Exception {

        // 🔑 CLOUD-SAFE PORT (Railway / Render / Local)
        int port = Integer.parseInt(
            System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
            new InetSocketAddress(port), 0
        );

        server.createContext("/", CloudIDE::handleIndex);
        server.createContext("/compile", CloudIDE::handleCompile);

        server.start();
        System.out.println(
            "✔ ClinicFlow Cloud IDE running on port " + port
        );
    }

    // Serve IDE page
    private static void handleIndex(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>ClinicFlow Cloud IDE</title>
            <style>
                body { font-family: Arial; margin: 20px; }
                textarea { width: 100%; height: 300px; }
                img { margin-top: 20px; border: 1px solid #ccc; }
                button { padding: 10px 20px; margin-top: 10px; }
                pre { color: red; white-space: pre-wrap; }
            </style>
        </head>
        <body>

        <h2>ClinicFlow Cloud IDE</h2>

        <textarea id="dsl">
        workflow GPVisit
        type Outpatient {
            start -> CheckIn

            stage CheckIn:
                CHECK_IN by Admin

            stage Assessment:
                ASSESSMENT by Nurse

            stage Consultation:
                CONSULTATION by Doctor

            CheckIn -> Assessment
            Assessment -> Consultation

            end Completed
        }
        </textarea>

        <br>
        <button onclick="generate()">Generate Diagram</button>

        <br><br>
        <pre id="error"></pre>
        <img id="result"/>

        <script>
        function generate() {
            fetch("/compile", {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: document.getElementById("dsl").value
            })
            .then(res => {
                if (!res.ok) {
                    return res.text().then(t => { throw t; });
                }
                return res.blob();
            })
            .then(blob => {
                document.getElementById("error").textContent = "";
                document.getElementById("result").src =
                    URL.createObjectURL(blob);
            })
            .catch(err => {
                document.getElementById("result").src = "";
                document.getElementById("error").textContent = err;
            });
        }
        </script>

        </body>
        </html>
        """;

        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    // Compile DSL → PNG
    private static void handleCompile(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String input = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        try {
            ClinicFlowCompilerService service =
                new ClinicFlowCompilerService();

            byte[] png = service.compileToPng(input);

            exchange.getResponseHeaders()
                .add("Content-Type", "image/png");

            exchange.sendResponseHeaders(200, png.length);
            exchange.getResponseBody().write(png);

        } catch (Exception e) {
            //  SEND PLAIN TEXT ERROR (SAFE FOR MULTI-LINE)
            byte[] msg =
                e.getMessage().getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "text/plain");

            exchange.sendResponseHeaders(400, msg.length);
            exchange.getResponseBody().write(msg);
        }

        exchange.close();
    }
}
