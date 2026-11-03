-- Punto de Venta Integral - esquema de base de datos (MySQL 8)
-- Ejecutar una sola vez:  mysql -u root -p < db/schema.sql

CREATE DATABASE IF NOT EXISTS punto_venta
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE punto_venta;

CREATE TABLE IF NOT EXISTS usuarios (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    nombre        VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol           ENUM('ADMIN', 'CAJERO') NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuarios_username UNIQUE (username)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS productos (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(30)   NOT NULL,
    nombre        VARCHAR(120)  NOT NULL,
    categoria     VARCHAR(60)   NOT NULL DEFAULT 'General',
    precio        DECIMAL(10,2) NOT NULL,
    existencias   INT           NOT NULL DEFAULT 0,
    stock_minimo  INT           NOT NULL DEFAULT 5,
    activo        BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_productos_codigo UNIQUE (codigo),
    CONSTRAINT ck_productos_precio CHECK (precio >= 0),
    CONSTRAINT ck_productos_existencias CHECK (existencias >= 0),
    INDEX idx_productos_nombre (nombre),
    INDEX idx_productos_categoria (categoria)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS ventas (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    fecha           DATETIME      NOT NULL,
    usuario_id      INT           NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    descuento_pct   DECIMAL(5,2)  NOT NULL DEFAULT 0,
    descuento_monto DECIMAL(10,2) NOT NULL DEFAULT 0,
    total           DECIMAL(10,2) NOT NULL,
    metodo_pago     ENUM('EFECTIVO', 'TARJETA') NOT NULL,
    monto_recibido  DECIMAL(10,2) NOT NULL,
    cambio          DECIMAL(10,2) NOT NULL DEFAULT 0,
    autorizado_por  INT NULL,
    CONSTRAINT fk_ventas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_ventas_autorizo FOREIGN KEY (autorizado_por) REFERENCES usuarios (id),
    CONSTRAINT ck_ventas_total CHECK (total >= 0),
    INDEX idx_ventas_fecha (fecha)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS detalle_venta (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    venta_id        INT           NOT NULL,
    producto_id     INT           NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad > 0),
    INDEX idx_detalle_venta (venta_id),
    INDEX idx_detalle_producto (producto_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS movimientos_inventario (
    id                      INT AUTO_INCREMENT PRIMARY KEY,
    fecha                   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    producto_id             INT      NOT NULL,
    tipo                    ENUM('ENTRADA', 'VENTA', 'AJUSTE') NOT NULL,
    cantidad                INT      NOT NULL,
    existencias_resultantes INT      NOT NULL,
    motivo                  VARCHAR(200) NOT NULL,
    usuario_id              INT      NOT NULL,
    venta_id                INT NULL,
    CONSTRAINT fk_mov_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_mov_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    INDEX idx_mov_producto (producto_id, fecha)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS bitacora (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    fecha      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id INT          NOT NULL,
    accion     VARCHAR(40)  NOT NULL,
    detalle    VARCHAR(255) NOT NULL DEFAULT '',
    CONSTRAINT fk_bitacora_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    INDEX idx_bitacora_fecha (fecha),
    INDEX idx_bitacora_usuario (usuario_id)
) ENGINE = InnoDB;
