package com.wifisecuritylab.app.service

import com.wifisecuritylab.app.data.model.PortalSubmission
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.Executors

class LocalHttpServer(private val port: Int = 8080) {

    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private var isRunning = false
    private var submissionCallback: ((PortalSubmission) -> Unit)? = null

    fun setSubmissionCallback(callback: (PortalSubmission) -> Unit) {
        this.submissionCallback = callback
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        executor.execute {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    executor.execute { handleClient(client) }
                }
            } catch (e: Exception) {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) { }
        executor.shutdown()
    }

    private fun handleClient(client: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val writer = PrintWriter(client.getOutputStream())

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val method = parts.getOrNull(0) ?: "GET"
            val path = parts.getOrNull(1) ?: "/"

            val headers = mutableMapOf<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                val headerParts = line!!.split(": ", limit = 2)
                if (headerParts.size == 2) {
                    headers[headerParts[0]] = headerParts[1]
                }
            }

            val userAgent = headers["User-Agent"] ?: "Unknown"
            val clientIp = client.inetAddress.hostAddress ?: "unknown"
            val cleanPath = path.substringBefore("?")

            when {
                method == "GET" && cleanPath in setOf(
                    "/",
                    "/portal",
                    "/generate_204",
                    "/gen_204",
                    "/hotspot-detect.html",
                    "/connecttest.txt",
                    "/ncsi.txt"
                ) -> {
                    servePortal(writer)
                }
                method == "POST" && cleanPath == "/submit" -> {
                    val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
                    val body = CharArray(contentLength)
                    reader.read(body)
                    val params = parseFormData(String(body))

                    val submission = PortalSubmission(
                        clientIp = clientIp,
                        syntheticUsername = params["username"] ?: "",
                        userAgent = userAgent
                    )
                    submissionCallback?.invoke(submission)
                    serveWarning(writer)
                }
                else -> {
                    serve404(writer)
                }
            }

            writer.flush()
            client.close()
        } catch (e: Exception) {
            client.close()
        }
    }

    private fun parseFormData(body: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        body.split("&").forEach { param ->
            val kv = param.split("=", limit = 2)
            if (kv.size == 2) {
                result[kv[0]] = URLDecoder.decode(kv[1], "UTF-8")
            }
        }
        return result
    }

    private fun servePortal(writer: PrintWriter) {
        val html = PORTAL_HTML
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/html; charset=UTF-8")
        writer.println("Content-Length: ${html.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(html)
    }

    private fun serveWarning(writer: PrintWriter) {
        val html = WARNING_HTML
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/html; charset=UTF-8")
        writer.println("Content-Length: ${html.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.println(html)
    }

    private fun serve404(writer: PrintWriter) {
        val html = "<html><body><h1>404 Not Found</h1></body></html>"
        writer.println("HTTP/1.1 404 Not Found")
        writer.println("Content-Type: text/html")
        writer.println("Content-Length: ${html.length}")
        writer.println("Connection: close")
        writer.println()
        writer.println(html)
    }

    companion object {
        const val PORTAL_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>WiFi Security Lab — Educational Portal</title>
<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  background: #0a0a0a;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}
.card {
  background: #141414;
  border: 1px solid #1db954;
  border-radius: 12px;
  padding: 32px;
  max-width: 400px;
  width: 100%;
  box-shadow: 0 0 20px rgba(29, 185, 84, 0.1);
}
.badge {
  display: inline-block;
  background: #1db954;
  color: #000;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 4px 10px;
  border-radius: 4px;
  margin-bottom: 16px;
}
h1 {
  font-size: 20px;
  color: #1db954;
  margin-bottom: 8px;
}
h2 {
  font-size: 16px;
  color: #fff;
  margin-bottom: 16px;
  font-weight: 500;
}
p {
  font-size: 13px;
  line-height: 1.6;
  color: #aaa;
  margin-bottom: 20px;
}
label {
  display: block;
  font-size: 12px;
  color: #888;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
input[type="text"], input[type="password"] {
  width: 100%;
  background: #0a0a0a;
  border: 1px solid #333;
  border-radius: 6px;
  padding: 12px;
  color: #fff;
  font-size: 14px;
  margin-bottom: 16px;
  outline: none;
}
input[type="text"]:focus, input[type="password"]:focus {
  border-color: #1db954;
}
button {
  width: 100%;
  background: #1db954;
  color: #000;
  border: none;
  border-radius: 6px;
  padding: 14px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
button:hover { background: #1ed760; }
.footer {
  margin-top: 20px;
  font-size: 11px;
  color: #555;
  text-align: center;
}
</style>
</head>
<body>
<div class="card">
  <span class="badge">Simulation Only</span>
  <h1>SECURITY LAB</h1>
  <h2>Wi-Fi Verification</h2>
  <p>This is an authorized security awareness demonstration. Do not enter real credentials.</p>
  <form action="/submit" method="POST">
    <label>Test Username</label>
    <input type="text" name="username" value="test_user" readonly>
    <label>Test Password</label>
    <input type="password" name="password" value="[REDACTED — SIMULATION]" readonly>
    <button type="submit">Test Login</button>
  </form>
  <div class="footer">LAB ENVIRONMENT ONLY — NO REAL DATA COLLECTED</div>
</div>
</body>
</html>"""

        const val WARNING_HTML = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Security Warning</title>
<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  background: #0a0a0a;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
}
.card {
  background: #141414;
  border: 1px solid #ff4444;
  border-radius: 12px;
  padding: 32px;
  max-width: 420px;
  width: 100%;
  box-shadow: 0 0 20px rgba(255, 68, 68, 0.1);
}
.badge {
  display: inline-block;
  background: #ff4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 4px 10px;
  border-radius: 4px;
  margin-bottom: 16px;
}
h1 {
  font-size: 22px;
  color: #ff4444;
  margin-bottom: 12px;
}
p {
  font-size: 14px;
  line-height: 1.6;
  color: #ccc;
  margin-bottom: 16px;
}
ul {
  list-style: none;
  margin-bottom: 20px;
}
li {
  font-size: 13px;
  color: #aaa;
  padding: 6px 0;
  padding-left: 20px;
  position: relative;
}
li::before {
  content: "✓";
  position: absolute;
  left: 0;
  color: #1db954;
  font-weight: bold;
}
.footer {
  margin-top: 20px;
  font-size: 11px;
  color: #555;
  text-align: center;
  border-top: 1px solid #222;
  padding-top: 16px;
}
</style>
</head>
<body>
<div class="card">
  <span class="badge">Security Warning</span>
  <h1>SECURITY WARNING</h1>
  <p>You just submitted information to a controlled Wi-Fi security demonstration.</p>
  <p>In a real phishing scenario, an attacker could attempt to steal credentials.</p>
  <p style="color:#fff; font-weight:600; margin-bottom:8px;">Protect yourself:</p>
  <ul>
    <li>Use HTTPS on all login pages</li>
    <li>Enable Multi-Factor Authentication (MFA)</li>
    <li>Connect only to trusted networks</li>
    <li>Verify domain names carefully</li>
    <li>Use a password manager</li>
  </ul>
  <div class="footer">WiFiSecurityLab~ — Authorized Research Only</div>
</div>
</body>
</html>"""
    }
}
