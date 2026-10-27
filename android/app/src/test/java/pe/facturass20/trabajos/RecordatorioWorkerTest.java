package pe.facturass20.trabajos;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.Test;

/** La revisión diaria corre a las 9:00. */
public class RecordatorioWorkerTest {

    @Test
    public void antesDeLasNueveEsHoyYDespuesManana() {
        assertEquals(Duration.ofHours(2), RecordatorioWorker.hastaLaProxima(LocalDateTime.of(2026, 10, 27, 7, 0)));
        assertEquals(Duration.ofHours(24), RecordatorioWorker.hastaLaProxima(LocalDateTime.of(2026, 10, 27, 9, 0)));
        assertEquals(Duration.ofMinutes(15 * 60 + 30),
                RecordatorioWorker.hastaLaProxima(LocalDateTime.of(2026, 10, 27, 17, 30)));
    }
}
