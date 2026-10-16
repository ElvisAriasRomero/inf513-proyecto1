package interpreter.events;

import interpreter.analex.utils.Token;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

/**
 * Comando ya interpretado: acción, parámetros y correo de quien lo envió.
 */
public class TokenEvent extends EventObject {

    private int action;
    private final List<String> params = new ArrayList<>();
    private final String sender;

    public TokenEvent(Object source, String sender) {
        super(source);
        this.sender = sender;
    }

    public List<String> getParams() {
        return params;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public String getSender() {
        return sender;
    }

    /** Guarda también los vacíos para que cada parámetro conserve su posición. */
    public void addParams(String param) {
        params.add(param);
    }

    @Override
    public String toString() {
        return "Remitente= " + sender + "\nAction= " + new Token().getStringToken(action) + "\nParams= " + params;
    }
}
