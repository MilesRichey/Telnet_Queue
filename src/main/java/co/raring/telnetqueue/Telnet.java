package co.raring.telnetqueue;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to initiate a Telnet connection to a Zenner utility client, and perform certain functions.
 */
public class Telnet {
    private final String serverName;
    private final int portNumber;
    private Socket sock;
    private PrintWriter out;
    private Reader in;

    /**
     * Configures a Telnet instance.
     * This won't connect to the server, just makes it possible(through {@code init}).
     * @param serverName IP Address of telnet configuration to connect to
     * @param portNumber Port Number of telnet configuration to connect to
     */
    public Telnet(String serverName, int portNumber) {
        this.serverName = serverName;
        this.portNumber = portNumber;
    }

    /**
     * Initialize a connection using {@code serverName} and {@code portNumber}
     * Once connection is established, read through gateways generic "MoTD"
     */
    public void init() {
        try {
            this.sock = new Socket(this.serverName, this.portNumber);
            this.out = new PrintWriter(this.sock.getOutputStream(), true);
            this.in = new InputStreamReader(this.sock.getInputStream());
            // Flush
            sendCommand("");
            // Figure out how to read all 'MOTD' messages, so we get the appropriate line read after command is sent
            readUntil("------------------------------------------------");
            readUntil("------------------------------------------------");
            readUntil("------------------------------------------------");
        } catch (IOException ex) {
            TQMain.throwError("Error while initializing connection to " + this.serverName + ":" + this.portNumber, ex);
        }
    }

    /**
     * Sends the command {@code c status} to query all gateways, both online and offline.
     *
     * @return {@code List<Integer>} of gateways under this connection
     * @throws IOException If there's an error while reading gateways
     */
    public List<Integer> readGateways() throws IOException {
        List<Integer> li = new ArrayList<>();
        sendCommand("c status");
        // Read output up until after c status finishes outputting
        String gws = this.readUntil("Database Status: OK");

        for (String z : gws.split("\n")) {
            // Spacing for a collector after running "c status"
            if (z.startsWith("     ")) {
                // Remove preceding spaces and just leave the Gateway sn
                li.add(Integer.valueOf(z.substring(5, 10)));
            }
        }
        return li;
    }

    /**
     * Reads {@code in} InputStream up until the specified pattern
     * @param pattern Pattern to search for
     * @return Each character up until pattern is found
     * @throws IOException If unable to access the InputStream, an exception will be thrown
     */
    public String readUntil(String pattern) throws IOException{
        char lastChar = pattern.charAt(pattern.length() - 1);
        StringBuilder sb = new StringBuilder();
        int c;

        while ((c = this.in.read()) != -1) {
            char ch = (char) c;
            //System.out.print(ch);
            sb.append(ch);
            if (ch == lastChar) {
                String str = sb.toString();
                if (str.endsWith(pattern)) {
                    return str.substring(0, str.length() -
                            pattern.length());
                }
            }
        }

        return null;
    }

    /**
     * Sends a command to the Telnet connection, and returns the ouput.
     *
     * @param comm Command to be sent to the utility
     * @return Output from command sent
     * @throws IOException If error occurs while searching for response
     */
    public String sendCommand(String comm) throws IOException {
        sendCommandNR(comm);
        return this.readUntil("\n");
    }

    /**
     * Sends a command to the Telnet connection, without expecting a response
     *
     * @param comm Command to be sent to the utility
     */
    public void sendCommandNR(String comm) {
        this.out.println(comm);
        this.out.flush();
    }

    /**
     * Closes the Telnet connection, it can be opened again using {@code init()}
     * @throws IOException In the case that it's unable to close the connection, an IOException will be thrown.
     */
    public void close() throws IOException {
        this.in.close();
        this.out.close();
        this.sock.close();
    }
}
