import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

import static org.hamcrest.MatcherAssert.assertThat;

public class SingleThreadSocketTest {

    private static MyServer server;
    private static int expectedCounter;

    static {
        try {
            server = new MyServer(8089);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File loggerFile = MyServer.log;

    //Запускается поток, имитирующий сервер
    @BeforeAll
    private static void creatingUserNServer() {
        Thread serverThread = new Thread(() -> {
            try {
                server.runServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        serverThread.start();

    }

    //Определение уникального никнейма
    @Test
    public void testNicknameAcception() throws IOException {
        //arrange
        String flag = null;
        final String testNickname = "Degeq";
        String expected = "true";

        //act
        try (Socket clientSocket = new Socket("localhost", server.getPort());
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            System.out.println(in.readLine());
            out.println(testNickname);
            flag = in.readLine();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //assert
        assertThat(flag, Matchers.equalTo(expected));
    }

    //Работа команды /exit
    @Test
    public void testExitOption() throws IOException {
        //arrange
        String flag = "";
        String expected = null;

        //act
        try (Socket clientSocket = new Socket("localhost", server.getPort());
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            System.out.println(in.readLine());
            out.println("testNickname");
            in.readLine();
            in.readLine();
            out.println("/exit");
            in.readLine();
            flag = in.readLine();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        //assert
        assertThat(flag, Matchers.equalTo(expected));
    }

    //Проверка работы метода при совпадении вводимого никнейма с одним из тех, что уже в списке
    @Test
    public void testNicknameRejection() throws IOException {
        //arrange
        String flag = null;
        final String testNickname = "Degeq";
        String expected = "false";

        //act
        try (Socket clientSocket = new Socket("localhost", server.getPort());
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ) {
            System.out.println(in.readLine());
            out.println(testNickname);
            expectedCounter = MyServer.stringCounter.get();
            flag = in.readLine();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //assert
        assertThat(flag, Matchers.equalTo(expected));
    }

    //Проверка работы метода для избежания печати сообщения, отправленных до авторизации пользователя
    @Test
    public void testSkip() throws IOException {
        //arrange
        int counter = 0;
        int expected = MyServer.stringCounter.get();

        //act
        try (BufferedReader br = new BufferedReader(new FileReader(loggerFile.getPath()))) {
            String line = br.readLine();
            while (line != null) {
                br.readLine();
                counter++;
            }
        }

        //assert
        assertThat(counter, Matchers.equalTo(expected));
    }


}