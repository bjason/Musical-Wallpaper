import java.io.*;
import java.util.Properties;

/// Retrieve and modify user preferences
public class PropertiesManager {

    private static final String propertiesPath = System.getProperty("user.home") + File.separator + "Pictures"
                    + File.separator + "Spotify Playlist Visualizor" + File.separator + "userPrefs.properties";

    private static Properties getProperties() throws IOException {
        Properties properties = new Properties();
        // load in from userPrefs.properties
        File dirs = new File(propertiesPath.replace("userPrefs.properties", ""));
        dirs.mkdirs();

        File f = new File(propertiesPath);
        if (!f.exists()) {
            f.createNewFile();
        }
        properties.load(new FileReader(propertiesPath));
        return properties;
    }

    public static String getProperty(String prop) throws IOException {
        Properties properties = getProperties();
        return properties.getProperty(prop);
    }

    public static String getProperty(String prop, String defaultValue) throws IOException {
        Properties properties = getProperties();
        return properties.getProperty(prop, defaultValue);
    }

    public static void setProperty(String prop, String value) throws IOException {
        Properties properties = getProperties();
        properties.setProperty(prop, value);
        // save the updated properties to file
        FileWriter fileWriter = new FileWriter(propertiesPath);
        properties.store(fileWriter, null);
        fileWriter.close();
    }

//    public static void setAllPropertiesToDefault() throws IOException {
//        setProperty("imageSizeCode", "1"); // index between 0 and 2 representing spotify's album image size
//    }
}
