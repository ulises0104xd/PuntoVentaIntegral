@echo off
rem Inicia el Punto de Venta Integral con doble clic.
rem Requiere Java 17 o superior. Si el comando "java" de su equipo es una version anterior,
rem defina la variable PV_JAVA con la ruta completa a javaw.exe de un JDK 17+, por ejemplo:
rem   setx PV_JAVA "C:\Program Files\Java\jdk-23\bin\javaw.exe"
setlocal

set "JAR=%~dp0..\target\punto-venta-0.1.0-prototipo.jar"
if not exist "%JAR%" set "JAR=%~dp0punto-venta-0.1.0-prototipo.jar"
if not exist "%JAR%" (
    echo No se encontro el archivo punto-venta-0.1.0-prototipo.jar.
    echo Genere el paquete con:  mvn package -DskipTests
    pause
    exit /b 1
)

set "JAVAW=javaw"
if defined JAVA_HOME set "JAVAW=%JAVA_HOME%\bin\javaw.exe"
if defined PV_JAVA set "JAVAW=%PV_JAVA%"

start "" "%JAVAW%" -jar "%JAR%"
endlocal
