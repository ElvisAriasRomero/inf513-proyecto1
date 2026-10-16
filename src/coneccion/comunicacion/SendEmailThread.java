package coneccion.comunicacion;

import coneccion.utils.Email;
import java.util.Base64;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import negociodato.utils.Constantes;

/**
 * Envía la respuesta en HTML por SMTP.
 */
public class SendEmailThread implements Runnable {

    private final Email email;

    public SendEmailThread(Email email) {
        this.email = email;
    }

    @Override
    public void run() {
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.host", Constantes.HOST);
        properties.setProperty("mail.smtp.port", Constantes.SMTP_PORT);
        properties.setProperty("mail.smtp.tls.enable", "true");
        properties.setProperty("mail.smtp.auth", "false");

        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Constantes.GRUPO, Constantes.PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(Constantes.MAIL));
            message.setRecipients(MimeMessage.RecipientType.TO, new InternetAddress[]{new InternetAddress(email.getTo())});
            message.setSubject(email.getSubject(), "UTF-8");

            // "related" permite adjuntar la imagen del QR dentro del mismo HTML (Gmail bloquea las imágenes data:)
            Multipart multipart = new MimeMultipart(email.getImagenBase64() == null ? "alternative" : "related");
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(email.getMessage(), "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);
            if (email.getImagenBase64() != null) {
                MimeBodyPart imagen = new MimeBodyPart();
                imagen.setDataHandler(new DataHandler(new ByteArrayDataSource(
                        Base64.getMimeDecoder().decode(email.getImagenBase64()), "image/png")));
                imagen.setContentID("<imagen>");
                imagen.setDisposition(MimeBodyPart.INLINE);
                imagen.setFileName("qr.png");
                multipart.addBodyPart(imagen);
            }
            message.setContent(multipart);
            message.saveChanges();

            Transport.send(message);
            System.out.println("MENSAJE ENVIADO A " + email.getTo());
        } catch (MessagingException ex) {
            Logger.getLogger(SendEmailThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
