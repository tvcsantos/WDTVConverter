package pt.com.santos.wdtvconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import pt.com.santos.util.FileUtilities;
import org.apache.commons.exec.ExecuteException;
import pt.com.santos.util.Pair;
import pt.com.santos.util.io.ExecutableFile;
import pt.com.santos.util.system.SystemUtilities;
import pt.com.santos.util.Properties;
import pt.com.santos.util.exception.NotExecutableException;
import pt.com.santos.util.exception.UnsupportedOperatingSystemException;
import pt.com.santos.util.program.mkvtoolnix.Dcadec;
import pt.com.santos.util.program.mkvtoolnix.Eac3to;
import pt.com.santos.util.program.mkvtoolnix.MKVToolnix;
import pt.com.santos.util.ProcessListener;
import pt.com.santos.util.subtitle.MKVSubtitle;

public class WDTVUtils {

    private static class OperationResult<T> {

        protected long executionTime;
        protected T result;

        public OperationResult(long executionTime, T result) {
            this.executionTime = executionTime;
            this.result = result;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public T getResult() {
            return result;
        }
    }

    private WDTVUtils() {
        throw new UnsupportedOperationException();
    }

    protected static final Logger logger = Logger.getLogger(
            WDTVUtils.class.getName());

    public static Logger getLogger() {
        return logger;
    }

    public static long convert(File source, File destination, File tempDir,
            Configuration conf, ProcessListener listener)
            throws FileNotFoundException, IOException, UnsupportedOperatingSystemException,
            NotExecutableException, ExecuteException {
        long start = System.currentTimeMillis();
        logger.log(Level.INFO, "Started convertion of {0}", source.getName());
        String ext = FileUtilities.getExtension(source);

        List<File> toDelete = new LinkedList<File>();

        if (ext.equalsIgnoreCase("avi")) {
            File outFile = new File(tempDir, "tmp1.mkv");
            convertAVI2MKV(source, outFile, listener);
            source = outFile;
            toDelete.add(outFile);
        }

        logger.log(Level.INFO, "Obtaining MKV Info for {0}", source.getName());
        Properties root = MKVToolnix.getInstance().getMKVInfoProperties(source);

        logger.log(Level.INFO, 
                "Obtaining Audio and Video Tracks information for {0}", 
                source.getName());
        /** obtaining video, audio, and subtitle tracks info **/
        List<Properties> tracks = root.get("Segment").get(0).
                get("Segment tracks").get(0).get("A track");
        Properties video = null;
        List<Properties> audios = new LinkedList<Properties>();
        List<Properties> subs = new LinkedList<Properties>();
        for (Properties n : tracks) {
            String type = n.get("Track type").get(0).getValue();
            if (type.equals("video")) {
                video = n;
            } else if (type.equals("audio")) {
                audios.add(n);
            } else if (type.equals("subtitles")) {
                subs.add(n);
            }
        }

        List<String> keep = new LinkedList<String>();

        for (Properties a : audios) {
            keep.add(a.get("Track number").get(0).getValue());
        }

        OperationResult<List<Pair<File, Properties>>> extractDTSTracks =
                extractDTSTracks(audios, source, tempDir, listener);

        for (Pair<File, Properties> p : extractDTSTracks.getResult()) {
            convertDTS2AC3(p.getFst(), new File(tempDir,
                    FileUtilities.getNameWithoutExtension(p.getFst().getName())
                    + ".ac3"), conf.getBitRate(), listener);
            p.getFst().delete();
            toDelete.add(new File(tempDir,
                    FileUtilities.getNameWithoutExtension(p.getFst().getName())
                    + ".ac3"));
            keep.remove(p.getSnd().get("Track number").get(0).getValue());
        }

        List<String> newac3tracks = new LinkedList<String>();
        for (Pair<File, Properties> p : extractDTSTracks.getResult()) {
            Properties a = p.getSnd();
            List<Properties> get = a.get("Default flag");
            int flag = 0;
            if (get != null) flag = Integer.valueOf(get.get(0).getValue());
            get = a.get("Language");
            String lang = null;
            if (get != null) lang = get.get(0).getValue();
            //String id = a.get("Track number").get(0).getValue();
            if (flag == 1) {
                newac3tracks.add("--default-track");
                newac3tracks.add("0");
            }
            if (lang != null) {
                newac3tracks.add("--language");
                newac3tracks.add("0:" + lang);
            }
            newac3tracks.add("--compression");
            newac3tracks.add("0:none");
            newac3tracks.add(/*"\"" +*/new File(tempDir,
                    FileUtilities.getNameWithoutExtension(p.getFst().getName())
                    + ".ac3").getAbsolutePath() /*+ "\""*/);
        }

        List<String> arguments = new LinkedList<String>();
        arguments.add("-o");
        arguments.add(/*"\"" +*/destination.getAbsolutePath() /*+ "\""*/);
        if (!conf.isKeepOT()) {
            if (!keep.isEmpty()) {
                arguments.add("-a");
                Iterator<String> it = keep.iterator();
                String copy = "";
                if (it.hasNext()) {
                    copy += it.next();
                }
                while (it.hasNext()) {
                    copy += "," + it.next();
                }
                arguments.add(copy);
            } else arguments.add("-A");
        } //else default copy all
        /*if (!newac3tracks.isEmpty()) {
            if (!conf.isKeepOT()) {
                if (!keep.isEmpty()) {
                    arguments.add("-a");
                    Iterator<String> it = keep.iterator();
                    String copy = "";
                    if (it.hasNext()) {
                        copy += it.next();
                    }
                    while (it.hasNext()) {
                        copy += "," + it.next();
                    }
                    arguments.add(copy);
                } else arguments.add("-A");
            }
        }*/
        arguments.add("--compression");
        arguments.add(video.get("Track number").get(0).getValue() + ":none");
        arguments.add(/*"\"" +*/source.getAbsolutePath() /*+ "\""*/);
        arguments.addAll(newac3tracks);

        if (conf.hasSubtitles()) {
            Collection<MKVSubtitle> subsc = conf.getSubtitles();
            for (MKVSubtitle sub : subsc) {
                if (sub.isDefault()) {
                    arguments.add("--default-track");
                    arguments.add("0");
                }
                arguments.add("--language");
                arguments.add("0:" + sub.getLanguage().getISO6392code());
                arguments.add("--compression");
                arguments.add("0:none");
                arguments.add(/*"\"" +*/sub.getFile().getAbsolutePath() /*+ "\""*/);
            }
        }

        logger.log(Level.INFO, "Generating {0}", destination.getName());

        //if (!(newac3tracks.isEmpty() && !conf.hasSubtitles())) {
            ExecutableFile mkvmerge = MKVToolnix.getInstance().getMKVMerge();
            try {
                mkvmerge.addProcessListener(listener);
                mkvmerge.execute(arguments.toArray(new String[]{}));
            } finally {
                mkvmerge.removeListener(listener);
            }
        //} else FileUtils.copy(source, destination);

        logger.info("Cleaning temporary files");
        for (File f : toDelete) {
            f.delete();
            String parent = f.getParentFile().getAbsolutePath();
            String name = f.getName();
            int index = name.lastIndexOf(".");
            String fileExt = (index >= 0) ? name.substring(index + 1) : "";
            if (fileExt.compareToIgnoreCase("ac3") == 0) {
                if (index >= 0) {
                    name = name.substring(0, index);
                }
                new File(parent, name + " - Log.txt").delete();
            }
        }

        logger.log(Level.INFO, "Ended convertion of {0}", source.getName());
        long end = System.currentTimeMillis();
        return end - start;
    }

    public static long convertAVI2MKV(File source, File destination,
            ProcessListener listener)
            throws FileNotFoundException, IOException,
            UnsupportedOperatingSystemException, NotExecutableException, ExecuteException {
        long start = System.currentTimeMillis();
        logger.log(Level.INFO, "Started conversion of {0} to {1}", 
                new Object[]{source.getName(), destination.getName()});
        ExecutableFile mkvmerge = MKVToolnix.getInstance().getMKVMerge();
        try {
            mkvmerge.addProcessListener(listener);
            mkvmerge.execute(new String[]{"-o",
                    /*"\"" +*/ destination.getAbsolutePath() /*+ "\""*/,
                    /*"\"" +*/ source.getAbsolutePath() /*+ "\""*/});
        } finally {
            if (mkvmerge != null) mkvmerge.removeListener(listener);
        }
        long end = System.currentTimeMillis();
        logger.log(Level.INFO, "Ended conversion of {0} to {1}", 
                new Object[]{source.getName(), destination.getName()});
        return end - start;
    }

    private static OperationResult<List<Pair<File, Properties>>>
            extractDTSTracks(List<Properties> audios,
            File source, File destination, ProcessListener listener)
            throws FileNotFoundException, IOException, UnsupportedOperatingSystemException,
            NotExecutableException, ExecuteException {
        long start = System.currentTimeMillis();
        logger.log(Level.INFO, "Started DTS tracks extraction for {0}", 
                source.getName());

        List<Pair<File, Properties>> files =
                new LinkedList<Pair<File, Properties>>();

        List<String> arguments = new LinkedList<String>();
        arguments.add("tracks");
        arguments.add(source.getAbsolutePath());
        int dtscount = 0;
        for (Properties a : audios) {
            List<Properties> get = a.get("Codec ID");
            String codec = null;
            if (get != null) codec = get.get(0).getValue();
            if (codec != null && codec.equals("A_DTS")) {
                String id = a.get("Track number").get(0).getValue();
                File f = new File(destination, id + ".dts");
                arguments.add(id + ":" + /*"\'" +*/ f.getAbsolutePath() /*+ "\'"*/);
                files.add(new Pair<File, Properties>(f, a));
                dtscount++;
            }
        }

        if (arguments.size() > 2) {
            ExecutableFile mkvextract =
                    MKVToolnix.getInstance().getMKVExtract();
            try {
                mkvextract.addProcessListener(listener);
                mkvextract.execute(arguments.toArray(new String[]{}));
            } finally {
                if (mkvextract != null) mkvextract.removeListener(listener);
            }
        }

        if (dtscount == 0) {
            logger.info("No DTS tracks to extract");
        } else {
            logger.log(Level.INFO, "Extracted {0} DTS tracks for {1}", 
                    new Object[]{dtscount, source.getName()});
        }

        long end = System.currentTimeMillis();
        logger.log(Level.INFO, "Ended DTS tracks extraction for {0}", 
                source.getName());

        return new OperationResult<List<Pair<File, Properties>>>(
                end - start, files);
    }

    public static long convertDTS2AC3(File source, File destination,
            int bitRate, ProcessListener listener)
            throws UnsupportedOperatingSystemException, NotExecutableException,
            FileNotFoundException, ExecuteException, IOException {
        long start = System.currentTimeMillis();

        logger.log(Level.INFO, "Started conversion of {0} to {1}", 
                new Object[]{source.getName(), destination.getName()});

        ExecutableFile executable = null;
        String[] args = null;
        if (SystemUtilities.osIsWindows()) {
            executable = Eac3to.getInstance().getProgram();
            args = new String[]{ /*"\"" +*/source.getAbsolutePath() /*+ "\""*/,
                        /*"\"" +*/ destination.getAbsolutePath() /*+ "\""*/,
                        "-" + bitRate, "-libav"};
        } else if (SystemUtilities.osIsLinux()
                || SystemUtilities.osIsMac() || SystemUtilities.osIsSolaris()) {
            executable = Dcadec.getInstance().getProgram();
            args = new String[]{"-o", "wavall",
                        /*"\"" +*/ source.getAbsolutePath() /*+ "\""*/,
                        "|", "aften", "-b", bitRate + "", "-",
                        /*"\"" +*/ destination.getAbsolutePath() /*+ "\""*/};
        } else {
            throw new UnsupportedOperatingSystemException(SystemUtilities.USER_OS);
        }
        
        try {
            executable.addProcessListener(listener);
            executable.execute(args);
        } finally {
            if (executable != null) executable.removeListener(listener);
        }

        long end = System.currentTimeMillis();

        logger.log(Level.INFO, "Ended conversion of {0} to {1}", 
                new Object[]{source.getName(), destination.getName()});

        return end - start;
    }
}
