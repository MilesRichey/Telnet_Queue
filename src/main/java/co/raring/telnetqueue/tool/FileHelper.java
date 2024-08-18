package co.raring.telnetqueue.tool;

import co.raring.telnetqueue.TQMain;

import java.io.*;

public class FileHelper {
    /**
     * Method to read a files content and show an error message if it's not possible
     *
     * @param file File to read data from
     * @return The contents of @file, or null if an exception is caught
     */
    // Non-FXML Methods
    public static String readFile(File file) {
        StringBuilder fileContents = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String buffer;
            while ((buffer = br.readLine()) != null) {
                fileContents.append(buffer);
                fileContents.append("\n");
            }
        } catch (IOException e) {
            TQMain.throwError("Unable to access file '" + file.getAbsolutePath() + "'", e);
            return null;
        }
        return fileContents.toString();
    }

    public static void appendFile(File file, String append) {
        try {
            file.createNewFile();
            FileWriter fw = new FileWriter(file);
            fw.append(append);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            TQMain.throwError("Unable to access file '" + file.getAbsolutePath() + "'", e);
        }
    }
}
