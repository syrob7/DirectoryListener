
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

public class DirectoryListener implements Runnable {

    private static String WATCH_DIRECTORY_SERVICE;
    private static String DIRECTORY_DEV;
    private static String DIRECTORY_TEST;
    private static Path directoryPath;

    public static void main(String[] args) throws IOException {
        DirectoryListener directoryListener = new DirectoryListener();

        directoryListener.loadProperies();
        directoryPath = FileSystems.getDefault().getPath(WATCH_DIRECTORY_SERVICE);
        Thread thread = new Thread(directoryListener);

        thread.start();
    }

    public void run() {
        try {
            System.out.println("Program is running .....");
            createDirectory();

            WatchService watchService = directoryPath.getFileSystem().newWatchService();
            directoryPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            //Start infinite loop to watch changes on the directory
            while (true) {

                WatchKey watchKey = watchService.take();

                for (final WatchEvent<?> event : watchKey.pollEvents()) {
                    takeActionOnCreateEvent(event);
                }

                if (!watchKey.reset()) {
                    watchKey.cancel();
                    watchService.close();
                    System.out.println("Watch directory got deleted. Stop watching it.");

                    break;
                }
            }
        } catch (InterruptedException interruptedException) {
            System.out.println("Thread got interrupted:"+interruptedException);
            return;
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
    }

    private void createDirectory() {
        File srcFolderHome = new File(WATCH_DIRECTORY_SERVICE);
        if (!srcFolderHome.exists()) {
            srcFolderHome.mkdirs();
        }

        File srcFolderDev = new File(DIRECTORY_DEV);
        if (!srcFolderDev.exists()) {
            srcFolderDev.mkdirs();
        }

        File srcFolderTest = new File(DIRECTORY_TEST);
        if(!srcFolderTest.exists()) {
            srcFolderTest.mkdirs();
        }
    }

    private void takeActionOnCreateEvent(WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
            Path file = (Path) event.context();
            String fileCreated = directoryPath.toAbsolutePath().toString()
                    + "\\" + file.getFileName().toString();
            System.out.println("New entry created: " + fileCreated);

            try {
                Thread.sleep(500);
                BasicFileAttributes attr = Files.readAttributes(Paths.get(fileCreated), BasicFileAttributes.class);
                moveFile(attr.creationTime(), file.getFileName().toString());
            } catch (IOException ex) {
                System.out.println("Can't read file attributes:" + fileCreated);
                ex.printStackTrace();
            } catch (InterruptedException ex) {

            }
        }
    }

    private void moveFile(FileTime fileTime, String file) throws IOException {
        int fileCreatedHour = getFileCreatedHour(fileTime);

        String src = WATCH_DIRECTORY_SERVICE + file;
        if (fileCreatedHour%2 == 0 && file.endsWith(".jar")) {
            System.out.println("move to dev");

            String dest = DIRECTORY_DEV + file;

            Files.move(Paths.get(src),Paths.get(dest));
        } else if (fileCreatedHour%2 != 0 && file.endsWith(".jar")) {
            System.out.println("move to test");

            String dest = DIRECTORY_TEST + file;

            Files.move(Paths.get(src),Paths.get(dest));
        } else if (file.endsWith(".xml")) {
            System.out.println("move to dev");

            String dest = DIRECTORY_DEV + file;

            Files.move(Paths.get(src),Paths.get(dest));
        }
    }

    private int getFileCreatedHour(FileTime fileTime) {
        Date fileDate = new Date( fileTime.toMillis() );
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(fileDate);

        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    private void loadProperies() throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties");

        Properties prop = new Properties();

        // load a properties file
        prop.load(input);

        // get the property value and print it out
        WATCH_DIRECTORY_SERVICE = prop.getProperty("dir.home");
        DIRECTORY_DEV = prop.getProperty("dir.dev");
        DIRECTORY_TEST = prop.getProperty("dir.test");
    }
}
