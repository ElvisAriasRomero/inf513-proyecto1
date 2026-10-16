            package coneccion.utils;

public class Email {
    public static final String SUBJECT = "Respuesta - Grupo 13";
    private String from;
    private String to;
    private String subject;
    private String message;
    private String imagenBase64; // PNG que el HTML muestra con <img src="cid:imagen">

    public Email(){
    }

    public Email(String to, String subject, String message) {
        this.to = to;
        this.subject = subject;
        this.message = message;
    }

    public Email(String to, String subject, String message, String imagenBase64) {
        this(to, subject, message);
        this.imagenBase64 = imagenBase64;
    }

    public String getImagenBase64() {
        return imagenBase64;
    }

    public Email(String from, String subject) {
        this.from = from;
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "[From: " + from + ", To: " + to + ", Subject: " + subject + ", Message: " + message + "]";
    }
}
