package coneccion.interfaces;

import coneccion.utils.Email;
import java.util.List;

/**
 *
 * @author  Grupo 13
 */
public interface IEmailEventListener {
      void onReceiveEmailEvent(List<Email> emails);
}
