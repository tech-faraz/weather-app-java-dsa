import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class WeatherApp {


    public static JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);

        if (locationData == null || locationData.isEmpty()) {
            System.out.println("Error: No valid location data found.");
            return null;
        }


        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");


        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,weathercode,relativehumidity_2m,windspeed_10m&timezone=America%2FLos_Angeles";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);
            if (conn == null || conn.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to weather API.");
                return null;
            }


            StringBuilder resultJson = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                while (scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }
            }
            conn.disconnect();


            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(resultJson.toString());


            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);


            double temperature = ((Number) ((JSONArray) hourly.get("temperature_2m")).get(index)).doubleValue();
            String weatherCondition = convertWeatherCode((long) ((JSONArray) hourly.get("weathercode")).get(index));
            long humidity = (long) ((JSONArray) hourly.get("relativehumidity_2m")).get(index);
            double windspeed = ((Number) ((JSONArray) hourly.get("windspeed_10m")).get(index)).doubleValue();


            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static JSONArray getLocationData(String locationName) {
        locationName = locationName.replaceAll(" ", "+");
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);
            if (conn == null || conn.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to geolocation API.");
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                while (scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }
            }
            conn.disconnect();


            JSONParser parser = new JSONParser();
            JSONObject resultsJsonObj = (JSONObject) parser.parse(resultJson.toString());

            return (JSONArray) resultsJsonObj.get("results");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();
        for (int i = 0; i < timeList.size(); i++) {
            if (timeList.get(i).toString().equalsIgnoreCase(currentTime)) {
                return i;
            }
        }
        System.out.println("Warning: Current time not found in API data. Defaulting to index 0.");
        return 0;
    }


    private static String getCurrentTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00");
        return currentDateTime.format(formatter);
    }


    private static String convertWeatherCode(long weathercode) {
        if (weathercode == 0L) {
            return "Clear";
        } else if (weathercode >= 1L && weathercode <= 3L) {
            return "Cloudy";
        } else if ((weathercode >= 51L && weathercode <= 67L) || (weathercode >= 80L && weathercode <= 99L)) {
            return "Rain";
        } else if (weathercode >= 71L && weathercode <= 77L) {
            return "Snow";
        }
        return "Unknown";
    }
}
