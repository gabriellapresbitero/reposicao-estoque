package io.github.gabriellapresbitero.estoque.analise;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class EstatisticasTest {

    @Test
    public void mediaEDesvioPadrao() {
        double[] vendas = {2, 4, 4, 4, 5, 5, 7, 9};
        assertEquals(5.0, Estatisticas.media(vendas), 1e-9);
        // desvio amostral: raiz(32 / 7)
        assertEquals(Math.sqrt(32.0 / 7), Estatisticas.desvioPadrao(vendas), 1e-9);
    }

    @Test
    public void casosDeBorda() {
        assertEquals(0.0, Estatisticas.media(new double[0]), 0);
        assertEquals(0.0, Estatisticas.desvioPadrao(new double[]{3}), 0);
        assertEquals(0.0, Estatisticas.desvioPadrao(new double[]{3, 3, 3}), 0);
    }

    @Test
    public void fatorZ() {
        assertEquals(1.645, NivelServico.fatorZ(95), 0);
        assertEquals(2.326, NivelServico.fatorZ(99), 0);
        assertThrows(IllegalArgumentException.class, () -> NivelServico.fatorZ(93));
    }

    @Test
    public void parametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new Parametros(95, 0, 90, 90));
        assertThrows(IllegalArgumentException.class, () -> new Parametros(95, 30, 3, 90));
        assertThrows(IllegalArgumentException.class, () -> new Parametros(42, 30, 90, 90));
    }
}
