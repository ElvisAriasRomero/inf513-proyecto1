package coneccion.comunicacion;

import coneccion.interfaces.IEmailEventListener;
import coneccion.utils.Command;
import coneccion.utils.Email;
import coneccion.utils.Extractor;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.sasl.AuthenticationException;
import negociodato.utils.Constantes;

/**
 * Revisa el buzón por POP3 cada 10 segundos y entrega los correos nuevos.
 */
public class MailVerificationThread implements Runnable {

    private Socket socket;
    private BufferedReader input;
    private DataOutputStream output;

    private IEmailEventListener emailEventListener;

    public void setEmailEventListener(IEmailEventListener emailEventListener) {
        this.emailEventListener = emailEventListener;
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<Email> emails = null;
                socket = new Socket(Constantes.HOST, Constantes.POP_PORT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new DataOutputStream(socket.getOutputStream());

                authUser(Constantes.GRUPO, Constantes.PASSWORD);

                int count = getEmailCount();
                if (count > 0) {
                    emails = getEmails(count);
                    System.out.println(emails);
                    deleteEmails(count);
                }
                output.writeBytes(Command.quit());
                input.readLine();
                input.close();
                output.close();
                socket.close();

                if (count > 0) {
                    emailEventListener.onReceiveEmailEvent(emails);
                }
            } catch (IOException ex) {
                Logger.getLogger(MailVerificationThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    private void authUser(String email, String password) throws IOException {
        input.readLine();
        output.writeBytes(Command.user(email));
        input.readLine();
        output.writeBytes(Command.pass(password));
        String message = input.readLine();
        if (message.contains("-ERR")) {
            throw new AuthenticationException();
        }
    }

    private void deleteEmails(int emails) throws IOException {
        for (int i = 1; i <= emails; i++) {
            output.writeBytes(Command.dele(i));
            input.readLine();
        }
    }

    private int getEmailCount() throws IOException {
        output.writeBytes(Command.stat());
        String line = input.readLine();
        String[] data = line.split(" ");
        return Integer.parseInt(data[1]);
    }

    private List<Email> getEmails(int count) throws IOException {
        List<Email> emails = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            output.writeBytes(Command.retr(i));
            String text = readMultiline();
            emails.add(Extractor.getEmail(text));
        }
        return emails;
    }

    private String readMultiline() throws IOException {
        StringBuilder lines = new StringBuilder();
        while (true) {
            String line = input.readLine();
            if (line == null) {
                throw new IOException("Server no responde (ocurrio un error al abrir el correo)");
            }
            if (line.equals(".")) {
                break;
            }
            lines.append('\n').append(line);
        }
        return lines.toString();
    }
}
