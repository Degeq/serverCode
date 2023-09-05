import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MessageBuilder {
    private String body;
    private String author;
    private String systemTime;
    private DateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy '@' HH:mm:ss");

    public void buildMessage(String body, String author) {
        this.body = body;
        this.author = author;
        this.systemTime = dateFormat.format(Calendar.getInstance().getTime());
    }

    public String getBody() {
        return body;
    }

    public String getNickname() {
        return author;
    }

    public String getMessageForLogger() {
        return "[ " + systemTime + ", " + author + ":] " + body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setNickname(String nickname) {
        this.author = nickname;
    }
}
