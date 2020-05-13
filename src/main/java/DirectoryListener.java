
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DirectoryListener implements Runnable {

    public static final String WATCH_DIRECTORY_SERVICE = "c:\\Demo\\Home\\";
    public static final String DIRECTORY_DEV = "c:\\Demo\\Dev\\";
    public static final String DIRECTORY_TEST = "c:\\Demo\\Test\\";
    private static Path directoryPath;

    public static void main(String[] args) {
        directoryPath = FileSystems.getDefault().getPath(WATCH_DIRECTORY_SERVICE);
        Thread thread = new Thread(new DirectoryListener());
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
        Date fileDate = new Date( fileTime.toMillis() );
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(fileDate);

        int fileCreatedHour = calendar.get(Calendar.HOUR_OF_DAY);

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
}
