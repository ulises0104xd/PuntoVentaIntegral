# Punto de Venta Integral – Versión beta (fase de Construcción, RUP)

Sistema de punto de venta para una pequeña empresa comercial, con arquitectura por capas
(ui → service → dao → MySQL). Incluye los 13 casos de uso del alcance:

| Caso de uso | Qué hace |
|---|---|
| CU01 Iniciar sesión | Login con hash PBKDF2-HMAC-SHA256 + sal, roles Administrador/Cajero, bitácora |
| CU02 Consultar productos | Búsqueda por código, nombre o categoría (todos los roles) |
| CU03 + CU05 Registrar venta y pago | Carrito, subtotal/total, pago en efectivo o tarjeta, cobro en **una sola transacción** que descuenta inventario |
| CU04 Descuento autorizado | Cajero hasta 10 %; más requiere credenciales de un administrador |
| CU06 Ticket | Ticket en texto plano de 40 columnas |
| CU07 Gestionar usuarios | Alta, activar/desactivar y cambio de contraseña (solo administrador) |
| CU08 Gestionar productos | Alta, edición y baja lógica (solo administrador) |
| CU09 / CU10 Entradas y ajustes | Entradas de mercancía y ajustes con motivo; cada cambio deja un movimiento |
| CU11 Historial y existencias bajas | Historial de movimientos y productos en o bajo su mínimo |
| CU12 / CU13 Reportes | Ventas por día/semana/mes, más vendidos, ingresos, existencias, historial por usuario; exporta CSV |

Las pestañas visibles dependen del rol (Cajero: Ventas y Productos; Administrador: todas), y los servicios
vuelven a exigir el rol, por lo que el control no depende solo de la interfaz.

## Interfaz

Aspecto moderno con [FlatLaf](https://www.formdev.com/flatlaf/) y un sistema de diseño propio (`ui/Tema.java`).
Pensada para el uso diario en caja:

- **Ventas:** se escanea o escribe el código/nombre y Enter; botones `−` `+` por renglón, total grande, cobro con
  cambio calculado en vivo y botones de billetes (`Exacto`, `$200`…), ticket con **Imprimir** y **Guardar como…**.
- **Teclado:** `F9` cobrar · `F2` cantidad · `Supr` quitar · `F3` buscar · `F4` descuento · `Ctrl+1…5` pestañas.
- **Formularios:** los errores se muestran en el mismo cuadro (no se cierra ni se pierde lo escrito); `Enter` acepta y `Esc` cancela.
- **Productos:** búsqueda mientras se escribe, existencias bajas resaltadas, doble clic para editar.
- **Reportes:** selectores de fecha, periodos rápidos (hoy, semana, mes, 30 días) y exportación a CSV.
- Avisos breves en la barra de estado y confirmación antes de desactivar productos o usuarios.

## Tecnologías

Java 17 · Swing · MySQL 8 · JDBC (Connector/J 8.4) · Maven · JUnit 5 · Git

## Requisitos

- **JDK 17 o superior** (`java -version` debe mostrar 17+; con Java 8 no compila ni ejecuta).
- MySQL 8 en ejecución (probado también con MariaDB 10.4 de XAMPP).
- Maven 3.9+.

## Puesta en marcha

1. Crear la base de datos y cargar los datos de ejemplo:

   ```bash
   mysql -u root -p < db/schema.sql
   mysql -u root -p < db/datos_ejemplo.sql
   ```

2. Revisar la conexión en `src/main/resources/db.properties` (o usar las variables de entorno
   `DB_URL`, `DB_USER` y `DB_PASSWORD`, que tienen prioridad).

3. Compilar y empaquetar. **Ojo:** `mvn package` también ejecuta las pruebas de integración, que crean
   datos de prueba en la base configurada; para empaquetar sin ellas usa `-DskipTests`:

   ```bash
   mvn package -DskipTests
   java -jar target/punto-venta-0.1.0-prototipo.jar   # usar JDK 17+ (por ejemplo la ruta completa a java.exe del JDK)
   ```

## Respaldo, restauración e inicio rápido (carpeta `scripts/`)

| Script | Para qué sirve |
|---|---|
| `scripts\iniciar.bat` | Inicia la aplicación con doble clic. Si `java` es anterior a 17, defina `PV_JAVA` con la ruta a `javaw.exe` de un JDK 17+. |
| `scripts\respaldo.ps1` | Crea un respaldo `.sql` con fecha y hora en `respaldos\` (se puede ejecutar con la aplicación abierta). |
| `scripts\restaurar.ps1 -Archivo <ruta>` | Restaura un respaldo; **reemplaza** los datos actuales y pide confirmación. Cierre la aplicación antes. |

```powershell
.\scripts\respaldo.ps1 -MysqlBin "C:\xampp\mysql\bin"
.\scripts\restaurar.ps1 -Archivo .\respaldos\punto_venta_AAAAMMDD_HHMMSS.sql -MysqlBin "C:\xampp\mysql\bin"
```

Si `root` tiene contraseña, agregue `-Clave "su_contraseña"` (o defina `DB_PASSWORD`). Si PowerShell bloquea los
scripts, ejecute antes `Set-ExecutionPolicy -Scope Process Bypass`.

## Usuarios de prueba (la aplicación los crea al arrancar si no existen)

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `cajero` | `cajero123` | Cajero |

Son credenciales de ejemplo para el prototipo; deben cambiarse antes de un uso real.

## Pruebas

```bash
mvn test
```

- Las pruebas unitarias (hash, carrito, reglas de descuento y pago) no necesitan base de datos.
- `InterfazAceptacionTest` (AC-01 a AC-15) e `InterfazUsabilidadTest` manejan las pantallas Swing reales con clics
  y texto; **necesitan pantalla** (en servidores sin pantalla se omiten solas o se ejecutan con `xvfb-run`).
- `VentaIntegracionTest` valida los criterios de aceptación del prototipo contra MySQL y se omite
  sola si no hay conexión. **Úsala con una base de pruebas**, porque crea productos, usuarios y ventas:

  ```bash
  mysql -u root -p -e "CREATE DATABASE punto_venta_test CHARACTER SET utf8mb4"
  # aplicar db/schema.sql sin las líneas CREATE DATABASE / USE sobre punto_venta_test, y luego:
  DB_URL="jdbc:mysql://localhost:3306/punto_venta_test?useSSL=false&allowPublicKeyRetrieval=true" mvn test
  ```

| Criterio | Prueba |
|---|---|
| CA1 Login y contraseñas sin texto plano | `ca1_login...` y `PasswordHasherTest` |
| CA2 Venta completa y atómica | `ca2_ventaGuarda...` |
| CA3 Sin datos parciales si algo falla | `ca3_siFalla...` |
| CA4 Descuento > 10 % requiere administrador | `ca4_descuento...` y `VentaReglasTest` |
| CA5 Concurrencia sobre la última existencia | `ca5_dosVentasSimultaneas...` |
| CA7 Totales exactos a dos decimales | `CarritoTest` |

## Estructura

```
db/                     esquema y datos de ejemplo
src/main/java/com/puntoventa/
  ui/       Swing (LoginFrame, MainFrame, VentaPanel)
  service/  reglas de negocio y transacciones (AuthService, VentaService, TicketService, ProductoService)
  dao/      SQL con JDBC (UsuarioDao, ProductoDao, VentaDao, InventarioDao, BitacoraDao)
  model/    objetos del dominio (records) y Carrito
  util/     Database, PasswordHasher, Formato, excepciones
```
