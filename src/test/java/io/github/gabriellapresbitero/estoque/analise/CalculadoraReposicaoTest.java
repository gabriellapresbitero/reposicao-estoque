package io.github.gabriellapresbitero.estoque.analise;

import io.github.gabriellapresbitero.estoque.leitura.Leitores;
import io.github.gabriellapresbitero.estoque.modelo.Produto;
import io.github.gabriellapresbitero.estoque.modelo.Situacao;
import io.github.gabriellapresbitero.estoque.modelo.Sugestao;
import io.github.gabriellapresbitero.estoque.modelo.Venda;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CalculadoraReposicaoTest {

    private static final LocalDate HOJE = LocalDate.of(2025, 9, 30);
    /** Janela curta (10 dias) para os números dos testes ficarem fáceis de conferir. */
    private static final Parametros PARAMETROS = new Parametros(95, 30, 10, 90);

    private final CalculadoraReposicao calculadora = new CalculadoraReposicao(PARAMETROS);

    private static Produto produto(String sku, int estoque, int leadTime, int lote) {
        return new Produto(sku, "Produto " + sku, estoque, leadTime, new BigDecimal("10.00"), lote);
    }

    /** Vendas iguais todos os dias da janela. */
    private static List<Venda> vendasConstantes(String sku, int porDia) {
        List<Venda> vendas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            vendas.add(new Venda(HOJE.minusDays(i), sku, porDia));
        }
        return vendas;
    }

    private Sugestao unica(Produto p, List<Venda> vendas) {
        return calculadora.calcular(List.of(p), vendas, HOJE).get(0);
    }

    @Test
    public void demandaConstanteNaoPrecisaDeEstoqueDeSeguranca() {
        Sugestao s = unica(produto("A", 1000, 5, 1), vendasConstantes("A", 4));
        assertEquals(4.0, s.demandaMedia(), 1e-9);
        assertEquals(0, s.estoqueSeguranca());
        assertEquals(20, s.pontoPedido()); // 4 por dia × 5 dias de entrega
    }

    @Test
    public void diasSemVendaEntramNaMedia() {
        // 50 unidades num único dia da janela de 10 dias = média de 5 por dia, não 50.
        Sugestao s = unica(produto("A", 1000, 4, 1), List.of(new Venda(HOJE, "A", 50)));
        assertEquals(5.0, s.demandaMedia(), 1e-9);
        assertTrue("venda concentrada exige estoque de segurança", s.estoqueSeguranca() > 0);
    }

    @Test
    public void estoqueDeSegurancaSegueAFormula() {
        List<Venda> vendas = new ArrayList<>();
        int[] porDia = {2, 4, 4, 4, 5, 5, 7, 9, 5, 5};
        for (int i = 0; i < porDia.length; i++) {
            vendas.add(new Venda(HOJE.minusDays(i), "A", porDia[i]));
        }
        Sugestao s = unica(produto("A", 1000, 4, 1), vendas);
        double desvio = Estatisticas.desvioPadrao(new double[]{2, 4, 4, 4, 5, 5, 7, 9, 5, 5});
        int esperado = (int) Math.ceil(1.645 * desvio * Math.sqrt(4));
        assertEquals(esperado, s.estoqueSeguranca());
        assertEquals((int) Math.ceil(5.0 * 4) + esperado, s.pontoPedido());
    }

    @Test
    public void ignoraVendasForaDaJanela() {
        List<Venda> vendas = new ArrayList<>(vendasConstantes("A", 2));
        vendas.add(new Venda(HOJE.minusDays(30), "A", 999)); // muito antiga
        vendas.add(new Venda(HOJE.plusDays(1), "A", 999)); // depois da data de referência
        assertEquals(2.0, unica(produto("A", 100, 3, 1), vendas).demandaMedia(), 1e-9);
    }

    @Test
    public void situacoes() {
        assertEquals(Situacao.RUPTURA, unica(produto("A", 0, 5, 1), vendasConstantes("A", 4)).situacao());
        assertEquals(Situacao.COMPRAR, unica(produto("A", 20, 5, 1), vendasConstantes("A", 4)).situacao());
        assertEquals(Situacao.ATENCAO, unica(produto("A", 40, 5, 1), vendasConstantes("A", 4)).situacao());
        assertEquals(Situacao.OK, unica(produto("A", 100, 5, 1), vendasConstantes("A", 4)).situacao());
        assertEquals(Situacao.EXCESSO, unica(produto("A", 400, 5, 1), vendasConstantes("A", 4)).situacao());
        assertEquals(Situacao.SEM_GIRO, unica(produto("A", 10, 5, 1), List.of()).situacao());
        assertEquals(Situacao.OK, unica(produto("A", 0, 5, 1), List.of()).situacao());
    }

    @Test
    public void quantidadeACompraArredondadaParaOLote() {
        // alvo = 4 × (5 + 30) = 140; estoque 20 -> faltam 120; lote de 12 -> 120 (10 caixas)
        Sugestao s = unica(produto("A", 20, 5, 12), vendasConstantes("A", 4));
        assertEquals(120, s.comprar());
        assertEquals(new BigDecimal("1200.00"), s.valorCompra());

        // estoque 18 -> faltam 122 -> 11 caixas = 132
        assertEquals(132, unica(produto("A", 18, 5, 12), vendasConstantes("A", 4)).comprar());
    }

    @Test
    public void arredondarParaLote() {
        assertEquals(36, CalculadoraReposicao.arredondarParaLote(25, 12));
        assertEquals(24, CalculadoraReposicao.arredondarParaLote(24, 12));
        assertEquals(0, CalculadoraReposicao.arredondarParaLote(0, 12));
        assertEquals(7, CalculadoraReposicao.arredondarParaLote(7, 1));
    }

    @Test
    public void capitalParado() {
        // Sem giro: todo o estoque está parado.
        assertEquals(new BigDecimal("100.00"), unica(produto("A", 10, 5, 1), List.of()).capitalParado());
        // Excesso: 400 em estoque, 4 por dia × 90 dias = 360 necessários -> 40 unidades × R$ 10.
        assertEquals(new BigDecimal("400.00"), unica(produto("A", 400, 5, 1), vendasConstantes("A", 4)).capitalParado());
    }

    @Test
    public void avisaSobreVendasDeSkuDesconhecido() {
        List<Venda> vendas = new ArrayList<>(vendasConstantes("A", 1));
        vendas.add(new Venda(HOJE, "FANTASMA", 3));
        calculadora.calcular(List.of(produto("A", 10, 3, 1)), vendas, HOJE);
        assertEquals(1, calculadora.avisos().size());
        assertTrue(calculadora.avisos().get(0).contains("FANTASMA"));
    }

    @Test
    public void ordenaDoMaisUrgenteParaOMenos() {
        List<Venda> vendas = new ArrayList<>(vendasConstantes("OK", 4));
        vendas.addAll(vendasConstantes("FALTA", 4));
        List<Sugestao> resultado = calculadora.calcular(
                List.of(produto("OK", 100, 5, 1), produto("FALTA", 0, 5, 1)), vendas, HOJE);
        assertEquals("FALTA", resultado.get(0).produto().sku());
    }

    @Test
    public void exemploDoRepositorio() throws Exception {
        List<Produto> produtos = Leitores.produtos(Path.of("exemplos/produtos.csv"));
        List<Venda> vendas = Leitores.vendas(Path.of("exemplos/vendas.csv"));
        List<Sugestao> resultado = new CalculadoraReposicao().calcular(produtos, vendas, HOJE);

        assertEquals(20, resultado.size());
        Sugestao feijao = resultado.stream().filter(s -> s.produto().sku().equals("FEI1")).findFirst().orElseThrow();
        assertEquals(Situacao.RUPTURA, feijao.situacao());
        Sugestao panetone = resultado.stream().filter(s -> s.produto().sku().equals("PAN5")).findFirst().orElseThrow();
        assertEquals(Situacao.SEM_GIRO, panetone.situacao());
        // toda compra sugerida respeita o lote mínimo
        resultado.forEach(s -> assertEquals(s.produto().sku(), 0, s.comprar() % s.produto().loteMinimo()));
    }
}
