import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MyServer {

    public static ExecutorService threadPool = Executors.newFixedThreadPool(16);
    public static File log = creationFile("logger.txt");
    public static File settings = creationFile("settings.txt");
    public static AtomicInteger stringCounter = new AtomicInteger(0);
    private int port;
    private List<String> nicknames = new ArrayList<>();

    public MyServer() throws IOException {

        System.out.println("Начнем работу сервера. Введите номер порта: ");
//Запись порта в файл вынесена в отдельный метод
        bindingPort();
    }

    public MyServer(int port) throws IOException {

        try (BufferedWriter wr = new BufferedWriter(new FileWriter(settings, true))) {
            wr.write(port);
            wr.append("\n");
            wr.flush();
        }

        this.port = port;
    }

    public void runServer() throws IOException {
        //В бесконечном цикле сервер ожидает подключение новых пользователей
        //Каждый вызов создает новый экземпляр SingleThreadSocket и запускает его метод run в новом потоке
        while (true) {
            try (ServerSocket server = new ServerSocket(port)) {
                    Socket clientSocket = server.accept();
                    threadPool.execute(new SingleThreadSocket(clientSocket, log, nicknames));
                    System.out.println("Новое подключение");
                } catch (IOException ex) {
                //Если порт оказывается занятым, происходит его переназначение и перезапись в файл settings
                    ex.printStackTrace();
                    System.out.println("Выбранный порт занят. Введите новый: ");
                    bindingPort();
            }
        }
    }

    public int getPort() {
        return port;
    }

    private void bindingPort() throws IOException {
        Scanner scanner = new Scanner(System.in);
        String number = scanner.nextLine();

        try (BufferedWriter wr = new BufferedWriter(new FileWriter(settings, true))) {
            wr.write(number);
            wr.append("\n");
            wr.flush();
        }

        this.port = Integer.parseInt(number);
    }

    public static File creationFile(String path) {
        File file = new File(path);
        try {
            if (file.createNewFile()) {
                System.out.println("Файл" + file.getName() + " создан");
            } else {
                file.delete();
                file.createNewFile();
                System.out.println("Файл" + file.getName() + " создан");
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return file;
    }

}
