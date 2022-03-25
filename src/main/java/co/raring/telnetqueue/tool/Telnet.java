package co.raring.telnetqueue.tool;

import co.raring.telnetqueue.TQMain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Telnet {
    private final String host;
    private final int port;

    private Socket sock;
    private BufferedReader br;
    private PrintWriter out;

    private Set<TReadHandler> handlers;
    private Set<TReadHandler> handRemove;
    private boolean readerRun = false;
    private final Thread reader = new Thread(() -> {
        while (readerRun) {
            try {
                if (br.ready()) {
                    String line = br.readLine();
                    // Get rid of ConcurrentModificationException
                    if (!handRemove.isEmpty()) {
                        handlers.removeAll(handRemove);
                        handRemove.clear();
                    }
                    for (TReadHandler handler : handlers) {
                        handler.onResponse(line);
                    }
                }
                //if (!br.ready()) readerRun = false;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    });
    private StringBuilder readerOut;


    public Telnet(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean init() {
        try {
            this.sock = new Socket(this.host, this.port);
            this.out = new PrintWriter(this.sock.getOutputStream(), true);
            this.br = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
        } catch (IOException ex) {
            TQMain.throwError("Error while initializing connection to " + this.host + ":" + this.port, ex);
            return false;
        }

        // Add default listener to print to the console
        this.handRemove = new HashSet<>();
        this.handlers = new HashSet<>();
        TReadHandler defaultHandler = (res) -> {
            readerOut.append(res).append("\n");
            TQMain.LOGGER.trace(res);
        };

        this.readerOut = new StringBuilder();
        addListener(defaultHandler);
        this.reader.start();
        this.readerRun = true;
        // Flush connection chars, allows us to send commands
        sendCommand("");

        return true;
    }

    public void close() throws IOException {
        this.readerRun = false;
        handlers = new HashSet<>();
        this.br.close();
        this.out.close();
        this.sock.close();
    }

    public void sendCommand(String comm) {
        out.print(comm + "\r\n");
        out.flush();
    }

    public String sendMIUCommand(String comm, String miu, long timeout) {
        sendCommand(comm);
        if (!miu.contains("\n")) {
            return readUntil("[" + miu + "]", timeout);
        }
        //return readUntil("alt=");
        return readUntil(miu, timeout);
    }

    public void addListener(TReadHandler handler) {
        this.handlers.add(handler);
    }

    public void removeListener(TReadHandler handler) {
        //this.handlers.remove(handler);
        this.handRemove.add(handler);
    }

    public String readUntil(String pattern, long timeoutMs) {
        StringBuilder buffer = new StringBuilder();
        TReadHandler temp = (res) -> {
            if (res.contains(pattern)) {
                buffer.append("\u001a");
            }
        };
        addListener(temp);
        long currentTime = System.currentTimeMillis();
        // Wait for buffer to contain the defined EOF
        while (!buffer.toString().contains("\u001a")) {
            if (timeoutMs != -1 && (currentTime - System.currentTimeMillis()) >= timeoutMs) {
                TQMain.LOGGER.warn("Timeout reached with read pattern of: " + pattern);
                return "TQ: Timeout Reached";
            }
        }
        removeListener(temp);
        return buffer.toString();
    }

    public List<String> readGateways() {
        List<String> li = new ArrayList<>();
        TReadHandler temp = (res) -> {
            if (res.length() < 21 && res.startsWith("     ")) {
                if (res.substring(13).contains("OFFLINE")) {
                    li.add(res.substring(5, 10) + " (OFFLINE)");
                } else {
                    li.add(res.substring(5, 10));
                }
            }
        };
        addListener(temp);
        sendCommand("c status");
        readUntil("Database Status: OK", 1000);
        removeListener(temp);
        return li;
    }

    public String getOutput() {
        return this.readerOut.toString();
    }

}