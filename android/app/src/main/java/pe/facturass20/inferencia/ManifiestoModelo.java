package pe.facturass20.inferencia;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * El modelo que corresponde a esta versión de la app ({@code assets/modelo.json}, documentación técnica
 * §6.6). Una versión nueva del modelo llega con una actualización de la app que trae otro manifiesto.
 *
 * @param url         apunta a una revisión fija del repositorio, para que el archivo no cambie bajo la
 *                    misma versión
 * @param sha256      huella del archivo en hexadecimal, en minúsculas
 * @param tamanoBytes tamaño exacto del archivo
 */
public record ManifiestoModelo(String version, String url, String sha256, long tamanoBytes) {

    public static final String ARCHIVO = "modelo.json";

    public ManifiestoModelo {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(url, "url");
        if (!url.startsWith("https://")) {
            throw new IllegalArgumentException("La URL del modelo debe ser HTTPS: " + url);
        }
        sha256 = Objects.requireNonNull(sha256, "sha256").toLowerCase(Locale.ROOT);
        if (!sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("SHA-256 inválido: " + sha256);
        }
        if (tamanoBytes <= 0) {
            throw new IllegalArgumentException("Tamaño inválido: " + tamanoBytes);
        }
    }

    public static ManifiestoModelo leer(InputStream entrada) throws IOException {
        try (Reader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(lector).getAsJsonObject();
            return new ManifiestoModelo(json.get("version").getAsString(), json.get("url").getAsString(),
                    json.get("sha256").getAsString(), json.get("tamanoBytes").getAsLong());
        } catch (JsonParseException | IllegalStateException | NullPointerException | IllegalArgumentException e) {
            throw new IOException("Manifiesto del modelo inválido", e);
        }
    }

    /** Nombre del archivo del modelo en el teléfono. */
    public String nombreArchivo() {
        return version + ".litertlm";
    }
}
