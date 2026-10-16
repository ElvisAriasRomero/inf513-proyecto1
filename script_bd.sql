-- =====================================================
-- SISTEMA DE PUBLICIDAD UNIVERSITARIA - GRUPO 13
-- PostgreSQL - Base de datos: db_grupo13sc
-- 11 tablas del sistema + tabla comando (lista del help)
-- =====================================================

DROP TABLE IF EXISTS comando, metrica, pagina_detectada, pauta, material_publicitario, cuota,
    detalle_cotizacion, cotizacion, servicio, cuenta_red_social, usuario, rol;  -- sin CASCADE: nunca toca tablas de otros sistemas

-- Necesaria para que la base de datos impida horarios cruzados en pauta.
-- Si el servidor no la permite, borrar esta línea y el bloque EXCLUDE de pauta:
-- el sistema igual valida el choque de horarios antes de guardar.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ===================== CU1 =====================
CREATE TABLE rol (
  id     SERIAL PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE usuario (
  id             SERIAL PRIMARY KEY,
  rol_id         INT NOT NULL REFERENCES rol(id),
  nombres        VARCHAR(80) NOT NULL,
  apellidos      VARCHAR(80) NOT NULL,
  ci_nit         VARCHAR(20),
  email          VARCHAR(120) NOT NULL UNIQUE,
  telefono       VARCHAR(20),
  username       VARCHAR(40) NOT NULL UNIQUE,
  password       VARCHAR(255) NOT NULL,          -- SHA-256
  estado         BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===================== CU2 =====================
CREATE TABLE cuenta_red_social (
  id            SERIAL PRIMARY KEY,
  usuario_id    INT NOT NULL REFERENCES usuario(id),
  red_social    VARCHAR(20) NOT NULL
                CHECK (red_social IN ('Facebook','Instagram','TikTok','YouTube')),
  nombre_cuenta VARCHAR(100) NOT NULL,
  url           VARCHAR(255) NOT NULL UNIQUE,
  token_acceso  TEXT,
  estado        BOOLEAN NOT NULL DEFAULT TRUE
);

-- ===================== CU3 =====================
CREATE TABLE servicio (
  id          SERIAL PRIMARY KEY,
  nombre      VARCHAR(80) NOT NULL,
  descripcion TEXT,
  precio      NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
  estado      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE cotizacion (
  id         SERIAL PRIMARY KEY,
  cliente_id INT NOT NULL REFERENCES usuario(id),
  usuario_id INT NOT NULL REFERENCES usuario(id),
  fecha      DATE NOT NULL DEFAULT CURRENT_DATE,
  total      NUMERIC(10,2) NOT NULL DEFAULT 0,
  tipo_pago  VARCHAR(10) NOT NULL DEFAULT 'contado'
             CHECK (tipo_pago IN ('contado','cuotas')),
  nro_cuotas INT NOT NULL DEFAULT 1 CHECK (nro_cuotas >= 1),
  estado     VARCHAR(15) NOT NULL DEFAULT 'pendiente'
             CHECK (estado IN ('pendiente','aprobada','pagada','rechazada')),
  CHECK (tipo_pago = 'cuotas' OR nro_cuotas = 1)
);

CREATE TABLE detalle_cotizacion (
  id              SERIAL PRIMARY KEY,
  cotizacion_id   INT NOT NULL REFERENCES cotizacion(id) ON DELETE CASCADE,
  servicio_id     INT NOT NULL REFERENCES servicio(id),
  cantidad        INT NOT NULL CHECK (cantidad > 0),
  precio_unitario NUMERIC(10,2) NOT NULL,
  subtotal        NUMERIC(10,2) NOT NULL
);

CREATE TABLE cuota (
  id                SERIAL PRIMARY KEY,
  cotizacion_id     INT NOT NULL REFERENCES cotizacion(id) ON DELETE CASCADE,
  nro_cuota         INT NOT NULL,
  monto             NUMERIC(10,2) NOT NULL CHECK (monto > 0),
  fecha_vencimiento DATE NOT NULL,
  fecha_pago        DATE,
  metodo            VARCHAR(10) CHECK (metodo IN ('efectivo','QR')),
  nro_transaccion   VARCHAR(50),
  comprobante       VARCHAR(255),
  estado            VARCHAR(10) NOT NULL DEFAULT 'pendiente'
                    CHECK (estado IN ('pendiente','pagada','vencida')),
  verificado_por    INT REFERENCES usuario(id),
  UNIQUE (cotizacion_id, nro_cuota),
  CHECK (estado <> 'pagada' OR (metodo IS NOT NULL AND fecha_pago IS NOT NULL)),
  CHECK (metodo <> 'QR' OR estado <> 'pagada' OR comprobante IS NOT NULL)
);

-- ===================== CU4 =====================
CREATE TABLE material_publicitario (
  id            SERIAL PRIMARY KEY,
  cotizacion_id INT NOT NULL REFERENCES cotizacion(id),
  titulo        VARCHAR(120) NOT NULL,
  archivo       VARCHAR(500) NOT NULL,           -- enlace (Drive, Dropbox, etc.)
  tipo          VARCHAR(10) NOT NULL CHECK (tipo IN ('imagen','video')),
  estado        VARCHAR(10) NOT NULL DEFAULT 'pendiente'
                CHECK (estado IN ('pendiente','aprobado','rechazado')),
  validado_por  INT REFERENCES usuario(id),
  observacion   TEXT,
  fecha_carga   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===================== CU5 =====================
CREATE TABLE pauta (
  id                   SERIAL PRIMARY KEY,
  material_id          INT NOT NULL REFERENCES material_publicitario(id),
  cuenta_red_social_id INT NOT NULL REFERENCES cuenta_red_social(id),
  tipo                 VARCHAR(10) NOT NULL
                       CHECK (tipo IN ('post','historia','reel','en_vivo')),
  fecha_hora_inicio    TIMESTAMPTZ NOT NULL,
  fecha_hora_fin       TIMESTAMPTZ NOT NULL,     -- post/historia/reel: inicio + 1 minuto
  url_publicacion      VARCHAR(500),             -- el scraping entra a este enlace
  estado               VARCHAR(12) NOT NULL DEFAULT 'programada'
                       CHECK (estado IN ('programada','publicada','cancelada')),
  CHECK (fecha_hora_fin > fecha_hora_inicio),
  CHECK (estado <> 'publicada' OR url_publicacion IS NOT NULL),
  -- en una misma cuenta no se cruzan los horarios
  EXCLUDE USING gist (
    cuenta_red_social_id WITH =,
    tstzrange(fecha_hora_inicio, fecha_hora_fin, '[)') WITH &&
  ) WHERE (estado <> 'cancelada')
);

-- ===================== CU6 =====================
CREATE TABLE pagina_detectada (
  id              SERIAL PRIMARY KEY,
  red_social      VARCHAR(20) NOT NULL,
  nombre_pagina   VARCHAR(255) NOT NULL,
  url             VARCHAR(500) NOT NULL UNIQUE,
  palabra_clave   VARCHAR(80) NOT NULL,
  seguidores      INT NOT NULL DEFAULT 0,
  estado          VARCHAR(12) NOT NULL DEFAULT 'detectada'
                  CHECK (estado IN ('detectada','denunciada','descartada')),
  revisado_por    INT REFERENCES usuario(id),
  fecha_deteccion TIMESTAMP NOT NULL DEFAULT NOW(),
  CHECK (estado = 'detectada' OR revisado_por IS NOT NULL)
);

-- ===================== CU7 =====================
CREATE TABLE metrica (
  id            SERIAL PRIMARY KEY,
  pauta_id      INT NOT NULL REFERENCES pauta(id) ON DELETE CASCADE,
  fecha_captura TIMESTAMP NOT NULL DEFAULT NOW(),
  vistas        INT NOT NULL DEFAULT 0,
  reacciones    INT NOT NULL DEFAULT 0,
  comentarios   INT NOT NULL DEFAULT 0,
  compartidos   INT NOT NULL DEFAULT 0
);
CREATE INDEX metrica_ultima ON metrica (pauta_id, fecha_captura DESC);

-- ===================== CU8 =====================
-- Sin tabla propia: los reportes consultan las tablas anteriores.

-- ===================== HELP =====================
CREATE TABLE comando (
  id        SERIAL PRIMARY KEY,
  caso_uso  VARCHAR(60) NOT NULL,
  accion    VARCHAR(60) NOT NULL,
  parametro VARCHAR(255) NOT NULL,
  ejemplo   VARCHAR(500) NOT NULL,
  roles     VARCHAR(60) NOT NULL
);

-- =====================================================
-- DATOS INICIALES
-- =====================================================
INSERT INTO rol (nombre) VALUES ('Director'), ('Encargado'), ('Cliente');

-- CAMBIAR el email por el correo real del Director: es el que podrá enviar todos los comandos.
-- password = SHA-256 de 'director123'
INSERT INTO usuario (rol_id, nombres, apellidos, ci_nit, email, telefono, username, password)
VALUES (1, 'Director', 'General', '0000000', '123evil71@gmail.com', '70000000', 'director',
        '9e4d7bba246abe731743986c4dc50897b68b1d0249a066abb3530fcbaa33dab3');

INSERT INTO servicio (nombre, descripcion, precio) VALUES
('Post', 'Publicación de imagen en una red', 100),
('Historia', 'Historia de 24 horas', 60),
('Reel', 'Video corto', 200),
('En vivo', 'Transmisión en vivo de hasta 1 hora', 500),
('Paquete', 'Post + Reel en las 4 redes', 1000);

INSERT INTO comando (caso_uso, accion, parametro, ejemplo, roles) VALUES
('Ayuda', 'HELP', '-', 'HELP', 'Todos'),
('CU1 Usuarios', 'INSROL', 'nombre', 'INSROL["Auditor"]', 'Director'),
('CU1 Usuarios', 'MODROL', 'id, nombre', 'MODROL["4","Supervisor"]', 'Director'),
('CU1 Usuarios', 'ELIROL', 'id', 'ELIROL["4"]', 'Director'),
('CU1 Usuarios', 'LISROL', '*', 'LISROL["*"]', 'Director, Encargado'),
('CU1 Usuarios', 'INSUSU', 'rol_id, nombres, apellidos, ci_nit, email, telefono, username, password', 'INSUSU["3","Juan","Pérez","7894561","juan@gmail.com","76543210","juanp","clave123"]', 'Director, Encargado'),
('CU1 Usuarios', 'MODUSU', 'id, rol_id, nombres, apellidos, ci_nit, email, telefono, username', 'MODUSU["2","3","Juan","Pérez","7894561","juan@gmail.com","76543210","juanp"]', 'Director, Encargado'),
('CU1 Usuarios', 'ELIUSU', 'id (lo desactiva)', 'ELIUSU["2"]', 'Director'),
('CU1 Usuarios', 'LISUSU', '*', 'LISUSU["*"]', 'Director, Encargado'),
('CU2 Cuentas', 'INSCUE', 'red (Facebook, Instagram, TikTok, YouTube), nombre, url, token (o -)', 'INSCUE["Facebook","UAGRM Oficial","https://facebook.com/uagrm","-"]', 'Director, Encargado'),
('CU2 Cuentas', 'MODCUE', 'id, red, nombre, url, token', 'MODCUE["1","Facebook","UAGRM","https://facebook.com/uagrm","-"]', 'Director, Encargado'),
('CU2 Cuentas', 'ELICUE', 'id (la desactiva)', 'ELICUE["1"]', 'Director, Encargado'),
('CU2 Cuentas', 'LISCUE', '*', 'LISCUE["*"]', 'Director, Encargado'),
('CU3 Oferta', 'INSSER', 'nombre, descripcion, precio', 'INSSER["Reel","Video corto","200"]', 'Director, Encargado'),
('CU3 Oferta', 'MODSER', 'id, nombre, descripcion, precio', 'MODSER["3","Reel","Video de 30 s","250"]', 'Director, Encargado'),
('CU3 Oferta', 'ELISER', 'id (lo desactiva)', 'ELISER["3"]', 'Director, Encargado'),
('CU3 Oferta', 'LISSER', '*', 'LISSER["*"]', 'Todos los registrados'),
('CU3 Cotización', 'INSCOT', 'cliente_id, contado | cuotas, nro_cuotas', 'INSCOT["2","cuotas","3"]', 'Director, Encargado'),
('CU3 Cotización', 'MODCOT', 'id, contado | cuotas, nro_cuotas', 'MODCOT["1","contado"]', 'Director, Encargado'),
('CU3 Cotización', 'ELICOT', 'id (solo pendiente)', 'ELICOT["1"]', 'Director, Encargado'),
('CU3 Cotización', 'LISCOT', '*', 'LISCOT["*"]', 'Todos (el cliente ve las suyas)'),
('CU3 Cotización', 'VERCOT', 'id (servicios y cuotas)', 'VERCOT["1"]', 'Todos (el cliente ve las suyas)'),
('CU3 Cotización', 'APRCOT', 'id (genera las cuotas)', 'APRCOT["1"]', 'Todos (el cliente las suyas)'),
('CU3 Cotización', 'RECCOT', 'id', 'RECCOT["1"]', 'Todos (el cliente las suyas)'),
('CU3 Cotización', 'INSDET', 'cotizacion_id, servicio_id, cantidad', 'INSDET["1","3","2"]', 'Director, Encargado'),
('CU3 Cotización', 'ELIDET', 'id del detalle', 'ELIDET["4"]', 'Director, Encargado'),
('CU3 Cotización', 'LISDET', 'cotizacion_id', 'LISDET["1"]', 'Todos (el cliente las suyas)'),
('CU3 Pagos', 'LISCUO', 'cotizacion_id', 'LISCUO["1"]', 'Todos (el cliente las suyas)'),
('CU3 Pagos', 'PAGCUO', 'id, efectivo  |  id, QR, nro_transaccion, enlace_comprobante', 'PAGCUO["5","QR","884512","https://drive.google.com/comprobante"]', 'Director, Encargado'),
('CU3 Pagos', 'QRPCUO', 'id de la cuota (llega el QR de PagoFácil por correo; al pagar se registra solo)', 'QRPCUO["5"]', 'Todos (el cliente las suyas)'),
('CU3 Pagos', 'VENCUO', '* (todas las cuotas por cobrar)', 'VENCUO["*"]', 'Director, Encargado'),
('CU4 Material', 'INSMAT', 'cotizacion_id, titulo, enlace_archivo, imagen | video', 'INSMAT["1","Banner Feria","https://drive.google.com/banner","imagen"]', 'Todos (el cliente las suyas)'),
('CU4 Material', 'LISMAT', '* o cotizacion_id', 'LISMAT["1"]', 'Todos (el cliente los suyos)'),
('CU4 Material', 'APRMAT', 'id, observacion (opcional)', 'APRMAT["1"]', 'Director, Encargado'),
('CU4 Material', 'RECMAT', 'id, observacion', 'RECMAT["1","El logo está pixelado"]', 'Director, Encargado'),
('CU5 Pauta', 'INSPAU', 'material_id, cuenta_id, post | historia | reel, AAAA-MM-DD HH:MM', 'INSPAU["1","2","reel","2026-10-15 18:00"]', 'Director, Encargado'),
('CU5 Pauta', 'INSPAU (en vivo)', 'material_id, cuenta_id, en_vivo, inicio, fin', 'INSPAU["1","1","en_vivo","2026-10-16 20:00","2026-10-16 21:00"]', 'Director, Encargado'),
('CU5 Pauta', 'PUBPAU', 'id, enlace_de_la_publicacion', 'PUBPAU["1","https://www.tiktok.com/@uagrm/video/123"]', 'Director, Encargado'),
('CU5 Pauta', 'CANPAU', 'id (libera el horario)', 'CANPAU["1"]', 'Director, Encargado'),
('CU5 Pauta', 'LISPAU', '*', 'LISPAU["*"]', 'Director, Encargado'),
('CU6 Páginas externas', 'ESCPAG', 'palabra_clave (* = UAGRM)', 'ESCPAG["UAGRM"]', 'Director, Encargado'),
('CU6 Páginas externas', 'LISPAG', '* (primero las pendientes con más seguidores)', 'LISPAG["*"]', 'Director, Encargado'),
('CU6 Páginas externas', 'DENPAG', 'id', 'DENPAG["3"]', 'Director, Encargado'),
('CU6 Páginas externas', 'DESPAG', 'id', 'DESPAG["4"]', 'Director, Encargado'),
('CU6 Páginas externas', 'INSPAG', 'red, nombre, url, palabra_clave, seguidores', 'INSPAG["Instagram","uagrm.becas.gratis","https://instagram.com/uagrm.becas.gratis","UAGRM","850"]', 'Director, Encargado'),
('CU7 Métricas', 'CAPMET', '* (scraping inmediato de las pautas publicadas)', 'CAPMET["*"]', 'Director, Encargado'),
('CU7 Métricas', 'LISMET', 'pauta_id (evolución)', 'LISMET["1"]', 'Director, Encargado'),
('CU7 Métricas', 'INSMET', 'pauta_id, vistas, reacciones, comentarios, compartidos', 'INSMET["1","2300","160","21","14"]', 'Director, Encargado'),
('CU8 Reportes', 'REPRED', '* (red más efectiva + gráficas)', 'REPRED["*"]', 'Director, Encargado'),
('CU8 Reportes', 'REPGRA', '* (solo gráficas por red)', 'REPGRA["*"]', 'Director, Encargado'),
('CU8 Reportes', 'REPTIP', '* (post, reel, en vivo por red)', 'REPTIP["*"]', 'Director, Encargado'),
('CU8 Reportes', 'REPMEN', '* (movimiento por mes)', 'REPMEN["*"]', 'Director, Encargado'),
('CU8 Reportes', 'REPPAG', '* (páginas externas por red)', 'REPPAG["*"]', 'Director, Encargado'),
('CU8 Reportes', 'REPCOB', '* (cuotas por cobrar)', 'REPCOB["*"]', 'Director, Encargado'),
('CU8 Reportes', 'REPCOT', '* (por estado + gráfica)', 'REPCOT["*"]', 'Director, Encargado');
