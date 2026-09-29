package pe.facturass20.datos.cifrado;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Supplier;

import javax.crypto.SecretKey;

/**
 * Fotos de las facturas cifradas con AES-256-GCM en {@code filesDir/imagenes/<uuid>.jpg.enc}
 * (documentación técnica §7.1). Nunca van a la galería ni al almacenamiento compartido; solo se descifran
 * en memoria para mostrarlas, para el PDF o para la exportación que pida el usuario.
 */
public final class AlmacenImagenes {

    private static final String CARPETA = "imagenes";
    private static final String EXTENSION = ".jpg.enc";

    private final File carpeta;
    private final Supplier<SecretKey> clave;

    public AlmacenImagenes(Context contexto) {
        this(new File(contexto.getFilesDir(), CARPETA), GestorClaves::claveImagenes);
    }

    AlmacenImagenes(File carpeta, Supplier<SecretKey> clave) {
        this.carpeta = carpeta;
        this.clave = clave;
    }

    /** Cifra y guarda la foto; devuelve el nombre del archivo, que es lo que va en {@code ruta_cifrada}. */
    public String guardar(byte[] jpeg) {
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            throw new UncheckedIOException(new IOException("No se pudo crear " + carpeta));
        }
        String nombre = UUID.randomUUID() + EXTENSION;
        try {
            Files.write(archivo(nombre).toPath(), CifradoAesGcm.cifrar(clave.get(), jpeg));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return nombre;
    }

    /** La foto descifrada, solo en memoria. */
    public byte[] leer(String nombre) {
        try {
            return CifradoAesGcm.descifrar(clave.get(), Files.readAllBytes(archivo(nombre).toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public boolean borrar(String nombre) {
        return archivo(nombre).delete();
    }

    /** Solo acepta nombres generados por {@link #guardar}, para no salir de la carpeta. */
    File archivo(String nombre) {
        if (!nombre.matches("[0-9a-f-]{36}\\.jpg\\.enc")) {
            throw new IllegalArgumentException("Nombre de imagen inválido: " + nombre);
        }
        return new File(carpeta, nombre);
    }
}
