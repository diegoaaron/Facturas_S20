package pe.facturass20.datos.cifrado;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import pe.facturass20.datos.dao.ContribuyenteDao;
import pe.facturass20.datos.entidades.ContribuyenteEntity;

/**
 * PIN de 4 dígitos del acceso (P01, P02; documentación técnica §10). Se guarda solo su hash
 * PBKDF2-HMAC-SHA256 con una sal aleatoria de 16 bytes. El bloqueo tras 3 fallos lo aplica la pantalla.
 *
 * <p>Las consultas van a la base: se llama fuera del hilo de UI.</p>
 */
public final class GestorPin {

    static final int ITERACIONES = 120_000;
    private static final int BITS_HASH = 256;
    private static final int LARGO_SAL = 16;

    private final ContribuyenteDao dao;

    public GestorPin(ContribuyenteDao dao) {
        this.dao = dao;
    }

    public static boolean formatoValido(char[] pin) {
        if (pin == null || pin.length != 4) {
            return false;
        }
        for (char c : pin) {
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public boolean tienePin() {
        ContribuyenteEntity contribuyente = dao.obtener();
        return contribuyente != null && contribuyente.pinHash != null;
    }

    /** Define o cambia el PIN. Exige que el contribuyente ya esté guardado. */
    public void establecer(char[] pin) {
        if (!formatoValido(pin)) {
            throw new IllegalArgumentException("El PIN tiene 4 dígitos");
        }
        ContribuyenteEntity contribuyente = dao.obtener();
        if (contribuyente == null) {
            throw new IllegalStateException("Primero se guarda el contribuyente");
        }
        byte[] sal = new byte[LARGO_SAL];
        new SecureRandom().nextBytes(sal);
        Base64.Encoder base64 = Base64.getEncoder();
        dao.guardarPin(contribuyente.idContribuyente, base64.encodeToString(hash(pin, sal)),
                base64.encodeToString(sal));
    }

    /** Compara en tiempo constante; falso si todavía no hay PIN. */
    public boolean verificar(char[] pin) {
        ContribuyenteEntity contribuyente = dao.obtener();
        if (!formatoValido(pin) || contribuyente == null || contribuyente.pinHash == null) {
            return false;
        }
        Base64.Decoder base64 = Base64.getDecoder();
        byte[] esperado = base64.decode(contribuyente.pinHash);
        return MessageDigest.isEqual(esperado, hash(pin, base64.decode(contribuyente.pinSal)));
    }

    private static byte[] hash(char[] pin, byte[] sal) {
        PBEKeySpec spec = new PBEKeySpec(pin, sal, ITERACIONES, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        } finally {
            spec.clearPassword();
        }
    }
}
