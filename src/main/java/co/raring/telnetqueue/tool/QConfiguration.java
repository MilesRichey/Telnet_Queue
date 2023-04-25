package co.raring.telnetqueue.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

// TODO: Implement saving of queue configurations
public class QConfiguration {
    private Properties config;

    /*
    What needs to be stored to Save/Load a Queue?
    1. Client (IP, Port)
    2. Gateway (sn/ALL)
    3. MIU List
    4. Commands
    5. Wait Method
    6. If timed: command wait time
    7. Progress(i,j,and z from TabController)
    - Should we store one tab instance config or all opened?
    - Probably just the single tab instance. Reasons: significantly easier, more customizable.
    What format should it be stored in? Serialized, Plain text?
    * Java Properties file should be fine with current data needed to initialize a Queue
    */
    private String client = "Addison Training";
    private String gw = "";
    //@Nullable
    private String[] miuList = {};
    private String[] commands = {};
    private boolean responseBased = false; // False = Timed wait method, True = Response wait method
    private int waitPeriod = 30; // If wait period is set to -1, then N/A
    private int i = 0;
    private int j = 0;
    private int z = 0;
    public static void main(String... args) throws IOException {
        QConfiguration c = new QConfiguration(new File("C:\\Users\\mrichey\\Dropbox (ZennerUSA)\\Richey-Miles\\Projects\\TelnetQueue\\src\\main\\resources\\exampleConfig.properties"));
        System.out.println(c.toString());

    }
    public QConfiguration() {

    }

    public QConfiguration(File file) throws IOException { // Take in a file and parse it somehow
        this.config = new Properties();
        InputStream is = new FileInputStream(file);
        this.config.load(is);
        is.close();

        this.client = this.config.getProperty("client");
        this.gw = this.config.getProperty("gw");
        this.miuList = this.config.getProperty("remainingNodes").split(",");
        this.commands = this.config.getProperty("commands").split("\\|"); // NOTE: Separate with pipe character to prevent command inconsistencies
        this.responseBased = Boolean.parseBoolean(this.config.getProperty("responseBased"));
        this.waitPeriod = Integer.parseInt(this.config.getProperty("waitPeriod"));
        this.i = Integer.parseInt(this.config.getProperty("i"));
        this.j = Integer.parseInt(this.config.getProperty("j"));
        this.z = Integer.parseInt(this.config.getProperty("z"));
    }

    /**
     * Commands as defined in the TelnetQueue configuration, must iterate atleast once (i>=0 | i < totalCommands)
     *
     * @return Command Index relative to {@code TabController.java}
     */
    public int getI() {
        return this.i;
    }

    public void setI(int i) {
        this.i = i;
    }

    /**
     * If a MIU list is given, then an iteration will occur(j>=0 | j < totalMius) ~ Else: j = 0
     *
     * @return MIU Index relative to {@code TabController.java}
     */
    public int getJ() {
        return this.j;
    }
    public void setJ(int j) {this.j = j;}

    /**
     * If GW is set to ALL, then an iteration will occur(z>=0 | z < totalGateways) ~ Else: z=0
     *
     * @return Gateway index relative to {@code TabController.java}
     */
    public int getZ() {
        return this.z;
    }
    public void setZ(int z) {this.z = z;}

    /**
     * Simple getter for wait period
     *
     * @return Queue wait period as defined in the Telnet Queue configuration
     */
    public int getWaitPeriod() {
        return this.waitPeriod;
    }
    public void setWaitPeriod(int wait) { this.waitPeriod = wait;}

    /**
     * Simple getter for commands
     *
     * @return List of commands as defined in the Telnet Queue configuration
     */
    public String[] getCommands() {
        return this.commands;
    }
    public void setCommands(String[] commands) {this.commands = commands;}
    /**
     * Simple getter for MIU list, possible to return null.
     *
     * @return List of MIUs to apply to commands
     */
    public String[] getMIUList() {
        return this.miuList;
    }
    public void setMIUList(String[] miuList) { this.miuList = miuList;}

    /**
     * Simple getter for the selected gateway
     *
     * @return "ALL" or 5 Digit Gateway SN
     */
    public String getGW() {
        return this.gw;
    }
    public void setGW(String gw) {
        this.gw = gw;
    }

    /**
     * Simple getter to return the connection information for the specified client
     *
     * @return IP Address:Port
     */
    public String getClient() {
        return this.client;
    }
    public void setClient(String client) {
        this.client = client;
    }
    public boolean isResponseBased() {
        return responseBased;
    }
    public void setResponseBased(boolean respBased) {
        this.responseBased = respBased;
    }
    @Override
    public String toString() {
        return new StringBuilder("{\n")
                .append("'client':").append("'").append(getClient()).append("',\n")
                .append("'gw':").append("'").append(getGW()).append("',\n")
                .append("'remainingNodes':").append(Arrays.toString(getMIUList())).append(",\n")
                .append("'commands':").append(Arrays.toString(getCommands())).append(",\n")
                .append("'responseBased':").append(isResponseBased()).append(",\n")
                .append("'i':").append(getI()).append(",\n")
                .append("'j':").append(getJ()).append(",\n")
                .append("'z':").append(getZ())
                .append("\n}")
                .toString();
    }
}
