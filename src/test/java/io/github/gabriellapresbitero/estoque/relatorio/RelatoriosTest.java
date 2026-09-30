package io.github.gabriellapresbitero.estoque.relatorio;

import io.github.gabriellapresbitero.estoque.modelo.ClasseAbc;
import io.github.gabriellapresbitero.estoque.modelo.Produto;
import io.github.gabriellapresbitero.estoque.modelo.Situacao;
import io.github.gabriellapresbitero.estoque.modelo.Sugestao;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RelatoriosTest {

    private static final Sugestao FALTA = new Sugestao(
            new Produto("FEI1", "Feijão 1kg", 0, 4, new BigDecimal("7.49"), 10),
            21.5, 6.2, 21, 107, 0, 760, new BigDecimal("5692.40"), new BigDecimal("14000"),
            ClasseAbc.A, Situacao.RUPTURA, BigDecimal.ZERO);

    private static final Sugestao PARADO = new Sugestao(
            new Produto("PAN5", "Panetone", 48, 20, new BigDecimal("19.90"), 6),
            0, 0, 0, 0, Double.POSITIVE_INFINITY, 0, BigDecimal.ZERO, BigDecimal.ZERO,
            ClasseAbc.C, Situacao.SEM_GIRO, new BigDecimal("955.20"));

    @Test
    public void csvComFormatoBrasileiro() {
        List<String> linhas = new RelatorioCsv().linhas(List.of(FALTA, PARADO));
        assertEquals(RelatorioCsv.CABECALHO, linhas.get(0));
        assertEquals("FEI1;Feijão 1kg;A;RUPTURA;0;21,50;6,20;4;21;107;0,00;760;5692,40;0,00", linhas.get(1));
        assertTrue("cobertura infinita fica vazia", linhas.get(2).contains(";0;;0;"));
    }

    @Test
    public void resumoMostraComprasEDinheiroParado() {
        String texto = new Resumo().gerar(List.of(FALTA, PARADO), LocalDate.of(2025, 9, 30));
        assertTrue(texto, texto.contains("análise até 30/09/2025"));
        assertTrue(texto, texto.contains("Investimento sugerido em compras: R$ 5.692,40"));
        assertTrue(texto, texto.contains("(em falta)"));
        assertTrue(texto, texto.contains("não vendeu no período"));
    }
}
