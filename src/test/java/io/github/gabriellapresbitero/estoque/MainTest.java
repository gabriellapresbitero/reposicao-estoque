package io.github.gabriellapresbitero.estoque;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MainTest {

    private final ByteArrayOutputStream saida = new ByteArrayOutputStream();
    private final ByteArrayOutputStream erro = new ByteArrayOutputStream();

    private int rodar(String... args) {
        return Main.executar(args, new PrintStream(saida, true, StandardCharsets.UTF_8),
                new PrintStream(erro, true, StandardCharsets.UTF_8));
    }

    @Test
    public void rodaOExemplo() throws Exception {
        Path destino = Files.createTempDirectory("estoque").resolve("sugestao.csv");
        assertEquals(0, rodar("--produtos", "exemplos/produtos.csv", "--vendas", "exemplos/vendas.csv",
                "--saida", destino.toString()));
        assertEquals(21, Files.readAllLines(destino).size()); // cabeçalho + 20 produtos
        String texto = saida.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("análise até 30/09/2025")); // usa a data da última venda
        assertTrue(texto.contains("LISTA DE COMPRAS"));
    }

    @Test
    public void nivelDeServicoMaiorPedeMaisEstoque() throws Exception {
        Path pasta = Files.createTempDirectory("estoque");
        rodar("--produtos", "exemplos/produtos.csv", "--vendas", "exemplos/vendas.csv",
                "--saida", pasta.resolve("a.csv").toString(), "--nivel-servico", "90");
        rodar("--produtos", "exemplos/produtos.csv", "--vendas", "exemplos/vendas.csv",
                "--saida", pasta.resolve("b.csv").toString(), "--nivel-servico", "99");
        assertTrue(somaEstoqueSeguranca(pasta.resolve("b.csv")) > somaEstoqueSeguranca(pasta.resolve("a.csv")));
    }

    private static int somaEstoqueSeguranca(Path csv) throws Exception {
        return Files.readAllLines(csv).stream().skip(1).mapToInt(l -> Integer.parseInt(l.split(";")[8])).sum();
    }

    @Test
    public void erros() {
        assertEquals(1, rodar("--produtos", "exemplos/produtos.csv"));
        assertEquals(1, rodar("--produtos", "nao-existe.csv", "--vendas", "exemplos/vendas.csv"));
        assertEquals(1, rodar("--produtos", "exemplos/produtos.csv", "--vendas", "exemplos/vendas.csv", "--nivel-servico", "93"));
        String mensagens = erro.toString(StandardCharsets.UTF_8);
        assertTrue(mensagens.contains("não encontrado"));
        assertTrue(mensagens.contains("93"));
    }
}
