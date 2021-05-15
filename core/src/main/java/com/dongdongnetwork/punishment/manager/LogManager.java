package com.dongdongnetwork.punishment.manager;
import com.dongdongnetwork.punishment.Universal;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
public class LogManager {
    private final File logsFolder;
    public LogManager() {
        Universal universal = Universal.get();
        logsFolder = new File(universal.getMethods().getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
        checkLastLog(true);
        File[] fList = logsFolder.listFiles();
        for (File file : fList) {
            if (file.isFile() && file.getName().contains(".gz") && (System.currentTimeMillis() - file.lastModified()) >= universal.getMethods().getInteger(universal.getMethods().getConfig(), "Log Purge Days") * 86400000L) {
                file.delete();
            }
        }
    }
    public final void checkLastLog(boolean force) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        File latestLog = new File(logsFolder, "latest.log");
        if (latestLog.exists()) {
            calendar.setTimeInMillis(latestLog.lastModified());
            if (day != calendar.get(Calendar.DAY_OF_MONTH) || force) {
                try {
                    if (FileUtils.readLines(latestLog, "UTF8").size() <= 0) {
                        return;
                    }
                    int filen = 1;
                    while (new File(logsFolder, sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz").exists()) {
                        filen++;
                    }
                    gzipFile(Files.newInputStream(latestLog.toPath()), logsFolder + "/" + sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz");
                    latestLog.delete();
                    latestLog.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(LogManager.class.getName()).log(Level.WARNING, "An unexpected error has occurred while trying to compress the latest log file. {0}", ex.getMessage());
                }
            }
        }
    }
    private void gzipFile(InputStream in, String to) throws IOException {
    	try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(to))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
    	} finally {
    		in.close();
    	}
    }
}
