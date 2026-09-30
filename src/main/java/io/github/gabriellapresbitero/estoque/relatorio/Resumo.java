package io.github.gabriellapresbitero.estoque.relatorio;

import io.github.gabriellapresbitero.estoque.Formatos;
import io.github.gabriellapresbitero.estoque.modelo.Situacao;
import io.github.gabriellapresbitero.estoque.modelo.Sugestao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Resumo em texto para o terminal: o que comprar, quanto vai custar e onde há dinheiro parado. */
public class Resumo {

    public String gerar(List<Sugestao> sugestoes, LocalDate dataReferencia) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(78)).append('\n');
        sb.append("REPOSIÇÃO DE ESTOQUE: análise até ").append(dataReferencia.format(Formatos.DATA)).append('\n');
        sb.append("=".repeat(78)).append('\n');

        Map<Situacao, Integer> contagem = new EnumMap<>(Situacao.class);
        for (Situacao situacao : Situacao.values()) {
            contagem.put(situacao, 0);
        }
        sugestoes.forEach(s -> contagem.merge(s.situacao(), 1, Integer::sum));
        contagem.forEach((situacao, total) -> sb.append(String.format("%-52s %4d produto(s)%n", situacao.descricao(), total)));

        BigDecimal totalCompra = somar(sugestoes.stream().map(Sugestao::valorCompra).toList());
        BigDecimal capitalParado = somar(sugestoes.stream().map(Sugestao::capitalParado).toList());
        sb.append('\n');
        sb.append("Investimento sugerido em compras: ").append(Formatos.moeda(totalCompra)).append('\n');
        sb.append("Capital parado (excesso + sem giro): ").append(Formatos.moeda(capitalParado)).append('\n');

        List<Sugestao> compras = sugestoes.stream().filter(s -> s.situacao().precisaComprar()).toList();
        if (!compras.isEmpty()) {
            sb.append("\nLISTA DE COMPRAS (mais urgente primeiro)\n");
            sb.append("-".repeat(78)).append('\n');
            sb.append(String.format("%-8s %-30s %3s %8s %7s %7s %13s%n", "SKU", "Produto", "ABC", "Estoque", "P.Ped.", "Comprar", "Valor"));
            for (Sugestao s : compras) {
                sb.append(String.format("%-8s %-30.30s %3s %8d %7d %7d %13s%s%n",
                        s.produto().sku(), s.produto().nome(), s.classe(), s.produto().estoqueAtual(),
                        s.pontoPedido(), s.comprar(), Formatos.moeda(s.valorCompra()),
                        s.situacao() == Situacao.RUPTURA ? "  (em falta)" : ""));
            }
        }

        List<Sugestao> parados = sugestoes.stream().filter(s -> s.capitalParado().signum() > 0).toList();
        if (!parados.isEmpty()) {
            sb.append("\nDINHEIRO PARADO NO ESTOQUE\n");
            sb.append("-".repeat(78)).append('\n');
            for (Sugestao s : parados) {
                String detalhe = s.situacao() == Situacao.SEM_GIRO
                        ? "não vendeu no período"
                        : String.format("estoque para %.0f dias", s.coberturaDias());
                sb.append(String.format("%-8s %-30.30s %12s  %s%n",
                        s.produto().sku(), s.produto().nome(), Formatos.moeda(s.capitalParado()), detalhe));
            }
        }
        return sb.toString();
    }

    private static BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
