
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DirectoryListener implements Runnable {

    private static Path directoryPath;

    public static void main(String[] args) {

        directoryPath = FileSystems.getDefault().getPath("c:\\Demo\\Home\\");
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
        File srcFolderHome = new File("c:\\Demo\\Home");
        if (!srcFolderHome.exists()) {
            srcFolderHome.mkdirs();
        }

        File srcFolderDev = new File("c:\\Demo\\Dev");
        if (!srcFolderDev.exists()) {
            srcFolderDev.mkdirs();
        }

        File srcFolderTest = new File("c:\\Demo\\Test");
        if(!srcFolderTest.exists()) {
            srcFolderTest.mkdirs();
        }
    }

    private void takeActionOnCreateEvent(WatchEvent<?> event) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
            Path fileCreated = (Path) event.context();
            try {
                BasicFileAttributes attr = Files.readAttributes(fileCreated, BasicFileAttributes.class);
                moveFile(attr.creationTime(), fileCreated);
            } catch (IOException ex) {
                System.out.println("Can't read file attributes:" + fileCreated);
                ex.printStackTrace();
            }

            System.out.println("New entry created:" + fileCreated);
        }
    }

    private void moveFile(FileTime fileTime, Path file) throws IOException {
        Date fileDate = new Date( fileTime.toMillis() );
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(fileDate);

        int fileCreatedHour = calendar.get(Calendar.HOUR_OF_DAY);

        if (fileCreatedHour%2 == 0 && file.getFileName().toString().endsWith(".jar")) {
            System.out.println("move to dev");

            String src = "c:\\Demo\\Home\\" + file.getFileName();
            String dest = "c:\\Demo\\Dev\\" + file.getFileName();

            Files.move(Paths.get(src),Paths.get(dest));
        } else if (fileCreatedHour%2 != 0 && file.getFileName().toString().endsWith(".jar")) {
            System.out.println("move to test");

            String src = "c:\\Demo\\Home\\" + file.getFileName();
            String dest = "c:\\Demo\\Test\\" + file.getFileName();

            Files.move(Paths.get(src),Paths.get(dest));
        } else if (file.getFileName().toString().endsWith("xml")) {
            System.out.println("move to dev");

            String src = "c:\\Demo\\Home\\" + file.getFileName();
            String dest = "c:\\Demo\\Dev\\" + file.getFileName();

            Files.move(Paths.get(src),Paths.get(dest));
        }
    }
}
