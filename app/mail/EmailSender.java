package mail;

import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;
import utils.ConfigUtils;

import java.io.File;
import java.io.IOException;

public class EmailSender {

    private static final String API_USER = ConfigUtils.parameter("sengrid.user");
    private static final String API_KEY = ConfigUtils.parameter("sendgrid.key");

    SendGrid sendgrid = new SendGrid(API_USER, API_KEY);

    public void sendFile(String filePath, String recipient) throws IOException {
        SendGrid.Email email = new SendGrid.Email();
        email.addTo(recipient);
        email.setFrom("kindle@keendly.com");
        email.setSubject("Your articles!");
        email.setText("Enjoy!");
        email.addAttachment("keendly.mobi", new File(filePath));
        try {
            sendgrid.send(email);
        } catch (SendGridException e) {
            // TODO log
            throw new RuntimeException(e);
        }
    }

}
