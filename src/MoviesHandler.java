import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    Gson gson = new Gson();


    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();
        switch (method) {
            case "GET":
                handleGet(ex);
                break;
            case "POST":
                handlePost(ex);
                break;
            case "DELETE":
                handleDelete(ex);
                break;
            default:
                send405(ex);
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String queryString = ex.getRequestURI().getQuery();
        String[] pathParts = ex.getRequestURI().getPath().split("/");

        if (pathParts.length == 3) {
            try {
                int id = Integer.parseInt(pathParts[2]);
                Movie movieById = moviesStore.getMovieById(id);
                if (movieById == null) {
                    sendErrorJson(ex, 404, "Фильм не найден", null);
                    return;
                }
                String movieByIdJson = gson.toJson(movieById);
                sendJson(ex, 200, movieByIdJson);
                return;
            } catch (NumberFormatException e) {
                sendErrorJson(ex, 400, "Некорректный ID", null);
                return;
            }
        }

        if (pathParts.length == 2 && pathParts[1].equals("movies")) {
            if (queryString == null) {
                ArrayList<Movie> moviesList = moviesStore.getAllMovies();
                String moviesListJson = gson.toJson(moviesList);
                sendJson(ex, 200, moviesListJson);
                return;
            }
            String[] query = queryString.split("=");
            if (query.length != 2 || !query[0].equals("year")) {
                sendErrorJson(ex, 400, "Некорректный параметр запроса — 'year'", null);
            return;
            }
            String movieYear = query[1];
            int movieYearInt;
            try {
                movieYearInt = Integer.parseInt(movieYear);
            } catch (NumberFormatException e) {
                sendErrorJson(ex, 400, "Некорректный параметр запроса — 'year'", null);
                return;
            }
            ArrayList<Movie> movieList = moviesStore.getAllMovies();
            ArrayList<Movie> movieListByYear = new ArrayList<>();
            for (Movie movie : movieList) {
                if (movie.getYear() == movieYearInt) {
                    movieListByYear.add(movie);
                }
            }
            String movieListByYearJson = gson.toJson(movieListByYear);
            sendJson(ex, 200, movieListByYearJson);
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            sendErrorJson(ex, 415, "некорректный заголовок запроса", null);
            return;
        }
        InputStream bodyStream = ex.getRequestBody();
        String body = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
        List<String> detailsList = new ArrayList<>();
        JsonElement jsonElement = JsonParser.parseString(body);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String title = jsonObject.get("title").getAsString();
        int year = jsonObject.get("year").getAsInt();

        if (title == null || title.isBlank() || title.length() > 100) {
            detailsList.add("некорректное название");
        }
        if (year < 1888 || year > (java.time.LocalDate.now().getYear() + 1)) {
            detailsList.add("некорректный год");
        }
        if (!detailsList.isEmpty()) {
            String[] details = detailsList.toArray(new String[0]);
            sendErrorJson(ex, 422, "ошибка валидации", details);
            return;
        }
            Movie movie = moviesStore.createMovie(title, year);
            String jsonBody = gson.toJson(movie);
            sendJson(ex, 201, jsonBody);
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        if (pathParts.length == 3) {
            try {
                int id = Integer.parseInt(pathParts[2]);
                Movie movieById = moviesStore.getMovieById(id);
                if (movieById == null) {
                    sendErrorJson(ex, 404, "Некорректный ID", null);
                    return;
                }
                    moviesStore.deleteMovieById(id);
                    sendNoContent(ex);
            } catch (NumberFormatException e) {
                sendErrorJson(ex, 400, "Некорректный ID", null);
            }
        }
    }

    private void send405(HttpExchange ex) throws IOException {
        sendNoContent(ex);
    }
}