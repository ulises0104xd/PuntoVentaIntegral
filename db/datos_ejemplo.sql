-- Datos de ejemplo para el prototipo (productos). Los usuarios iniciales los crea la aplicación
-- al arrancar (ver AuthService.sembrarUsuariosIniciales) porque necesitan hash de contraseña.
USE punto_venta;

INSERT IGNORE INTO productos (codigo, nombre, categoria, precio, existencias, stock_minimo) VALUES
('7501001', 'Refresco cola 600 ml',      'Bebidas',   18.50, 48, 12),
('7501002', 'Agua natural 1 L',          'Bebidas',   12.00, 60, 15),
('7501003', 'Jugo de naranja 1 L',       'Bebidas',   32.00, 24, 6),
('7502001', 'Galletas surtidas 200 g',   'Abarrotes', 24.90, 30, 8),
('7502002', 'Arroz 1 kg',                'Abarrotes', 27.50, 40, 10),
('7502003', 'Frijol negro 1 kg',         'Abarrotes', 39.00, 35, 10),
('7502004', 'Aceite vegetal 900 ml',     'Abarrotes', 46.00, 20, 5),
('7503001', 'Leche entera 1 L',          'Lácteos',   26.00, 50, 12),
('7503002', 'Queso panela 400 g',        'Lácteos',   58.00, 12, 4),
('7504001', 'Jabón de barra',            'Limpieza',  14.50, 45, 10),
('7504002', 'Detergente en polvo 1 kg',  'Limpieza',  38.00, 18, 5),
('7505001', 'Cuaderno profesional',      'Papelería', 35.00, 25, 6);
