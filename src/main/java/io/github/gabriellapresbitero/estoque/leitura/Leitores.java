package io.github.gabriellapresbitero.estoque.leitura;

import io.github.gabriellapresbitero.estoque.modelo.Produto;
import io.github.gabriellapresbitero.estoque.modelo.Venda;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Leitura dos dois arquivos de entrada: cadastro de produtos e histórico de vendas. */
public final class Leitores {

    public static final List<String> COLUNAS_PRODUTOS = List.of("sku", "nome", "estoque_atual", "lead_time_dias", "custo_unitario");
    public static final List<String> COLUNAS_VENDAS = List.of("data", "sku", "quantidade");

    private Leitores() {
    }

    public static List<Produto> produtos(Path arquivo) throws IOException {
        return Csv.ler(arquivo, COLUNAS_PRODUTOS, Leitores::produto);
    }

    public static List<Venda> vendas(Path arquivo) throws IOException {
        return Csv.ler(arquivo, COLUNAS_VENDAS, Leitores::venda);
    }

    static Produto produto(Csv.Linha linha) {
        return new Produto(
                linha.texto("sku"),
                linha.texto("nome"),
                linha.inteiro("estoque_atual"),
                linha.inteiro("lead_time_dias"),
                linha.dinheiro("custo_unitario"),
                linha.inteiro("lote_minimo", 1));
    }

    static Venda venda(Csv.Linha linha) {
        return new Venda(linha.data("data"), linha.texto("sku"), linha.inteiro("quantidade"));
    }
}
