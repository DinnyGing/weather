package com.itvdn.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class Weather {
    enum City{
        KHARKOV("kharkov", 49.99231719970703, 36.231014251708984),
        LVIV("lviv", 49.84195327758789, 49.84195327758789),
        KYIV("kyiv", 50.450035095214844, 50.450035095214844);
        final String name;
        final double lat;
        final double lon;

        City(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }

        public String getName() {
            return name;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        @Override
        public String toString() {
            return "City{" +
                    "name='" + name + '\'' +
                    ", lat=" + lat +
                    ", lon=" + lon +
                    '}';
        }
    }
    String apiUrl;
    String from;
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        System.out.println("What are you interested in? 1. Kyiv, 2. Lviv, 3. Kharkov");
        Scanner in = new Scanner(System.in);
        City city = City.KYIV;
        switch (in.nextInt()){
            case 2:
                city = City.LVIV;
                break;
            case 3:
                city = City.KHARKOV;
                break;
        }
        System.out.println(city);
        ExecutorService service = Executors.newFixedThreadPool(3);
        Weather openweathermap = new Weather("https://api.openweathermap.org/data/2.5/onecall?lat=??&lon=??&appid=e7704bc895b4a8d2dfd4a29d404285b6", city.getLat(), city.getLon());
        Weather tomorrow = new Weather("https://api.tomorrow.io/v4/weather/realtime?location=??&apikey=0JXYYOl1PzTeB9Lx4IupQhaFIaOU93dV", city.getName());
        Weather weatherbit = new Weather("https://api.weatherbit.io/v2.0/current?lat=??&lon=??&key=97c21901a3214e03a2f63560a7fa57e2", city.getLat(), city.getLon());
        Weather[] weathers = {openweathermap, tomorrow, weatherbit};
        Callable<String>[] callables = new Callable[3];
        int i = 0;
//        long t1 = System.nanoTime();
        for (Weather weather : weathers){
            callables[i] = () -> {
                System.out.printf("%s started working\n", Thread.currentThread().getName());
            Thread.sleep(1000);
                return String.valueOf(weather.getTemp());
            };
            i++;
        }
        List<Future<String>> futures = new ArrayList<>();
        for (Callable<String> callable : callables){
            futures.add(service.submit(callable));
        }

        System.out.println("Main continue working");

        for (Future<String> future : futures) {
            print(Double.parseDouble(future.get()));
        }
//        long t2 = System.nanoTime();
//        long t3 = System.nanoTime();
//        for (Weather weather : weathers){
//            print(weather.getTemp());
//        }
//        long t4 = System.nanoTime();
//        System.out.println(t2 - t1);
//        System.out.println(t4 - t3);
        service.shutdown();

    }
    public static void print(double value){
        System.out.printf("Current temp: %.2f ºC\n", value);
    }

    public Weather(String apiUrl, String city) {
        this.from = apiUrl.split("\\.")[1];
        this.apiUrl = apiUrl.replaceFirst("\\?\\?", String.valueOf(city));
    }
    public Weather(String apiUrl, double lat, double lon) {
        this.from = apiUrl.split("\\.")[1];
        this.apiUrl = apiUrl.replaceFirst("\\?\\?", String.valueOf(lat))
                .replaceFirst("\\?\\?", String.valueOf(lon));
    }
    private String connection(){
        String res = "";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                res = response.toString();
            } else {
                System.out.println("Mistake HTTP-respond: " + responseCode);
            }
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    private double getTempApiOpenweathermap() {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(connection()).getAsJsonObject();
        JsonObject current = jsonObject.getAsJsonObject("current");
        return current.getAsJsonPrimitive("temp").getAsDouble() - 273.15;
    }
    private double getTempApiTomorrow(){
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(connection()).getAsJsonObject();
        JsonObject current = jsonObject.getAsJsonObject("data").getAsJsonObject("values");
        return current.getAsJsonPrimitive("temperature").getAsDouble();
    }
    private double getTempApiWeatherbit(){
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(connection()).getAsJsonObject();
        JsonObject current = jsonObject.getAsJsonArray("data").get(0).getAsJsonObject();
        return current.getAsJsonPrimitive("app_temp").getAsDouble();
    }
    public double getTemp(){
        double temp = 0;
        switch (from){
            case "tomorrow":
                temp = getTempApiTomorrow();
                break;
            case "weatherbit":
                temp = getTempApiWeatherbit();
                break;
            case "openweathermap":
                temp = getTempApiOpenweathermap();
                break;
        }
        return temp;
    }
}
