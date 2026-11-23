# Sistema de Publicidad Universitaria · Grupo 13

**INF513 Tecnología Web · UAGRM FICCT · Proyecto 1: sistema vía correo electrónico (Email-Driven Architecture).**

El cliente interactúa con la base de datos **solo por correo**: el comando va en el **asunto**, el sistema lo valida, lo ejecuta en PostgreSQL y responde al remitente con **"Éxito"** y los datos, o con **"Error: [descripción del fallo]"**.

| | |
|---|---|
| Correo del sistema | `grupo13sc@tecnoweb.org.bo` |
| Servidor de correo y base de datos | `mail.tecnoweb.org.bo` (POP3 110, SMTP 25, PostgreSQL 5432) |
| Base de datos | `db_grupo13sc` (usuario `grupo13sc`) |
| Lenguaje | Java 11 (NetBeans / Ant) |
| Librerías (`lib/`) | `javax.mail.jar`, `activation.jar`, `postgresql-42.7.5.jar` |

---

## 1. Arquitectura

```
[Cliente / Remitente]
       │ correo con el comando en el Asunto
       ▼
[Servidor de correo: mail.tecnoweb.org.bo]
       │ POP3, cada 10 s
       ▼
[MailVerificationThread] ── lee y borra los correos nuevos ── access.log: RECIBIDO
       │
       ▼
[Interpreter + Analex] ──(sintaxis inválida / comando desconocido / cantidad de parámetros)──► correo "Error: ..."
       │ sintaxis válida
       ▼
[Aplication] ── Permisos (rol del remitente) ──(sin permiso)──► correo "Error: Acceso denegado ..."
       │
       ▼
[Capa de negocio N*] ── valida tipos y reglas ──(dato inválido)──► correo "Error: ..."
       │
       ▼
[Capa de datos D*] ── PreparedStatement ──► PostgreSQL
       ├── error de ejecución (dato repetido, referencia...) ──► correo "Error: ..."   + error.log
       └── operación exitosa ──────────────────────────────────► correo "Éxito" + datos + access.log
                                                                  (SendEmailThread por SMTP)
Tareas automáticas:
  ScrapingThread    cada 6 h  → métricas de nuestras publicaciones (CU7); 1 vez al día → páginas externas (CU6)
  VerificadorPagos  cada 10 s → consulta a PagoFácil los pagos QR pendientes (CU3)
```

**Capas del código (`src/`):**

| Paquete | Responsabilidad |
|---|---|
| `coneccion` | Correo: `MailVerificationThread` (POP3 con sockets), `SendEmailThread` (SMTP con JavaMail), `Extractor` (remitente y asunto) |
| `interpreter` | Analizador léxico (`Analex`), tokens y comandos (`Token`), intérprete (`Interpreter`) |
| `main` | `Aplication` (un método por caso de uso), `Permisos`, `HtmlBuilder` (respuestas HTML), `Bitacora` (logs), `Consola` (pruebas sin correo) |
| `negociodato.negocio` | Reglas del negocio (`N*`) |
| `negociodato.dato` | Acceso a la base con `PreparedStatement` (`D*`) |
| `pagos` | PagoFácil QR Master v2: cliente, callback y verificador |
| `scraping` | Scraping de métricas y de páginas externas |

---

## 2. Diagrama de flujo del analizador de correos

```
                        ┌──────────────────────────────┐
                        │ Cada 10 s: conectar por POP3  │
                        │ USER / PASS / STAT            │
                        └──────────────┬───────────────┘
                                       │ ¿hay correos?
                          no ┌─────────┴─────────┐ sí
                             ▼                   ▼
                       esperar 10 s     RETR de cada correo → extraer remitente (Return-Path)
                                        y asunto (decodificado UTF-8) → DELE → QUIT
                                                 │
                                                 ▼
                              ┌──────────── Analex (por cada asunto) ────────────┐
                              │ 1. Leer palabra de letras: ¿existe el código?     │──no──► Error: comando desconocido
                              │    (LISUSU → caso de uso USUARIO + acción LISTAR) │
                              │ 2. ¿Sigue "[" ?                                   │
                              │ 3. Leer cada parámetro:                           │
                              │      "con comillas" → hasta la comilla de cierre  │
                              │       \" = comilla dentro del dato, \\ = barra    │
                              │      separados por "," · se ignoran espacios      │──carácter de control──► Error: sintaxis
                              │ 4. "]" y nada más después                         │──texto después de "]"──► Error: sintaxis
                              └──────────────────────┬───────────────────────────┘
                                                     ▼
                              Interpreter: ¿la cantidad de parámetros es la exacta
                              para ese comando? (Token define mínimo y máximo)      ──no──► Error: cantidad de parámetros
                                                     │ sí
                                                     ▼
                              Aplication: rol del remitente → ¿tiene permiso?       ──no──► Error: acceso denegado
                                                     │ sí
                                                     ▼
                              Negocio: tipos (número, decimal, fecha, correo, URL,
                              opción permitida) y reglas (estados, choques de horario)──no──► Error: [detalle]
                                                     │ sí
                                                     ▼
                              Datos: PreparedStatement → PostgreSQL                 ──falla──► Error: [detalle SQL]
                                                     │ ok
                                                     ▼
                              Respuesta "Éxito" + access.log
```

---

## 3. Estructura de la base de datos

Script completo: [`script_bd.sql`](script_bd.sql) (PostgreSQL). **11 tablas del sistema + `comando` (ayuda).**

```
rol ─────────< usuario >──────────────────────────────┐
                 │  │                                  │ (quién registró / revisó / verificó:
                 │  └──< cuenta_red_social              │  ON DELETE SET NULL)
                 │            │                         │
   cliente_id ──<  cotizacion >── detalle_cotizacion >── servicio
   (CASCADE)        │      │
                    │      └──< cuota
                    └──< material_publicitario
                               │
          cuenta_red_social ──< pauta >──┘
                                  │
                                  └──< metrica
   pagina_detectada (independiente; revisado_por → usuario)
```

| Tabla | CU | Columnas principales | Relaciones |
|---|---|---|---|
| `rol` | CU1 | id, nombre | — |
| `usuario` | CU1 | id, rol_id, nombres, apellidos, ci_nit, email (único), telefono, username (único), password (SHA-256), fecha_creacion | rol |
| `cuenta_red_social` | CU2 | id, usuario_id, red_social (Facebook/Instagram/TikTok/YouTube), nombre_cuenta, url (única), token_acceso | usuario (SET NULL) |
| `servicio` | CU3 | id, nombre, descripcion, precio (3 decimales) | — |
| `cotizacion` | CU3 | id, cliente_id, usuario_id, fecha, total, tipo_pago (contado/cuotas), nro_cuotas, estado (pendiente al cargarla, aprobada, pagada) | usuario cliente (CASCADE), usuario autor (SET NULL) |
| `detalle_cotizacion` | CU3 | id, cotizacion_id, servicio_id, cantidad, precio_unitario, subtotal | cotizacion (CASCADE), servicio (CASCADE) |
| `cuota` | CU3 | id, cotizacion_id, nro_cuota, monto, fecha_vencimiento, fecha_pago, metodo (efectivo/QR), nro_transaccion, comprobante, estado (pendiente/pagada/vencida), verificado_por | cotizacion (CASCADE), usuario (SET NULL) |
| `material_publicitario` | CU4 | id, cotizacion_id, titulo, archivo (enlace), tipo (imagen/video), estado (pendiente/aprobado/rechazado), validado_por, observacion, fecha_carga | cotizacion (CASCADE), usuario (SET NULL) |
| `pauta` | CU5 | id, material_id, cuenta_red_social_id, tipo (post/historia/reel/en_vivo), fecha_hora_inicio, fecha_hora_fin, url_publicacion, estado (programada/publicada/cancelada) | material (CASCADE), cuenta (CASCADE) |
| `pagina_detectada` | CU6 | id, red_social, nombre_pagina, url (única), palabra_clave, seguidores, estado (detectada/denunciada/descartada), revisado_por, fecha_deteccion | usuario (SET NULL) |
| `metrica` | CU7 | id, pauta_id, fecha_captura, vistas, reacciones, comentarios, compartidos | pauta (CASCADE) |
| `comando` | HELP | id, caso_uso, accion, parametro (plantilla), ejemplo, roles | — |

**CU8 (reportes)** no tiene tabla: consulta las anteriores.

**Reglas en la base:** restricciones `CHECK` de estados y valores; una misma cuenta no puede tener dos en vivo en horarios cruzados (post, historia y reel sí pueden coincidir) (`EXCLUDE USING gist`, extensión `btree_gist`); al eliminar, lo que pertenece al registro se borra en cascada y las referencias a "quién lo hizo" quedan vacías.

---

## 4. Instalación y configuración

1. **Base de datos:** ejecutar el script en `db_grupo13sc` (ojo: borra y vuelve a crear las 12 tablas del sistema; no toca otras tablas):
   ```powershell
   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -h mail.tecnoweb.org.bo -U grupo13sc -d db_grupo13sc -f script_bd.sql
   ```
   El script crea los 3 roles, los servicios de ejemplo, la ayuda y un Director. **El correo del Director** es el que puede usar todos los comandos al inicio.
2. **Contraseña del grupo** (no está en el código; se lee de una variable de entorno):
   ```powershell
   $env:GRUPO_PASSWORD="..."
   ```
3. **PagoFácil (opcional):**
   ```powershell
   $env:PAGOFACIL_TOKEN_SERVICE="..."; $env:PAGOFACIL_TOKEN_SECRET="..."
   ```
4. **Compilar y ejecutar** (o abrir la carpeta en NetBeans y ejecutar `main.Main`):
   ```powershell
   javac -encoding UTF-8 -d build/classes -cp "lib/*" (Get-ChildItem -Recurse src -Filter *.java).FullName
   java -cp "build/classes;lib/*" main.Main
   ```
5. Enviar un correo a `grupo13sc@tecnoweb.org.bo` con el asunto `HELP`.

**Para probar sin correo** (misma lógica, permisos y base): `java -cp "build/classes;lib/*" main.Consola correo@remitente.com`. Las respuestas salen en la terminal y en `respuestas/`.

**Base de datos local (opcional):** `$env:DB_HOST="localhost"; $env:DB_USER="postgres"; $env:DB_PASSWORD="..."`.

---

## 5. Formato de los comandos

```
CODIGO["parametro1","parametro2",...]
```

- El código junta **acción + tabla** en 6 letras: `LISUSU` = listar usuarios, `INSCOT` = insertar cotización.
- Parámetros **entre comillas** y separados por **comas**. Se aceptan espacios de más, tildes, ñ y símbolos (UTF-8).
- **Comilla dentro de un dato:** se escribe `\"`, por ejemplo `INSMAT["1","Banner \"Feria\"","https://...","imagen"]`. Una barra invertida se escribe `\\`.
- **Cantidad exacta de parámetros:** cada comando define cuántos acepta. Si faltan o sobran, responde *"Error: Cantidad de parámetros incorrecta: INSUSU espera 8 parámetro(s) y recibió 2."*
- Después del `]` no puede venir nada más.
- `["*"]` = "todos"; un parámetro opcional vacío se escribe `"-"`; fechas `"AAAA-MM-DD HH:MM"`.
- No importan mayúsculas o minúsculas. **`HELP`** devuelve los comandos que el rol del remitente puede usar, con su plantilla lista para copiar.

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
| `REG` registrarse | `ASI` asignar rol | | |

Reportes: `REPRED` (redes), `REPGRA` (gráficas), `REPTIP` (tipos), `REPMEN` (mensual), `REPPAG` (páginas), `REPCOB` (cobranza), `REPCOT` (cotizaciones).

### Respuestas

| Resultado | Correo |
|---|---|
| Éxito | Título **"Éxito"** y *"Éxito: Usuario 3 registrado"*, o la tabla con los datos pedidos |
| Error | Título **"Error"** y *"Error: [descripción]"*: comando desconocido, sintaxis inválida, cantidad de parámetros, acceso denegado, tipo de dato, dato repetido (*"El username 'elvis' ya está registrado"*), etc. |

---

## 6. Roles y permisos

El rol se obtiene del correo de quien envía el comando (tabla `usuario`).

| Rol | Puede |
|---|---|
| Director | Todo. Es el único que crea usuarios con rol, los modifica, les asigna rol (`ASIUSU`) o los elimina |
| Encargado | Todo lo operativo; de usuarios y roles solo puede listar |
| Cliente | Ver los servicios; ver, aceptar o rechazar **sus** cotizaciones; ver y pagar con QR **sus** cuotas; subir **su** material |
| Visitante (correo no registrado) | `HELP` y registrarse solo con `REGUSU` (queda como Cliente) |

**Eliminar** (`ELIUSU`, `ELICUE`, `ELISER`) borra de verdad y en cascada: un usuario se lleva sus cotizaciones (con cuotas, material, pautas y métricas); una cuenta, sus pautas; un servicio sale de las cotizaciones que lo tenían. Nadie puede eliminarse a sí mismo.

---

## 7. Logs (auditoría)

Se crean en la carpeta `logs/` al iniciar el programa:

| Archivo | Qué guarda | Ejemplo |
|---|---|---|
| `access.log` | Cada correo recibido y cada comando ejecutado con su resultado | `2026-11-19 12:47:13 \| ana@gmail.com \| INSUSU["1"] \| ERROR \| Cantidad de parámetros incorrecta...` |
| `error.log` | Cada error con su detalle y, si fue una excepción inesperada (base de datos, correo, PagoFácil, scraping), su traza completa | `WARNING: ana@gmail.com \| LISPER["*"] \| Comando desconocido...` |

---

## 8. Seguridad

- **Inyección SQL:** todas las consultas usan `PreparedStatement` con parámetros (`Dato.java`); nunca se arma SQL concatenando datos del correo.
- **Validación en el servidor** antes de ir a la base: tipos (entero, decimal, fecha, correo, URL), opciones permitidas y cantidad de parámetros.
- **Permisos por rol** en cada comando, y el cliente solo accede a sus propios datos.
- **Contraseñas** de usuarios guardadas con SHA-256. La contraseña del grupo y las credenciales de PagoFácil van en variables de entorno, no en el código (`Tokens*.txt` y `*.env` están en `.gitignore`).
- **Pagos QR:** un aviso de PagoFácil no se cree tal cual: se confirma consultando `/query-transaction`.

---

## 9. Reglas del negocio

- La cotización solo se modifica mientras está **pendiente**; al aprobarla se generan las cuotas (al contado, 1 cuota; en cuotas, una por mes).
- Pagos en efectivo o QR. Cuando se pagan todas las cuotas, la cotización pasa a **pagada**; las cuotas con fecha pasada se marcan **vencidas**.
- El material se acepta cuando la cotización tiene al menos una cuota pagada.
- La pauta necesita material **aprobado**, y en una misma cuenta los en vivo no se cruzan (post, historia y reel pueden coincidir).

## 10. Pago con QR (PagoFácil QR Master v2)

`/login` → `/list-enabled-services` → `/generate-qr` (el QR llega como imagen en el correo) → el cliente paga → el sistema lo confirma consultando `/query-transaction` cada 10 s (o por el callback). Estados: 1 En proceso, **2 Pagado**, 4 Anulado, 5 En revisión. `VERCUO["cuota_id"]` consulta el estado ("ya pagué").

## 11. Scraping (CU6 y CU7)

`HttpClient` de Java, sin librerías extra. **Métricas:** lee los contadores públicos de cada publicación. **Páginas externas:** busca la palabra clave en las 4 redes y descarta las cuentas oficiales.

## 12. Verificación

`test/Verificacion.java` prueba el intérprete (formato, comillas escapadas, cantidad de parámetros), el scraping, PagoFácil y los permisos sin base de datos ni correo. Debe imprimir `OK: todas las verificaciones pasaron`.
