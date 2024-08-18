package co.raring.telnetqueue.jna;

import co.raring.telnetqueue.TQMain;
import com.sun.jna.platform.win32.Advapi32Util;

import java.util.Map;
import java.util.TreeMap;

import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;

/**
 * Class used to access Windows PuTTY sessions
 */
public class JNAReg {
    // Registry address where PuTTY Sessions are kept
    private static final String PUTTY_SESSIONS = "Software\\SimonTatham\\PuTTY\\Sessions\\";

    /**
     * Method to return all available PuTTY sessions using Java Native Access
     *
     * @return {@code Map<String, String>} of Utility Name, Host:Port
     */
    public static Map<String, String> getSessions() {
        TQMain.LOGGER.info("Querying PuTTY Sessions");
        long time = System.currentTimeMillis();
        // Get all available "utilities" in the PuTTY registry
        String[] keys = Advapi32Util.registryGetKeys(HKEY_CURRENT_USER, PUTTY_SESSIONS);

        Map<String, String> ent = new TreeMap<>();

        for (String key : keys) {
            String fullPath = PUTTY_SESSIONS + key;
            int port = -1;
            String host = "null";
            if (Advapi32Util.registryValueExists(HKEY_CURRENT_USER, fullPath, "HostName")) {
                key = key.replaceAll("%20", " ");
                host = Advapi32Util.registryGetStringValue(HKEY_CURRENT_USER, fullPath, "HostName");
                port = Advapi32Util.registryGetIntValue(HKEY_CURRENT_USER, fullPath, "PortNumber");
                if (!host.isEmpty() || port > 1) {
                    ent.put(key, host + ":" + port);
                }
                continue;
            }
            TQMain.LOGGER.warn("Invalid PuTTY Session found {} @ ({}:{})\n", key, host, port);
        }
        TQMain.LOGGER.info("Finished querying PuTTY Sessions({}ms)\n", System.currentTimeMillis() - time);
        return ent;
    }

}