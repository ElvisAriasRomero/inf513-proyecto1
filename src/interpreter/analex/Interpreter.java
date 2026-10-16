package interpreter.analex;

import interpreter.analex.interfaces.ITokenEventListener;
import interpreter.analex.models.TokenCommand;
import interpreter.analex.utils.Token;
import interpreter.events.TokenEvent;

/**
 * Analiza el asunto del correo y llama al caso de uso que corresponde.
 */
public class Interpreter implements Runnable {

    private ITokenEventListener listener;
    private Analex analex;

    private final String command;
    private final String sender;

    public Interpreter(String command, String sender) {
        this.command = command;
        this.sender = sender;
    }

    public void setListener(ITokenEventListener listener) {
        this.listener = listener;
    }

    private void filterEvent(TokenCommand token_command) {
        TokenEvent token_event = new TokenEvent(this, sender);
        token_event.setAction(token_command.getAction());

        for (int i = 0; i < token_command.countParams(); i++) {
            token_event.addParams(analex.getParam(token_command.getParams(i)));
        }

        switch (token_command.getName()) {
            case Token.HELP:
                listener.help(token_event);
                break;
            case Token.ROL:
                listener.rol(token_event);
                break;
            case Token.USUARIO:
                listener.usuario(token_event);
                break;
            case Token.CUENTA:
                listener.cuenta(token_event);
                break;
            case Token.SERVICIO:
                listener.servicio(token_event);
                break;
            case Token.COTIZACION:
                listener.cotizacion(token_event);
                break;
            case Token.DETALLE:
                listener.detalle(token_event);
                break;
            case Token.CUOTA:
                listener.cuota(token_event);
                break;
            case Token.MATERIAL:
                listener.material(token_event);
                break;
            case Token.PAUTA:
                listener.pauta(token_event);
                break;
            case Token.PAGINA:
                listener.pagina(token_event);
                break;
            case Token.METRICA:
                listener.metrica(token_event);
                break;
            case Token.REPORTE:
                listener.reporte(token_event);
                break;
            default:
                tokenError(new Token(Token.ERROR, Token.ERROR_COMMAND), command.trim());
        }
    }

    private void tokenError(Token token, String error) {
        TokenEvent token_event = new TokenEvent(this, sender);
        token_event.setAction(token.getAttribute());
        token_event.addParams(command.trim());
        token_event.addParams(error);
        listener.error(token_event);
    }

    @Override
    public void run() {
        analex = new Analex(command);
        TokenCommand token_command = new TokenCommand();
        Token token;

        while ((token = analex.Preanalisis()).getName() != Token.END && token.getName() != Token.ERROR) {
            if (token.getName() == Token.CU) {
                token_command.setName(token.getAttribute());
            } else if (token.getName() == Token.ACTION) {
                token_command.setAction(token.getAttribute());
            } else if (token.getName() == Token.PARAMS) {
                token_command.addParams(token.getAttribute());
            }
            analex.next();
        }

        if (token.getName() == Token.END) {
            filterEvent(token_command);
        } else {
            tokenError(token, analex.lexeme());
        }
    }
}
