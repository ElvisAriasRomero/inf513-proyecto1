package coneccion.utils;

import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

public class Extractor {
    private final static String GMAIL = "d=gmail";
    private final static String HOTMAIL = "d=hotmail";
    private final static String YAHOO = "d=yahoo";
    private final static String INSTITUTIONAL = "uagrm.edu.bo";
    private final static String INSTITUTIONAL2 = "student.cbasantacruz.edu.bo";

    public static Email getEmail(String plain_text){
        return new Email(getFrom(plain_text), getSubject(plain_text));
    }
    
    private static String getFrom(String plain_text){
        String search = "Return-Path: <";
        int index_begin = plain_text.indexOf(search) + search.length();
        int index_end = plain_text.indexOf(">", index_begin);
        return plain_text.substring(index_begin, index_end);
    }
    
    private static String getTo(String plain_text){
        String to = "";
        if (plain_text.contains(GMAIL)) {
            to = getToFromGmail(plain_text);
        } else if (plain_text.contains(HOTMAIL)) {
            to = getToFromHotmail(plain_text);
        } else if (plain_text.contains(YAHOO)) {
            to = getToFromYahoo(plain_text);
        } else if (plain_text.contains(INSTITUTIONAL)) {
            to = getToFromInstitutional(plain_text);
        }
        else if (plain_text.contains(INSTITUTIONAL2)) {
            to = getToFromInstitutional(plain_text);
        }
        return to;
    }

    private static String getSubject(String plain_text){
        String search = "Subject: ";
        int i = plain_text.indexOf(search) + search.length();
        String end_string = "";

        if (plain_text.contains(GMAIL)) {
            end_string = "To:";
        } else if (plain_text.contains(HOTMAIL)) {
            end_string = "Thread-Topic";
        } else if (plain_text.contains(YAHOO)) {
            end_string = "MIME-Version:";
        } else if (plain_text.contains(INSTITUTIONAL)) {
            end_string = "To:";
        }
        else if (plain_text.contains(INSTITUTIONAL2)) {
            end_string = "To:";
        }

        int e = plain_text.indexOf(end_string, i);
        if (e == -1) {
            e = plain_text.length();
        }
        String subject = plain_text.substring(i, e).trim();
        
        // Decodificar el subject si está codificado en MIME
        try {
            subject = MimeUtility.decodeText(subject);
        } catch (UnsupportedEncodingException ex) {
            // Si falla la decodificación, usar el subject original
            System.err.println("Error al decodificar subject: " + ex.getMessage());
        }
        
        return subject;
    }
    
    private static String getToFromGmail(String plain_text){
        return getToCommon(plain_text);
    }
    
    private static String getToFromHotmail(String plain_text){
        String aux = getToCommon(plain_text);
        return aux.substring(1, aux.length() - 1);
    }
    
    private static String getToFromYahoo(String plain_text){
        int index = plain_text.indexOf("To: ");
        int i = plain_text.indexOf("<", index);
        int e = plain_text.indexOf(">", i);
        return plain_text.substring(i + 1, e);
    }
    
    private static String getToFromInstitutional(String plain_text){
        return getToCommon(plain_text);
    }
    
    private static String getToCommon(String plain_text){
        String aux = "To: ";
        int index_begin = plain_text.indexOf(aux) + aux.length();
        int index_end = plain_text.indexOf("\n", index_begin);
        return plain_text.substring(index_begin, index_end).trim();
    }
}
