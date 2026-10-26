package pe.facturass20.inferencia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.LongConsumer;

/** SHA-256 de un archivo, por bloques, para verificar el modelo descargado (§6.6, paso 4). */
public final class Huella {

    private static final int BLOQUE = 1 << 20;

    private Huella() { }

    /**
     * @param avance recibe los bytes leídos hasta el momento; puede ser nulo
     * @return la huella en hexadecimal, en minúsculas
     */
    public static String sha256(File archivo, LongConsumer avance) throws IOException {
        MessageDigest resumen;
        try {
            resumen = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] bloque = new byte[BLOQUE];
        long leidos = 0;
        try (InputStream entrada = new FileInputStream(archivo)) {
            for (int n; (n = entrada.read(bloque)) > 0; ) {
                resumen.update(bloque, 0, n);
                leidos += n;
                if (avance != null) {
                    avance.accept(leidos);
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("Verificación cancelada");
                }
            }
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte b : resumen.digest()) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
