package interpreter.analex;

import interpreter.analex.utils.Cinta;
import interpreter.analex.utils.TSParams;
import interpreter.analex.utils.Token;

/**
 * Analizador léxico del asunto del correo, con el formato del enunciado:
 *   CODIGO["parametro1","parametro2",...]      ejemplo: INSUSU["3","Juan","Peña"]
 *   CODIGO["*"]                                ejemplo: LISUSU["*"]
 *   HELP
 * Un código como LISUSU produce dos tokens: el caso de uso (usuario) y la acción (listar).
 */
public class Analex {

    public static final char EOF = 0;
    public static final char SPACE = 32;
    public static final char TAB = 9;
    public static final char NL = 10;
    public static final char CR = 13;
    public static final char COMILLA = '"';
    public static final char ESCAPE = '\\';

    private final Cinta cinta;
    private final TSParams tsp;
    private String ac;
    private Token R;
    private Token pendiente; // la acción que sigue al caso de uso del mismo código

    public Analex(String cinta) {
        this.cinta = new Cinta(cinta);
        tsp = new TSParams();
        init();
    }

    public void init() {
        cinta.init();
        tsp.init();
        pendiente = null;
        dt();
    }

    public Token Preanalisis() {
        return R;
    }

    public void next() {
        if (pendiente != null) {
            R = pendiente;
            pendiente = null;
        } else {
            dt();
        }
    }

    public String getParam(int pos) {
        return pos < 0 ? "" : tsp.getStr(pos);
    }

    public String lexeme() {
        return ac;
    }

    private boolean isSpace(char c) {
        return c == SPACE || c == TAB || c == NL || c == CR;
    }

    private boolean isLetter(char c) {
        return Character.isLetter(c);
    }

    private boolean isBeginParams(char c) {
        return c == '[';
    }

    private boolean isEndParams(char c) {
        return c == ']';
    }

    private boolean isComa(char c) {
        return c == ',';
    }

    private void saltarEspacios() {
        while (isSpace(cinta.CC())) {
            cinta.forward();
        }
    }

    private void dt() {
        saltarEspacios();
        char c = cinta.CC();

        if (c == EOF || isEndParams(c)) {
            if (isEndParams(c)) {
                // Después del corchete de cierre no puede venir nada más: CODIGO["a","b"]["c"] es sintaxis inválida
                cinta.forward();
                saltarEspacios();
                if (cinta.CC() != EOF) {
                    ac = "" + cinta.CC();
                    R = new Token(Token.ERROR, Token.ERROR_CHARACTER);
                    return;
                }
            }
            ac = "";
            R = new Token(Token.END);
            return;
        }

        if (isBeginParams(c) || isComa(c)) {
            cinta.forward();
            saltarEspacios();
            StringBuilder param = new StringBuilder();
            if (cinta.CC() == COMILLA) {
                // "con comillas": se acepta todo (comas, ;, ], tildes, @...) hasta la comilla de cierre.
                // Una comilla dentro del dato se escribe \" y una barra invertida \\
                cinta.forward();
                while ((c = cinta.CC()) != EOF && c != COMILLA) {
                    if (c < SPACE && !isSpace(c)) {
                        ac = "" + c;
                        R = new Token(Token.ERROR, Token.ERROR_CHARACTER);
                        return;
                    }
                    if (c == ESCAPE) {
                        cinta.forward();
                        char siguiente = cinta.CC();
                        if (siguiente == COMILLA || siguiente == ESCAPE) {
                            c = siguiente;            // \" → "   y   \\ → \
                        } else {
                            param.append(ESCAPE);     // cualquier otra \ se deja tal cual
                            continue;
                        }
                    }
                    param.append(c);
                    cinta.forward();
                }
                cinta.forward(); // comilla de cierre
                saltarEspacios();
            } else {
                // sin comillas: hasta la siguiente coma o el corchete
                while ((c = cinta.CC()) != EOF && !isComa(c) && !isEndParams(c)) {
                    param.append(c);
                    cinta.forward();
                }
            }
            ac = param.toString().trim();
            R = new Token(Token.PARAMS, tsp.add(ac));
            return;
        }

        if (isLetter(c)) {
            StringBuilder palabra = new StringBuilder();
            while (isLetter(c = cinta.CC())) {
                palabra.append(c);
                cinta.forward();
            }
            ac = palabra.toString();
            Token[] tokens = Token.comando(ac);
            if (tokens == null || !(c == EOF || isSpace(c) || isBeginParams(c))) {
                R = new Token(Token.ERROR, Token.ERROR_COMMAND);
                return;
            }
            R = tokens[0];
            pendiente = tokens.length > 1 ? tokens[1] : null;
            return;
        }

        ac = "" + c;
        R = new Token(Token.ERROR, Token.ERROR_COMMAND);
    }
}
