package pe.facturass20.datos.cifrado;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM con el IV de 12 bytes antepuesto al texto cifrado: {@code [IV | cifrado + etiqueta]}.
 * El IV lo genera el Keystore en cada cifrado, así que nunca se repite con la misma clave.
 */
final class CifradoAesGcm {

    private static final String TRANSFORMACION = "AES/GCM/NoPadding";
    private static final int LARGO_IV = 12;
    private static final int BITS_ETIQUETA = 128;

    private CifradoAesGcm() { }

    static byte[] cifrar(SecretKey clave, byte[] datos) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, clave);
            byte[] iv = cipher.getIV();
            byte[] cifrado = cipher.doFinal(datos);
            byte[] salida = Arrays.copyOf(iv, iv.length + cifrado.length);
            System.arraycopy(cifrado, 0, salida, iv.length, cifrado.length);
            return salida;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar", e);
        }
    }

    /** Falla si los datos se alteraron o no se cifraron con esta clave (la etiqueta GCM no coincide). */
    static byte[] descifrar(SecretKey clave, byte[] datos) {
        if (datos.length <= LARGO_IV) {
            throw new IllegalArgumentException("Datos cifrados incompletos");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(BITS_ETIQUETA, datos, 0, LARGO_IV));
            return cipher.doFinal(datos, LARGO_IV, datos.length - LARGO_IV);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo descifrar", e);
        }
    }
}
