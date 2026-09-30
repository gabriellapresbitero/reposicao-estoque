package io.github.gabriellapresbitero.estoque.leitura;

import io.github.gabriellapresbitero.estoque.modelo.Produto;
import io.github.gabriellapresbitero.estoque.modelo.Venda;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class LeitoresTest {

    @Test
    public void leProdutosComLoteOpcional() {
        String csv = """
                SKU;Nome;Estoque Atual;Lead Time (dias);Custo Unitário;Lote Mínimo
                ARR5;Arroz 5kg;60;5;24,90;10
                FEI1;"Feijão; carioca";0;4;1.007,49;
                """;
        List<Produto> produtos = Csv.ler(csv, "produtos.csv", Leitores.COLUNAS_PRODUTOS, Leitores::produto);
        assertEquals(2, produtos.size());
        assertEquals(new BigDecimal("24.90"), produtos.get(0).custoUnitario());
        assertEquals(10, produtos.get(0).loteMinimo());
        assertEquals("Feijão; carioca", produtos.get(1).nome());
        assertEquals(new BigDecimal("1007.49"), produtos.get(1).custoUnitario());
        assertEquals(1, produtos.get(1).loteMinimo()); // vazio -> 1
    }

    @Test
    public void leVendas() {
        List<Venda> vendas = Csv.ler("data;sku;quantidade\n30/09/2025;ARR5;3\n2025-09-29;ARR5;1\n",
                "vendas.csv", Leitores.COLUNAS_VENDAS, Leitores::venda);
        assertEquals(new Venda(LocalDate.of(2025, 9, 30), "ARR5", 3), vendas.get(0));
        assertEquals(LocalDate.of(2025, 9, 29), vendas.get(1).data());
    }

    @Test
    public void erroComArquivoELinha() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> Csv.ler(
                "data;sku;quantidade\n30/09/2025;A;1\n30/09/2025;A;três\n",
                "vendas.csv", Leitores.COLUNAS_VENDAS, Leitores::venda));
        assertTrue(erro.getMessage(), erro.getMessage().startsWith("vendas.csv, linha 3"));
    }

    @Test
    public void erroQuandoFaltaColuna() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> Csv.ler(
                "data;sku\n30/09/2025;A\n", "vendas.csv", Leitores.COLUNAS_VENDAS, Leitores::venda));
        assertTrue(erro.getMessage().contains("quantidade"));
    }

    @Test
    public void produtoInvalido() {
        String csv = "sku;nome;estoque_atual;lead_time_dias;custo_unitario\nX;Y;-1;5;1,00\n";
        assertThrows(IllegalArgumentException.class,
                () -> Csv.ler(csv, "produtos.csv", Leitores.COLUNAS_PRODUTOS, Leitores::produto));
    }

    @Test
    public void normalizaNomesDeColunas() {
        assertEquals("lead_time_dias", Csv.normalizarNome(" Lead Time (dias) "));
        assertEquals("custo_unitario", Csv.normalizarNome("Custo Unitário"));
    }
}
