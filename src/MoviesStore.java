import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MoviesStore {

    Map<Integer, Movie> moviesStore = new HashMap<>();
    private int idCount = 0;

    protected int generateId() {
        return ++idCount;
    }

    ArrayList<Movie> getAllMovies() {
        return new ArrayList<>(moviesStore.values());
    }

    public Movie getMovieById(int id) {
        return moviesStore.get(id);
    }

    public void deleteMovieById(int id) {
        moviesStore.remove(id);
    }

    void clearMovieStore() {
        moviesStore.clear();
        idCount = 0;
    }

    public Movie createMovie(String title, int year) {
        Movie movie = new Movie(generateId(), title, year);
        moviesStore.put(movie.getId(), movie);
        return movie;
    }
}
