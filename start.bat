@echo off

REM Указываем путь к Maven-кешу
set M2R=C:\Users\sklep\.m2\repository\org\openjfx

REM Собираем module-path вручную
set MP=%M2R%\javafx-base\21\javafx-base-21.jar
set MP=%MP%;%M2R%\javafx-base\win\21\javafx-base-21-win.jar
set MP=%MP%;%M2R%\javafx-controls\21\javafx-controls-21.jar
set MP=%MP%;%M2R%\javafx-controls\win\21\javafx-controls-21-win.jar
set MP=%MP%;%M2R%\javafx-fxml\21\javafx-fxml-21.jar
set MP=%MP%;%M2R%\javafx-fxml\win\21\javafx-fxml-21-win.jar
set MP=%MP%;%M2R%\javafx-graphics\21\javafx-graphics-21.jar
set MP=%MP%;%M2R%\javafx-graphics\win\21\javafx-graphics-21-win.jar

echo Проверка наличия JAR...
if not exist "target\Mini_Telegram-1.0-SNAPSHOT.jar" (
    echo ❌ JAR не найден! Сначала собери проект через Maven (clean package).
    pause
    exit /b
)

echo ✅ JAR найден. Запускаем клиентов...

REM Ключевой момент — пустая строка после start "" ИНАЧЕ CMD ёбнется
start "" cmd /k java --module-path "%MP%" --add-modules javafx.controls,javafx.fxml -jar target\Mini_Telegram-1.0-SNAPSHOT.jar

start "" cmd /k java --module-path "%MP%" --add-modules javafx.controls,javafx.fxml -jar target\Mini_Telegram-1.0-SNAPSHOT.jar

echo 🚀 Готово!
pause
