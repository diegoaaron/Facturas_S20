-- Script físico de la base local de Facturas S20 (SQLite; cifrada con SQLCipher en el teléfono).
-- Generado desde el esquema v1 que exporta Room (android/app/schemas/.../1.json).

CREATE TABLE IF NOT EXISTS `contribuyente` (`id_contribuyente` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ruc` TEXT NOT NULL, `nombre` TEXT NOT NULL, `titular` TEXT NOT NULL, `ultimo_digito` INTEGER NOT NULL, `pin_hash` TEXT, `pin_sal` TEXT, `fecha_alta` TEXT NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS `index_contribuyente_ruc` ON `contribuyente` (`ruc`);

CREATE TABLE IF NOT EXISTS `periodo` (`id_periodo` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_contribuyente` INTEGER NOT NULL, `anio` INTEGER NOT NULL, `mes` INTEGER NOT NULL, `total_ventas` INTEGER, `estado` TEXT NOT NULL, FOREIGN KEY(`id_contribuyente`) REFERENCES `contribuyente`(`id_contribuyente`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE UNIQUE INDEX IF NOT EXISTS `index_periodo_id_contribuyente_anio_mes` ON `periodo` (`id_contribuyente`, `anio`, `mes`);

CREATE TABLE IF NOT EXISTS `emisor` (`id_emisor` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ruc` TEXT NOT NULL, `razon_social` TEXT NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS `index_emisor_ruc` ON `emisor` (`ruc`);

CREATE TABLE IF NOT EXISTS `factura_compra` (`id_factura` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_periodo` INTEGER NOT NULL, `id_emisor` INTEGER NOT NULL, `serie` TEXT NOT NULL, `numero` TEXT NOT NULL, `numero_normalizado` TEXT NOT NULL, `fecha_emision` TEXT NOT NULL, `moneda` TEXT NOT NULL, `importe_total` INTEGER NOT NULL, `origen` TEXT NOT NULL, `estado` TEXT NOT NULL, `motivo_anulacion` TEXT, `fecha_registro` TEXT NOT NULL, FOREIGN KEY(`id_periodo`) REFERENCES `periodo`(`id_periodo`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`id_emisor`) REFERENCES `emisor`(`id_emisor`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE INDEX IF NOT EXISTS `index_factura_compra_id_periodo_estado` ON `factura_compra` (`id_periodo`, `estado`);
CREATE INDEX IF NOT EXISTS `index_factura_compra_id_emisor_serie_numero_normalizado` ON `factura_compra` (`id_emisor`, `serie`, `numero_normalizado`);

CREATE TABLE IF NOT EXISTS `imagen_factura` (`id_imagen` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_factura` INTEGER NOT NULL, `ruta_cifrada` TEXT NOT NULL, `nitidez` REAL NOT NULL, `fecha_captura` TEXT NOT NULL, FOREIGN KEY(`id_factura`) REFERENCES `factura_compra`(`id_factura`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE UNIQUE INDEX IF NOT EXISTS `index_imagen_factura_id_factura` ON `imagen_factura` (`id_factura`);

CREATE TABLE IF NOT EXISTS `campo_extraido` (`id_campo` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_factura` INTEGER NOT NULL, `id_modelo` INTEGER, `nombre_campo` TEXT NOT NULL, `valor_leido` TEXT, `valor_final` TEXT, `confianza` REAL NOT NULL, `corregido` INTEGER NOT NULL, FOREIGN KEY(`id_factura`) REFERENCES `factura_compra`(`id_factura`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`id_modelo`) REFERENCES `modelo_local`(`id_modelo`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE INDEX IF NOT EXISTS `index_campo_extraido_id_factura` ON `campo_extraido` (`id_factura`);
CREATE INDEX IF NOT EXISTS `index_campo_extraido_id_modelo` ON `campo_extraido` (`id_modelo`);

CREATE TABLE IF NOT EXISTS `modelo_local` (`id_modelo` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `version` TEXT NOT NULL, `tamano_mb` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `estado` TEXT NOT NULL, `ruta` TEXT, `fecha_instalacion` TEXT);
CREATE UNIQUE INDEX IF NOT EXISTS `index_modelo_local_version` ON `modelo_local` (`version`);

CREATE TABLE IF NOT EXISTS `parametro_version` (`id_parametro` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `version` TEXT NOT NULL, `umbral_aviso` INTEGER NOT NULL, `tope_anual` INTEGER NOT NULL, `vigente_desde` TEXT NOT NULL, `activo` INTEGER NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS `index_parametro_version_version` ON `parametro_version` (`version`);

CREATE TABLE IF NOT EXISTS `categoria_nrus` (`id_categoria` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_parametro` INTEGER NOT NULL, `codigo` INTEGER NOT NULL, `limite_mensual` INTEGER NOT NULL, `cuota` INTEGER NOT NULL, FOREIGN KEY(`id_parametro`) REFERENCES `parametro_version`(`id_parametro`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE UNIQUE INDEX IF NOT EXISTS `index_categoria_nrus_id_parametro_codigo` ON `categoria_nrus` (`id_parametro`, `codigo`);

CREATE TABLE IF NOT EXISTS `cronograma_venc` (`id_cronograma` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_parametro` INTEGER NOT NULL, `anio` INTEGER NOT NULL, `mes` INTEGER NOT NULL, `ultimo_digito` INTEGER NOT NULL, `fecha_limite` TEXT NOT NULL, FOREIGN KEY(`id_parametro`) REFERENCES `parametro_version`(`id_parametro`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE UNIQUE INDEX IF NOT EXISTS `index_cronograma_venc_id_parametro_anio_mes_ultimo_digito` ON `cronograma_venc` (`id_parametro`, `anio`, `mes`, `ultimo_digito`);

CREATE TABLE IF NOT EXISTS `determinacion` (`id_determinacion` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_periodo` INTEGER NOT NULL, `id_categoria` INTEGER, `total_adquisiciones` INTEGER NOT NULL, `monto_determinante` INTEGER NOT NULL, `cuota` INTEGER, `fecha_vencimiento` TEXT NOT NULL, `nivel_alerta` TEXT NOT NULL, `aviso_tope_anual` INTEGER NOT NULL, `fecha_calculo` TEXT NOT NULL, FOREIGN KEY(`id_periodo`) REFERENCES `periodo`(`id_periodo`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`id_categoria`) REFERENCES `categoria_nrus`(`id_categoria`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE UNIQUE INDEX IF NOT EXISTS `index_determinacion_id_periodo` ON `determinacion` (`id_periodo`);
CREATE INDEX IF NOT EXISTS `index_determinacion_id_categoria` ON `determinacion` (`id_categoria`);

CREATE TABLE IF NOT EXISTS `aviso` (`id_aviso` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_periodo` INTEGER NOT NULL, `tipo` TEXT NOT NULL, `fecha_programada` TEXT NOT NULL, `enviado` INTEGER NOT NULL, FOREIGN KEY(`id_periodo`) REFERENCES `periodo`(`id_periodo`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE INDEX IF NOT EXISTS `index_aviso_id_periodo` ON `aviso` (`id_periodo`);

CREATE TABLE IF NOT EXISTS `reporte_mensual` (`id_reporte` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id_periodo` INTEGER NOT NULL, `ruta_pdf` TEXT NOT NULL, `sha256` TEXT NOT NULL, `fecha_generacion` TEXT NOT NULL, FOREIGN KEY(`id_periodo`) REFERENCES `periodo`(`id_periodo`) ON UPDATE NO ACTION ON DELETE NO ACTION );
CREATE INDEX IF NOT EXISTS `index_reporte_mensual_id_periodo` ON `reporte_mensual` (`id_periodo`);
