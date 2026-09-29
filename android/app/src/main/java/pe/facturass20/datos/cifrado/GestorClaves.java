package pe.facturass20.datos.cifrado;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Claves de la app (documentación técnica §7.1 y §10). Las claves AES viven en el Android Keystore y
 * nunca salen de él:
 * <ul>
 *   <li><b>Base de datos:</b> SQLCipher necesita la clave en bytes, así que se genera una aleatoria de 32
 *       bytes y se guarda <i>envuelta</i> (cifrada con una clave del Keystore) en preferencias privadas.</li>
 *   <li><b>Imágenes:</b> se cifran directamente con otra clave del Keystore.</li>
 * </ul>
 * Si se borran los datos de la app se pierden las claves y con ellas la base; por eso no hay copias de
 * seguridad automáticas y el respaldo es la exportación (§9).
 */
public final class GestorClaves {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS_BASE = "facturas_s20_base";
    private static final String ALIAS_IMAGENES = "facturas_s20_imagenes";
    private static final String PREFERENCIAS = "claves";
    private static final String CLAVE_BASE_ENVUELTA = "clave_base_envuelta";
    private static final int LARGO_CLAVE_BASE = 32;

    private GestorClaves() { }

    /** Clave de SQLCipher; se crea la primera vez y después siempre devuelve la misma. */
    public static synchronized byte[] claveBaseDatos(Context contexto) {
        SharedPreferences preferencias =
                contexto.getApplicationContext().getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE);
        SecretKey envoltura = claveKeystore(ALIAS_BASE);
        String guardada = preferencias.getString(CLAVE_BASE_ENVUELTA, null);
        if (guardada != null) {
            return CifradoAesGcm.descifrar(envoltura, Base64.getDecoder().decode(guardada));
        }
        byte[] clave = new byte[LARGO_CLAVE_BASE];
        new SecureRandom().nextBytes(clave);
        String envuelta = Base64.getEncoder().encodeToString(CifradoAesGcm.cifrar(envoltura, clave));
        if (!preferencias.edit().putString(CLAVE_BASE_ENVUELTA, envuelta).commit()) {
            throw new IllegalStateException("No se pudo guardar la clave de la base de datos");
        }
        return clave;
    }

    /** Clave AES-256 del Keystore para las fotos de las facturas. */
    public static SecretKey claveImagenes() {
        return claveKeystore(ALIAS_IMAGENES);
    }

    private static synchronized SecretKey claveKeystore(String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (keyStore.containsAlias(alias)) {
                return (SecretKey) keyStore.getKey(alias, null);
            }
            KeyGenerator generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
            generador.init(new KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            return generador.generateKey();
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo acceder al Keystore", e);
        }
    }
}
