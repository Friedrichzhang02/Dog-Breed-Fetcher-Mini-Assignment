package dogapi;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedFetcher.BreedNotFoundException {
        try {
            if (breed == null || breed.trim().isEmpty()) {
                throw new BreedNotFoundException("Breed name is empty.");
            }

            final String normalized = breed.trim().toLowerCase(Locale.ROOT);
            final String url = "https://dog.ceo/api/breed/" + normalized + "/list";

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new BreedNotFoundException("Failed to fetch sub-breeds for: " + breed);
                }

                String body = response.body().string();
                JSONObject json = new JSONObject(body);

                // dog.ceo returns { status: "success" | "error", message: [...] | "error message" }
                String status = json.optString("status", "");
                if (!"success".equalsIgnoreCase(status)) {
                    String apiMsg = json.optString("message", "Breed not found: " + breed);
                    throw new BreedNotFoundException(apiMsg);
                }

                JSONArray arr = json.getJSONArray("message");
                List<String> result = new ArrayList<>(arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    result.add(arr.getString(i));
                }
                return result;
            }

        } catch (Exception e) {
            // Per the interface requirement: surface *any* failure as BreedNotFoundException
            if (e instanceof BreedNotFoundException) {
                throw (BreedNotFoundException) e;
            }
            throw new BreedNotFoundException("Breed not found or API error for: " + breed, e);
        }
    }
}