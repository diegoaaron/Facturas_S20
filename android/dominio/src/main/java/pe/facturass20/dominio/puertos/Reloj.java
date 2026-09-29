package pe.facturass20.dominio.puertos;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** «Hoy» en la zona de Lima. Es un puerto para que las pruebas puedan fijar la fecha. */
public interface Reloj {

    ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    LocalDate hoy();

    LocalDateTime ahora();

    /** El reloj real del teléfono, en hora de Lima. */
    static Reloj sistema() {
        return deClock(Clock.system(ZONA_LIMA));
    }

    static Reloj deClock(Clock clock) {
        return new Reloj() {
            @Override
            public LocalDate hoy() {
                return LocalDate.now(clock);
            }

            @Override
            public LocalDateTime ahora() {
                return LocalDateTime.now(clock);
            }
        };
    }
}
