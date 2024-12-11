import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class WeatherApp {
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String API_KEY = "53f7e86121cf2113b32bab7c9594038c";

    public static void main(String[] args) throws IOException {
        // Создаем HTTP сервер
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new HtmlFormHandler());
        server.createContext("/weather", new WeatherHandler());
        server.setExecutor(null);

        // Останавливаем сервер через 100 секунд
        scheduleServerStop(server, 10000);

        System.out.println("Сервер запущен на http://localhost:8080");
        server.start();
    }

    private static void scheduleServerStop(HttpServer server, int seconds) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            System.out.println("Остановка сервера была в течение " + seconds + " секунд...");
            server.stop(0);  // Останавливаем сервер
            System.out.println("Сервер остановлен.");
        }, seconds, TimeUnit.SECONDS);
    }

    // Обработчик для отображения HTML формы
    static class HtmlFormHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = """
                    <html>
                    <head>
                        <title>Weather App</title>
                        <style>
                            body {
                                font-family: Arial, sans-serif;
                                background-color: #f4f4f9;
                                color: #333;
                                text-align: center;
                                margin: 0;
                                padding: 0;
                            }
                            h1 {
                                color: #007BFF;
                                margin-top: 50px;
                            }
                            form {
                                margin-top: 20px;
                            }
                            input[type="text"] {
                                padding: 10px;
                                width: 250px;
                                border: 1px solid #ccc;
                                border-radius: 5px;
                            }
                            button {
                                padding: 10px 15px;
                                background-color: #007BFF;
                                color: white;
                                border: none;
                                border-radius: 5px;
                                cursor: pointer;
                            }
                            button:hover {
                                background-color: #0056b3;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>Введите город для получения погоды</h1>
                        <form action="/weather" method="get">
                            <input type="text" name="city" placeholder="Введите город" required>
                            <button type="submit">Получить погоду</button>
                        </form>
                    </body>
                    </html>
                """;

                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // Обработчик для обработки данных и отображения погоды
    static class WeatherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String city = getCityFromQuery(query);

                if (city == null || city.isEmpty()) {
                    String response = "Не указан город. Вернитесь назад и заполните форму.";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    return;
                }

                // Получаем данные о погоде
                String weatherData = getWeatherData(city);

                // Формируем HTML-ответ
                String response = """
                    <html>
                    <head>
                        <title>Weather Result</title>
                        <style>
                            body {
                                font-family: Arial, sans-serif;
                                background-color: #f9f9f9;
                                color: #333;
                                margin: 0;
                                padding: 0;
                                text-align: center;
                            }
                            h1 {
                                color: #28a745;
                                margin-top: 50px;
                            }
                            .weather {
                                margin-top: 20px;
                                font-size: 18px;
                                background-color: #ffffff;
                                border: 1px solid #ddd;
                                border-radius: 5px;
                                padding: 20px;
                                display: inline-block;
                                box-shadow: 0px 4px 6px rgba(0, 0, 0, 0.1);
                            }
                            a {
                                display: inline-block;
                                margin-top: 20px;
                                padding: 10px 20px;
                                background-color: #007BFF;
                                color: white;
                                text-decoration: none;
                                border-radius: 5px;
                            }
                            a:hover {
                                background-color: #0056b3;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>Погода в городе: %s</h1>
                        <div class="weather">%s</div>
                        <br>
                        <a href="/">Назад</a>
                    </body>
                    </html>
                """.formatted(city, weatherData);

                exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        private String getCityFromQuery(String query) {
            if (query != null && query.startsWith("city=")) {
                return query.substring(5).replace("+", " ");
            }
            return null;
        }

        private String getWeatherData(String city) {
            try {
                String urlString = API_URL + "?q=" + city + "&appid=" + API_KEY + "&units=metric&lang=ru";
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    return parseWeatherData(response.toString());
                } else {
                    return "Ошибка: не удалось получить данные о погоде. Код ответа: " + responseCode;
                }
            } catch (Exception e) {
                return "Ошибка: " + e.getMessage();
            }
        }

        private String parseWeatherData(String jsonData) {
            try {
                String cityName = extractValue(jsonData, "\"name\":\"", "\",");

                // Получаем информацию о погоде
                String iconCode = extractValue(jsonData, "\"icon\":\"", "\"");

                // Получаем URL иконки
                String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                String temperature = extractValue(jsonData, "\"temp\":", ",");
                String feelsLike = extractValue(jsonData, "\"feels_like\":", ",");
                String humidity = extractValue(jsonData, "\"humidity\":", ",");
                String clouds = extractValue(jsonData, "\"all\":", "}");
                String windSpeed = extractValue(jsonData, "\"speed\":", ",");
                String windDeg = extractValue(jsonData, "\"deg\":", "}");


                return """
            <h2>Город: %s</h2>
            <img src="%s" alt="Weather Icon"/><br>
            Температура: %.1f°C<br>
            Ощущается как: %.1f°C<br>
            Облачность: %s%%<br>
            Влажность: %s%%<br>
            Ветер: %s м/с, направление - %s°
        """.formatted(
                        cityName,
                        iconUrl,
                        Double.parseDouble(temperature),
                        Double.parseDouble(feelsLike),
                        clouds, humidity,
                        windSpeed, windDeg
                );
            } catch (Exception e) {
                return "Ошибка обработки данных: " + e.getMessage();
            }
        }


        private String extractValue(String json, String start, String end) {
            int startIndex = json.indexOf(start) + start.length();
            int endIndex = json.indexOf(end, startIndex);
            if (startIndex > -1 && endIndex > -1) {
                return json.substring(startIndex, endIndex);
            }
            return "";
        }
    }
}
