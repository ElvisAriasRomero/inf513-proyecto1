# Sistema de Publicidad Universitaria · Grupo 13

Aplicación Java que se maneja **por correo electrónico**: el asunto del correo es el comando y la respuesta llega como tabla HTML.

```
Correo → MailVerificationThread → Interpreter/Analex → Aplication (permisos) → N* → D* → PostgreSQL
                                                             ↓
                                         SendEmailThread ← HtmlBuilder
ScrapingThread (cada 6 h) → métricas de nuestras publicaciones (CU7)
                 (1 vez al día) → páginas externas sospechosas (CU6)
```

- Correo del sistema: `grupo13sc@tecnoweb.org.bo`
- Base de datos: `db_grupo13sc` en `mail.tecnoweb.org.bo`
- Configuración en un solo lugar: `src/negociodato/utils/Constantes.java`

## Puesta en marcha

1. Ejecutar `script_bd.sql` en `db_grupo13sc` (crea las 11 tablas + `comando`, los roles, servicios de ejemplo y el Director).
2. En el script, el correo del Director (`INSERT INTO usuario`) es el que puede usar todos los comandos al inicio.
3. Definir la contraseña del grupo como variable de entorno (no está en el código): `$env:GRUPO_PASSWORD="..."` en PowerShell.
4. Abrir la carpeta en NetBeans y ejecutar `main.Main`. Las librerías ya están en `lib/` (javax.mail, activation y el driver de PostgreSQL).
5. Enviar un correo a `grupo13sc@tecnoweb.org.bo` con el asunto `HELP`.

## Formato de los comandos (el del enunciado)

```
CODIGO["parametro1","parametro2",...]
```

- El código junta **acción + tabla** en 6 letras: `LISUSU` = listar usuarios, `INSCOT` = insertar cotización.
- Los parámetros van entre comillas y separados por comas. Dentro de las comillas se puede escribir cualquier cosa (tildes, ñ, comas, símbolos) menos otra comilla doble.
- `["*"]` significa "todos" o "sin filtro": `LISUSU["*"]`. Un parámetro opcional vacío se escribe `"-"`.
- Las fechas van como `"AAAA-MM-DD HH:MM"`.
- Los archivos (material, comprobante QR) se envían como **enlace** (Drive, Dropbox, etc.).
- No importan mayúsculas o minúsculas. `HELP` devuelve la lista completa con ejemplos (tabla `comando`).

| Acción | | Tabla | |
|---|---|---|---|
| `INS` insertar | `MOD` modificar | `ROL` rol | `USU` usuario |
| `ELI` eliminar | `LIS` listar | `CUE` cuenta de red social | `SER` servicio |
| `VER` ver | `APR` aprobar | `COT` cotización | `DET` detalle de cotización |
| `REC` rechazar | `PAG` pagar | `CUO` cuota | `MAT` material |
| `QRP` QR de pago | `VEN` vencidas | `PAU` pauta | `PAG` página detectada |
| `PUB` publicar | `CAN` cancelar | `MET` métrica | |
| `ESC` escanear | `DEN` denunciar | | |
| `DES` descartar | `CAP` capturar | | |

Reportes: `REPRED` (redes), `REPGRA` (gráficas), `REPTIP` (tipos), `REPMEN` (mensual), `REPPAG` (páginas), `REPCOB` (cobranza), `REPCOT` (cotizaciones).

## Roles

El rol se obtiene del correo de quien envía el comando (tabla `usuario`).

| Rol | Puede |
|---|---|
| Director | Todo |
| Encargado | Todo menos administrar roles y desactivar usuarios |
| Cliente | Ver sus cotizaciones, servicios y cuotas; aceptar o rechazar sus cotizaciones; pagar sus cuotas con QR; subir su material |
| No registrado | Solo `HELP` |

## Flujo completo de ejemplo

| # | Quién | Asunto del correo |
|---|---|---|
| 1 | Director | `INSUSU["2","Ana","Rojas","456789","ana@gmail.com","70011122","ana","clave123"]` (Encargado) |
| 2 | Encargado | `INSCUE["TikTok","UAGRM Oficial","https://www.tiktok.com/@uagrm","-"]` |
| 3 | Encargado | `INSUSU["3","Juan","Pérez","7894561","juan@gmail.com","76543210","juanp","clave123"]` (Cliente) |
| 4 | Encargado | `INSCOT["3","cuotas","3"]` |
| 5 | Encargado | `INSDET["1","3","2"]` y `INSDET["1","4","1"]` → total 900 Bs |
| 6 | Cliente | `APRCOT["1"]` → se crean 3 cuotas de 300 Bs |
| 7 | Cliente | `QRPCUO["1"]` → le llega el QR de PagoFácil; al pagar, la cuota se registra sola |
| 8 | Cliente | `INSMAT["1","Banner Feria","https://drive.google.com/banner","video"]` |
| 9 | Encargado | `APRMAT["1"]` |
| 10 | Encargado | `INSPAU["1","1","reel","2026-10-15 18:00"]` |
| 11 | Encargado | `PUBPAU["1","https://www.tiktok.com/@uagrm/video/123"]` |
| 12 | Sistema | Cada 6 h guarda las vistas, reacciones, comentarios y compartidos de la pauta 1 |
| 13 | Director | `REPRED["*"]` → tabla y gráficas de la red más efectiva |
| 14 | Director | `LISPAG["*"]` → `DENPAG["3"]` / `DESPAG["4"]` |

## Reglas que cuida el sistema

- La cotización solo se modifica mientras está **pendiente**; al aprobarla se generan las cuotas (al contado, 1 cuota).
- Solo efectivo o QR; el pago con QR exige número de transacción y comprobante.
- Cuando se pagan todas las cuotas, la cotización pasa a **pagada**. Las cuotas que pasan su fecha se marcan **vencidas**.
- El material se puede subir cuando la cotización tiene al menos una cuota pagada.
- La pauta necesita material **aprobado**. En una misma cuenta los horarios no se cruzan (un post, historia o reel ocupa 1 minuto; un en vivo, de inicio a fin). Lo valida el sistema y también la base de datos.
- El cliente solo ve y opera sus propias cotizaciones.

## Pago con QR de PagoFácil (CU3)

Es el mismo flujo del proyecto taller-web (`PagoFacilService` + webhook), adaptado al correo (`src/pagos/`).

```
Cliente: QRPCUO["5"]
   → PagoFácil /login (tcTokenService, tcTokenSecret) → accessToken
   → PagoFácil /generate-qr (tcCommerceId, pedido CUOTA-5-<hora>) → imagen del QR
   → el QR llega como imagen dentro del correo de respuesta
Cliente paga con la app de su banco
   → PagoFácil avisa a  <PAGOFACIL_CALLBACK_URL>  (Estado = 2, PedidoID = CUOTA-5-<hora>)
   → la cuota queda pagada sola (metodo QR) y al cliente le llega la confirmación
   → si era la última cuota, la cotización pasa a pagada
```

Configuración en `Constantes.java` o en variables de entorno con los mismos nombres del `.env` de taller-web:

| Variable | Qué es |
|---|---|
| `PAGOFACIL_COMMERCE_ID`, `PAGOFACIL_TOKEN_SERVICE`, `PAGOFACIL_TOKEN_SECRET` | Credenciales del comercio (las mismas de taller-web) |
| `PAGOFACIL_CALLBACK_URL` | URL pública que llega al webhook, por ejemplo `https://xxxx.ngrok-free.app/pagofacil/webhook` |
| `PAGOFACIL_PUERTO` | Puerto local del webhook (8080). Exponerlo con `ngrok http 8080` |
| `PAGOFACIL_MODO_PRUEBA` | `true`: al banco se le cobra 0.01 Bs, igual que en taller-web |

Si el webhook no está expuesto a internet, el QR igual se genera y se paga; en ese caso el Encargado registra el pago a mano con `PAGCUO["id","QR","nro_transaccion","enlace_comprobante"]`.

## Scraping (CU6 y CU7)

Usa el `HttpClient` de Java, sin librerías extra (`src/scraping/Scraper.java`).

- **Métricas propias:** entra al `url_publicacion` de cada pauta publicada en los últimos 30 días y lee los contadores públicos. Si una red no los muestra (por ejemplo, Facebook sin sesión), se pueden registrar con `INSMET[...]`.
- **Páginas externas:** busca `UAGRM site:facebook.com` (y las otras 3 redes) en DuckDuckGo, descarta las cuentas oficiales y guarda las nuevas con sus seguidores. La palabra clave se cambia en `Constantes.PALABRA_CLAVE` o se envía en `ESCPAG["palabra"]`.
- Las redes cambian su HTML con el tiempo y no permiten el scraping en sus términos. Para producción conviene usar sus APIs oficiales con el `token_acceso` de cada cuenta.

## Verificación

`test/Verificacion.java` prueba el intérprete, el scraping y los permisos sin necesitar base de datos ni correo.
En NetBeans: clic derecho sobre el archivo → **Run File**. Debe imprimir `OK: todas las verificaciones pasaron`.
