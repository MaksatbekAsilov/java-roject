import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class Openhtml {
    public static void main(String[] args) {
        // Путь к вашему HTML-файлу
        String filePath = "C:\\Users\\Asimo\\Downloads\\Java.html"; // Замените на путь к вашему HTML-файлу

        // Создаем объект File
        File htmlFile = new File(filePath);

        // Проверяем, доступен ли класс Desktop
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();

            try {
                // Открываем HTML-файл в браузере
                desktop.browse(htmlFile.toURI());
                System.out.println("HTML файл открыт в браузере.");
            } catch (IOException e) {
                System.out.println("Ошибка при открытии HTML-файла: " + e.getMessage());
            }
        } else {
            System.out.println("Функция Desktop не поддерживается на текущей платформе.");
        }
    }
}
