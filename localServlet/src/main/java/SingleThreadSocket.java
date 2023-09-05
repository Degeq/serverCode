import java.io.*;
import java.net.Socket;
import java.util.List;

public class SingleThreadSocket implements Runnable {
    private Socket clientSocket;
    private File log;
    private List<String> nicknames;
    private int counter;
//При создании объекта помимо клиентского сокета передаются данные общие для всех потоков (пользователей):
// файл-логгер, список никнеймов
    public SingleThreadSocket(Socket clientSocket, File log, List<String> nicknames) {
        this.clientSocket = clientSocket;
        this.log = log;
        this.nicknames = nicknames;
    }

    @Override
    public void run() {
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//Перед началом общения пользователь обязан ввести собственный уникальный ник, если ник уже занят,
// то запрашивается повторный ввод до тех пор пока не будет указан свободный ник (за это отвечает метод uniqueNickCheck
            out.println(String.format("Введите ник, кототрый вы хотите использовать в чате: "));
            String nickname = uniqueNickCheck(out, in);

            out.println(String.format("Добро пожаловать, " + nickname + "! Вы можете начать общение. " +
                    "Чтобы выйти из чата, введите: /exit. "));
            //Класс MessageBuilder помогает в хранении информации о пользователе в текущем потоке
            // (его ник, серверное время) и создает сообщения в требуемом для логирования формате
            MessageBuilder messageBuilder = new MessageBuilder();
            messageBuilder.buildMessage("Пользователь " + nickname + " присоединился к чату", "");

            //Поток, отвечающий за вывод сообщений на экране пользователя. Сообщения читаются из файла
            // логгирования и выводятся на экране пользователя. Поток запускается после успешной
            // авторизации пользователя (создания уникального ника). Выводятся только сообщения,
            // отправленные после авторизации пользователя - достигается за счет метода skip(), который, используя
            // MyServer.stringCounter - счетчик записей в файле-логгере, пропускает n первых строчек в файле,
            // записанных до авторизации пользователя.
            Thread threadPrinter = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new FileReader(log.getPath()))) {
                    //Вызов метода для переноса курсора на необходимую строку
                    skip(br, counter);
                    while (!clientSocket.isClosed()) {
                        String toPrint = br.readLine();
                        if (toPrint == null) {
//                            Thread.sleep(1000);
                        } else {
                            out.println(String.format(toPrint));
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            //Поток, обрабатывающий получаемые сообщения и записывающий в файл-логгер.
            Thread threadLogger = new Thread(() -> {
                try {
                    try (BufferedWriter bos = new BufferedWriter(new FileWriter(log.getPath(), true))) {
                        //Первым пользователю отправляет системное сообщение о том, что он присоединился к чату
                        synchronized (log) {
                            writer(bos, messageBuilder);
                            threadPrinter.start();
                            counter = MyServer.stringCounter.incrementAndGet() + 1;
                        }
                        //Обмен сообщениями идет до того момента, пока сокет не будет закрыт /exit или иным методом
                        while (!clientSocket.isClosed()) {
                            String message = in.readLine();
                            messageBuilder.buildMessage(message, nickname);
                            //Проверка на команду /exit
                            if (messageBuilder.getBody().equals("/exit")) {
                                threadPrinter.interrupt();
                                out.println(String.format("Вы покинули чат"));
                                messageBuilder.setBody("Пользователь" + nickname + " покинул чат");
                                messageBuilder.setNickname("");

                                synchronized (log) {
                                    //writer() - метод для записи сообщений в файл-логгер
                                    writer(bos, messageBuilder);
                                    //Увеличение глобального счетчика строк
                                    MyServer.stringCounter.getAndIncrement();
                                    bos.close();
                                }

                                break;
                            //Если введено любое другое сообщение производится обработка и его запись в файл-логгер
                            } else {

                                synchronized (log) {
                                    writer(bos, messageBuilder);
                                    //Увеличение глобального счетчика строк
                                    MyServer.stringCounter.getAndIncrement();
                                }

                            }
                        }

                        in.close();
                        out.close();
                        clientSocket.close();
                        Thread.currentThread().interrupt();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            threadLogger.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public static void skip(BufferedReader br, int i) {
        for (int j = 0; j < i; j++) {
            try {
                br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writer(BufferedWriter bos, MessageBuilder messageBuilder) throws IOException {
        bos.write(messageBuilder.getMessageForLogger());
        bos.append("\n");
        bos.flush();

    }

    private String uniqueNickCheck(PrintWriter out, BufferedReader in) throws IOException {

        String nickname = in.readLine();

        synchronized (nicknames) {
            while (nicknames.contains(nickname)) {
                out.println("false");
                out.println("Выбранный ник уже используется другим участником. Придумайте новый: ");
                nickname = in.readLine();
            }
            out.println("true");
            nicknames.add(nickname);
        }

        return nickname;
    }
}

