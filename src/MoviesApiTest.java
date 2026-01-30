import com.google.gson.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        server.clearStore();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void shouldGetMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");

        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void shouldPostMovies() throws Exception {

        String json = """
                {
                  "title": "Matrix",
                  "year": 1999
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        assertTrue(body.contains("\"title\":\"Matrix\""));
        assertTrue(body.contains("\"year\":1999"));
        assertTrue(body.contains("\"id\""));
    }

    @Test
    void shouldHandleEmptyTitle() throws Exception {

        String json = """
                {
                  "title": "",
                  "year": 1999
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies с пустым заголовком должен вернуть 422");
    }

    @Test
    void shouldHandleTooLongTitle() throws Exception {

        String longTitle =
                "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "aaaaaaaaaa" +
                        "a";

        String json = """
{
  "title": "%s",
  "year": 1999
}
""".formatted(longTitle);

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies со слишком длинным заголовком должен вернуть 422");
    }

    @Test
    void shouldHandleIncorrectYearInPast() throws Exception {

        String json = """
                {
                  "title": "Matrix",
                  "year": 1777
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies с годом до 1888 должен вернуть 422");
    }

    @Test
    void shouldHandleIncorrectYearInFuture() throws Exception {

        String json = """
                {
                  "title": "Matrix",
                  "year": 2028
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode(), "POST /movies с годом из будущего должен вернуть 422");
    }

    @Test
    void shouldHaveErrorDataWhenTitle422() throws Exception {

        String json = """
                {
                  "title": "",
                  "year": 1999
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("\"error\""), "должно быть поле error");
        assertTrue(resp.body().contains("\"details\":["),
                "details должен быть массивом");
    }

    @Test
    void shouldHaveErrorDataWhenYear422() throws Exception {

        String json = """
                {
                  "title": "Matrix",
                  "year": 1799
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("\"error\""), "должно быть поле error");
        assertTrue(resp.body().contains("\"details\":["),
                "details должен быть массивом");
    }

    @Test
    void shouldHandleSeveralErrors() throws Exception {

        String json = """
                {
                  "title": "",
                  "year": 1799
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("\"error\""), "должно быть поле error");
        assertTrue(resp.body().contains("\"details\":["),
                "details должен быть массивом");
        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray details = obj.getAsJsonArray("details");
        assertEquals(2, details.size(),
                "При двух ошибках валидации details должен содержать 2 элемента");
    }

    @Test
    void shouldHandleSingleError() throws Exception {

        String json = """
                {
                  "title": "",
                  "year": 1999
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("\"error\""), "должно быть поле error");
        assertTrue(resp.body().contains("\"details\":["),
                "details должен быть массивом");
        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        JsonArray details = obj.getAsJsonArray("details");
        assertEquals(1, details.size(),
                "При 1 ошибках валидации details должен содержать 1 элемента");
    }

    @Test
    void shouldHandleWrongContentType() throws Exception {
        String json = """
                {
                  "title": "Matrix",
                  "year": 1999
                }
                """;
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "xml")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode());
    }

    @Test
    void shouldGetMovieById() throws Exception {
        String json = """
    {
      "title": "Matrix",
      "year": 1999
    }
    """;

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> postResp =
                client.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp.statusCode());

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> getResp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResp.statusCode(), "GET /movies/{id} должен вернуть 200");

        JsonObject obj =
                JsonParser.parseString(getResp.body()).getAsJsonObject();

        assertEquals(1, obj.get("id").getAsInt());
        assertEquals("Matrix", obj.get("title").getAsString());
        assertEquals(1999, obj.get("year").getAsInt());
    }

    @Test
    void shouldHandleIfMovieByIdNotFound() throws Exception {

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> getResp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, getResp.statusCode(), "GET /movies/{id} при отсутствии id должен вернуть 404");
    }

    @Test
    void shouldHandleIfIdNotInt() throws Exception {

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> getResp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, getResp.statusCode(), "GET /movies/{id} при id ≠ число должен вернуть 400");
    }

    @Test
    void shouldReturn204WhenDeleteMovieById() throws Exception {
        String json = """
    {
      "title": "Matrix",
      "year": 1999
    }
    """;

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> postResp =
                client.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp.statusCode());

        HttpRequest delete = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> deleteResp =
                client.send(delete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, deleteResp.statusCode(), "DELETE /movies/{id} должен вернуть 204");
    }

    @Test
    void shouldDeleteMovieById() throws Exception {
        String json = """
    {
      "title": "Matrix",
      "year": 1999
    }
    """;

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> postResp =
                client.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp.statusCode());

        HttpRequest delete = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> deleteResp =
                client.send(delete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, deleteResp.statusCode());

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> getResp =
                client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, getResp.statusCode(), "GET /movies/{id} после удаления должен вернуть 404");
    }

    @Test
    void shouldHandleIncorrectIdForDelete() throws Exception {
        String json = """
    {
      "title": "Matrix",
      "year": 1999
    }
    """;

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> postResp =
                client.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp.statusCode());

        HttpRequest delete = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/21"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(delete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, resp.statusCode(), "отсутствует фильм с указанным имдексом должен вернуть 404");
    }

    @Test
    void shouldGetMoviesOfCertainYear() throws Exception {
        String json1 = """
    {
      "title": "Matrix",
      "year": 1999
    }
    """;

        String json2 = """
    {
      "title": "OtherFilm",
      "year": 1999
    }
    """;

        String json3 = """
    {
      "title": "OtherFilm2",
      "year": 1999
    }
    """;

        String json4 = """
{
  "title": "OtherFilm3",
  "year": 2001
}
""";

        HttpRequest post1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .build();

        HttpRequest post2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();

        HttpRequest post3 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json3))
                .build();

        HttpRequest post4 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json4))
                .build();

        HttpResponse<String> postResp1 =
                client.send(post1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp2 =
                client.send(post2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp3 =
                client.send(post3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp4 =
                client.send(post4, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


        assertEquals(201, postResp1.statusCode());
        assertEquals(201, postResp2.statusCode());
        assertEquals(201, postResp3.statusCode());
        assertEquals(201, postResp4.statusCode());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1999"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies?year=YYYY при корректном указании года должен вернуть 200");

        assertTrue(resp.body().trim().startsWith("[") && resp.body().trim().endsWith("]"),
                "Ожидается JSON-массив");

        JsonArray jsonArray =
                JsonParser.parseString(resp.body()).getAsJsonArray();

        for (JsonElement element : jsonArray) {
            JsonObject movieObj = element.getAsJsonObject();
            assertEquals(1999, movieObj.get("year").getAsInt());
        }
    }

    @Test
    void shouldHandleIncorrectYear() throws Exception { //dodelat!
        String json1 = """
    {
      "title": "Matrix",
      "year": 1999
    }
    """;

        String json2 = """
    {
      "title": "OtherFilm",
      "year": 1999
    }
    """;

        String json3 = """
    {
      "title": "OtherFilm2",
      "year": 1999
    }
    """;

        String json4 = """
{
  "title": "OtherFilm3",
  "year": 2001
}
""";

        HttpRequest post1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .build();

        HttpRequest post2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();

        HttpRequest post3 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json3))
                .build();

        HttpRequest post4 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json4))
                .build();

        HttpResponse<String> postResp1 =
                client.send(post1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp2 =
                client.send(post2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp3 =
                client.send(post3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp4 =
                client.send(post4, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp1.statusCode());
        assertEquals(201, postResp2.statusCode());
        assertEquals(201, postResp3.statusCode());
        assertEquals(201, postResp4.statusCode());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(),
                "GET /movies?year=YYYY при некорректном указании года должен вернуть 400");
    }

    @Test
    void shouldGetMoviesByYear_whenEmpty_returnsEmptyArray() throws Exception { //dodelat!
        String json1 = """
    {
      "title": "Matrix",
      "year": 1999
    }
    """;

        String json2 = """
    {
      "title": "OtherFilm",
      "year": 1999
    }
    """;

        String json3 = """
    {
      "title": "OtherFilm2",
      "year": 1999
    }
    """;

        String json4 = """
{
  "title": "OtherFilm3",
  "year": 2001
}
""";

        HttpRequest post1 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .build();

        HttpRequest post2 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();

        HttpRequest post3 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json3))
                .build();

        HttpRequest post4 = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json4))
                .build();

        HttpResponse<String> postResp1 =
                client.send(post1, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp2 =
                client.send(post2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp3 =
                client.send(post3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpResponse<String> postResp4 =
                client.send(post4, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp1.statusCode());
        assertEquals(201, postResp2.statusCode());
        assertEquals(201, postResp3.statusCode());
        assertEquals(201, postResp4.statusCode());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1988"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }
}
