/*
 * WDTVConverterApp.java
 */

package pt.com.santos.wdtvconverter;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import pt.com.santos.util.FileUtilities;
import org.jdesktop.application.Application;
import pt.com.santos.util.appframework.DefaultSingleFrameApplication;
import pt.com.santos.util.appframework.Setting;
import pt.com.santos.util.subtitle.Language;

/**
 * The main class of the application.
 */
public class WDTVConverterApp extends DefaultSingleFrameApplication {

    public static final int BITRATE_192 = 192;
    public static final int BITRATE_286 = 286;
    public static final int BITRATE_384 = 384;
    public static final int BITRATE_448 = 448;
    public static final int BITRATE_640 = 640;

    @Setting protected File tempDirectory;
    @Setting protected boolean convertDTS2AC3;
    @Setting protected boolean keepOriginalTracks;
    @Setting protected int bitrate;

    @Override
    protected void initApplication() {
        super.initApplication();
        tempDirectory = settingsDirectory;
        bitrate = BITRATE_448;
    }

    @Override
    protected void preShutdown() {
        super.preShutdown();
        List<File> list = new LinkedList<File>();
        if (tempDirectory != null) list.add(tempDirectory);
        list = FileUtilities.listFiles(list, Integer.MAX_VALUE);
        list = FileUtilities.filter(list,
                new FileUtilities.ExtensionFileFilter(
                    "dts", "avi", "mkv", "ac3", "wav", "txt"));
        for (File f : list) f.deleteOnExit();
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of WDTVConverterApp
     */
    public static WDTVConverterApp getApplication() {
        return Application.getInstance(WDTVConverterApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(WDTVConverterApp.class, args);
    }
    
    @Override
    protected void createSettingsDirectory() {}

    @Override
    protected void populateSettingsDirectory() {}

    @Override
    protected void update() throws Exception {}

    @Override
    protected String initSettingsDirectory() {
        return ".wdtvconverter";
    }

    @Override
    protected URL initWebLocation() {
        return null;
    }

    @Override
    protected String initName() {
        return "WD TV Converter";
    }

    @Override
    protected Long initUpdateInterval() {
        return null;
    }

    protected static final Map<String, Language> LANGUAGES = loadLanguages();

    private static Map<String, Language> loadLanguages() {
        Map<String, Language> result = new HashMap<String, Language>();
        InputStream is = WDTVConverterApp.class.getResourceAsStream(
                "resources/languages.txt");
        if (is == null) return result;
        Scanner sc = new Scanner(is);
        int count = 0;
        Map<String, Integer> map = new HashMap<String,Integer>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (++count >= 2) {
                StringTokenizer st = new StringTokenizer(line,"\t\r\n");
                int tokens = st.countTokens();
                if (tokens < 4) continue;
                String code2 = st.nextToken().trim();
                String code2U = code2.toUpperCase();
                String code1 = null;
                boolean upEnabled = false;
                boolean webEnabled = false;
                if (tokens == 4) {
                    code1 = null;
                } else {
                    code1 = st.nextToken().trim();
                }
                String name = st.nextToken().trim();
                Integer a = Integer.parseInt(st.nextToken().trim());
                if (a.intValue() == 1) upEnabled = true;
                a = Integer.parseInt(st.nextToken().trim());
                if (a.intValue() == 1) webEnabled = true;
                if (code1 != null && code1.isEmpty()) code1 = null;
                if (!upEnabled || !webEnabled) continue;
                Integer value = map.get(code2U);
                if (value == null) value = new Integer(0);
                map.put(code2U, ++value);
                result.put(code2U + (value > 1 ? value : "")
                        ,new Language(name, code2, code1));
            }
        }
        return result;
    }

    public void setTempDirectory(File tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public File getTempDirectory() {
        return tempDirectory;
    }

    public boolean isConvertDTS2AC3() {
        return convertDTS2AC3;
    }

    public void setConvertDTS2AC3(boolean convertDTS2AC3) {
        this.convertDTS2AC3 = convertDTS2AC3;
    }

    public boolean isKeepOriginalTracks() {
        return keepOriginalTracks;
    }

    public void setKeepOriginalTracks(boolean keepOriginalTracks) {
        this.keepOriginalTracks = keepOriginalTracks;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }
}
