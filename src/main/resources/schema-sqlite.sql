CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    telefono VARCHAR(15),
    contrasena VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL,
    version_token BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ajustes_empresariales (
    id BIGINT NOT NULL PRIMARY KEY,
    razon_social VARCHAR(150) NOT NULL,
    ruc VARCHAR(11) NOT NULL,
    direccion VARCHAR(300) NOT NULL,
    telefono1 VARCHAR(15),
    telefono2 VARCHAR(15),
    correo VARCHAR(200),
    registro_sanitario VARCHAR(500),
    slogan VARCHAR(200),
    ultima_actualizacion DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(300),
    -- SQLite JDBC binds BigDecimal as text. TEXT preserves exact decimal values for Java calculations.
    precio TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS pedidos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre_cliente VARCHAR(150) NOT NULL,
    telefono_cliente VARCHAR(15) NOT NULL,
    direccion_entrega VARCHAR(300) NOT NULL,
    observaciones VARCHAR(500),
    fecha_registro DATE NOT NULL,
    fecha_entrega DATE,
    origen VARCHAR(20) NOT NULL,
    usuario_registro_id BIGINT REFERENCES usuarios(id),
    estado_entrega VARCHAR(20) NOT NULL,
    estado_pago VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS detalles_pedido (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id),
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INTEGER NOT NULL,
    precio_unitario TEXT NOT NULL,
    subtotal TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS historial_modificaciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id),
    usuario VARCHAR(255) NOT NULL,
    usuario_id BIGINT REFERENCES usuarios(id),
    campo VARCHAR(30) NOT NULL,
    valor_anterior VARCHAR(20) NOT NULL,
    valor_nuevo VARCHAR(20) NOT NULL,
    fecha TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS notificaciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    titulo VARCHAR(200) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    leida BOOLEAN NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    CONSTRAINT uk_notificacion_pedido_usuario UNIQUE (pedido_id, usuario_id)
);

CREATE TABLE IF NOT EXISTS configuraciones_usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id),
    idioma VARCHAR(10) NOT NULL,
    nivel_zoom INTEGER NOT NULL,
    filtro_color VARCHAR(30) NOT NULL,
    filtro_color_activo BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS empleados (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id),
    dni VARCHAR(8) NOT NULL UNIQUE,
    fecha_ingreso DATE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_usuarios_nombre_id
    ON usuarios (nombre, id);
CREATE INDEX IF NOT EXISTS idx_pedidos_pago_fecha
    ON pedidos (estado_pago, fecha_registro DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_pedidos_completados_fecha
    ON pedidos (estado_entrega, estado_pago, fecha_entrega DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_pedidos_operativos_fecha
    ON pedidos (fecha_registro DESC, id);
CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario_leida_fecha
    ON notificaciones (usuario_id, leida, fecha_creacion DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_historial_pedido_fecha
    ON historial_modificaciones (pedido_id, fecha, id);
