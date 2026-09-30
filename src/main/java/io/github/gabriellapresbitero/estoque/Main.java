package io.github.gabriellapresbitero.estoque;

import io.github.gabriellapresbitero.estoque.analise.CalculadoraReposicao;
import io.github.gabriellapresbitero.estoque.analise.Parametros;
import io.github.gabriellapresbitero.estoque.leitura.Leitores;
import io.github.gabriellapresbitero.estoque.modelo.Produto;
import io.github.gabriellapresbitero.estoque.modelo.Sugestao;
import io.github.gabriellapresbitero.estoque.modelo.Venda;
import io.github.gabriellapresbitero.estoque.relatorio.RelatorioCsv;
import io.github.gabriellapresbitero.estoque.relatorio.Resumo;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Linha de comando.
 *
 * <pre>
 * java -jar target/reposicao.jar --produtos exemplos/produtos.csv --vendas exemplos/vendas.csv
 * </pre>
 */
public class Main {

    private static final String USO = """
            Uso: java -jar reposicao.jar --produtos ARQUIVO --vendas ARQUIVO [opções]

              --produtos ARQUIVO       cadastro: sku;nome;estoque_atual;lead_time_dias;custo_unitario[;lote_minimo]
              --vendas ARQUIVO         histórico: data;sku;quantidade
              --data AAAA-MM-DD        último dia de histórico a considerar (padrão: data da última venda)
              --nivel-servico P        80, 85, 90, 95, 97.5, 98, 99 ou 99.9 (padrão: 95)
              --cobertura N            dias de estoque que cada compra deve garantir além da entrega (padrão: 30)
              --janela N               dias de histórico usados na média (padrão: 90)
              --saida ARQUIVO          CSV com a análise completa (padrão: sugestao_compras.csv)
            """;

    public static void main(String[] args) {
        PrintStream saida = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        PrintStream erro = new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8);
        System.exit(executar(args, saida, erro));
    }

    static int executar(String[] args, PrintStream saida, PrintStream erro) {
        Path arquivoProdutos = null;
        Path arquivoVendas = null;
        Path destino = Path.of("sugestao_compras.csv");
        LocalDate data = null;
        Parametros p = Parametros.PADRAO;
        double nivel = p.nivelServico();
        int cobertura = p.diasCobertura();
        int janela = p.janelaDias();

        try {
            for (int i = 0; i < args.length; i++) {
                String opcao = args[i];
                if (opcao.equals("--ajuda") || opcao.equals("-h")) {
                    saida.print(USO);
                    return 0;
                }
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("falta o valor de " + opcao);
                }
                String valor = args[++i];
                switch (opcao) {
                    case "--produtos" -> arquivoProdutos = Path.of(valor);
                    case "--vendas" -> arquivoVendas = Path.of(valor);
                    case "--data" -> data = LocalDate.parse(valor);
                    case "--nivel-servico" -> nivel = Double.parseDouble(valor.replace(',', '.'));
                    case "--cobertura" -> cobertura = Integer.parseInt(valor);
                    case "--janela" -> janela = Integer.parseInt(valor);
                    case "--saida" -> destino = Path.of(valor);
                    default -> throw new IllegalArgumentException("opção desconhecida: " + opcao);
                }
            }
            if (arquivoProdutos == null || arquivoVendas == null) {
                throw new IllegalArgumentException("informe --produtos e --vendas");
            }

            List<Produto> produtos = Leitores.produtos(arquivoProdutos);
            List<Venda> vendas = Leitores.vendas(arquivoVendas);
            if (data == null) {
                data = vendas.stream().map(Venda::data).max(LocalDate::compareTo)
                        .orElseThrow(() -> new IllegalArgumentException("o arquivo de vendas está vazio"));
            }

            CalculadoraReposicao calculadora = new CalculadoraReposicao(
                    new Parametros(nivel, cobertura, janela, p.diasExcesso()));
            List<Sugestao> sugestoes = calculadora.calcular(produtos, vendas, data);

            new RelatorioCsv().salvar(sugestoes, destino);
            calculadora.avisos().forEach(aviso -> erro.println("Aviso: " + aviso));
            saida.print(new Resumo().gerar(sugestoes, data));
            saida.println("\nAnálise completa salva em: " + destino);
            return 0;
        } catch (NoSuchFileException e) {
            erro.println("Erro: arquivo não encontrado: " + e.getFile());
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            erro.println("Erro: " + e.getMessage());
            erro.print(USO);
        } catch (IOException e) {
            erro.println("Erro ao ler ou gravar arquivo: " + e.getMessage());
        }
        return 1;
    }
}
