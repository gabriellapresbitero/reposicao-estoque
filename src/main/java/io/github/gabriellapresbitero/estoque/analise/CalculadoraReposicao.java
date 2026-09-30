package io.github.gabriellapresbitero.estoque.analise;

import io.github.gabriellapresbitero.estoque.modelo.ClasseAbc;
import io.github.gabriellapresbitero.estoque.modelo.Produto;
import io.github.gabriellapresbitero.estoque.modelo.Situacao;
import io.github.gabriellapresbitero.estoque.modelo.Sugestao;
import io.github.gabriellapresbitero.estoque.modelo.Venda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Calcula, para cada produto, quando e quanto comprar.
 *
 * <pre>
 * demanda média (d)        = unidades vendidas por dia, contando os dias SEM venda
 * desvio padrão (σ)        = variação da venda diária
 * estoque de segurança     = Z × σ × √(lead time)
 * ponto de pedido          = d × lead time + estoque de segurança
 * estoque alvo             = d × (lead time + dias de cobertura) + estoque de segurança
 * quantidade a comprar     = estoque alvo − estoque atual, arredondado para cima no lote mínimo
 * </pre>
 */
public class CalculadoraReposicao {

    /** Dias de antecedência para o alerta de "atenção". */
    static final int DIAS_ATENCAO = 7;

    private final Parametros parametros;
    private final List<String> avisos = new ArrayList<>();

    public CalculadoraReposicao() {
        this(Parametros.PADRAO);
    }

    public CalculadoraReposicao(Parametros parametros) {
        this.parametros = parametros;
    }

    /**
     * @param dataReferencia último dia do histórico considerado (normalmente, ontem)
     */
    public List<Sugestao> calcular(List<Produto> produtos, List<Venda> vendas, LocalDate dataReferencia) {
        avisos.clear();
        LocalDate inicio = dataReferencia.minusDays(parametros.janelaDias() - 1L);
        Map<String, Produto> porSku = produtos.stream()
                .collect(Collectors.toMap(Produto::sku, p -> p, (a, b) -> {
                    throw new IllegalArgumentException("SKU repetido no cadastro: " + a.sku());
                }));

        // Monta, para cada produto, um vetor com a venda de cada dia da janela (dias sem venda = 0).
        Map<String, double[]> vendaDiaria = new HashMap<>();
        produtos.forEach(p -> vendaDiaria.put(p.sku(), new double[parametros.janelaDias()]));
        Set<String> skusDesconhecidos = new TreeSet<>();
        for (Venda v : vendas) {
            if (v.data().isBefore(inicio) || v.data().isAfter(dataReferencia)) {
                continue;
            }
            double[] dias = vendaDiaria.get(v.sku());
            if (dias == null) {
                skusDesconhecidos.add(v.sku());
                continue;
            }
            dias[(int) ChronoUnit.DAYS.between(inicio, v.data())] += v.quantidade();
        }
        if (!skusDesconhecidos.isEmpty()) {
            avisos.add("vendas de SKUs que não estão no cadastro foram ignoradas: " + skusDesconhecidos);
        }

        // Curva ABC pelo valor vendido (quantidade × custo) no período.
        Map<String, BigDecimal> valorVendido = new HashMap<>();
        for (Produto p : produtos) {
            double unidades = 0;
            for (double d : vendaDiaria.get(p.sku())) {
                unidades += d;
            }
            valorVendido.put(p.sku(), p.custoUnitario().multiply(BigDecimal.valueOf((long) unidades)));
        }
        Map<String, ClasseAbc> classes = CurvaAbc.classificar(valorVendido);

        List<Sugestao> sugestoes = new ArrayList<>();
        for (Produto p : porSku.values()) {
            sugestoes.add(analisar(p, vendaDiaria.get(p.sku()), valorVendido.get(p.sku()), classes.get(p.sku())));
        }
        sugestoes.sort(Comparator.comparing(Sugestao::situacao)
                .thenComparing(Sugestao::classe)
                .thenComparing(Sugestao::valorCompra, Comparator.reverseOrder())
                .thenComparing(s -> s.produto().sku()));
        return sugestoes;
    }

    public List<String> avisos() {
        return List.copyOf(avisos);
    }

    Sugestao analisar(Produto p, double[] vendaDiaria, BigDecimal valorVendido, ClasseAbc classe) {
        double media = Estatisticas.media(vendaDiaria);
        double desvio = Estatisticas.desvioPadrao(vendaDiaria);
        double z = NivelServico.fatorZ(parametros.nivelServico());
        int lt = p.leadTimeDias();

        int estoqueSeguranca = (int) Math.ceil(z * desvio * Math.sqrt(lt));
        int pontoPedido = (int) Math.ceil(media * lt) + estoqueSeguranca;
        double cobertura = media > 0 ? p.estoqueAtual() / media : Double.POSITIVE_INFINITY;

        Situacao situacao = classificar(p, media, pontoPedido, cobertura);

        int comprar = 0;
        if (situacao.precisaComprar()) {
            int alvo = (int) Math.ceil(media * (lt + parametros.diasCobertura())) + estoqueSeguranca;
            comprar = arredondarParaLote(Math.max(alvo - p.estoqueAtual(), 0), p.loteMinimo());
        }

        BigDecimal capitalParado = BigDecimal.ZERO;
        if (situacao == Situacao.SEM_GIRO) {
            capitalParado = p.custoUnitario().multiply(BigDecimal.valueOf(p.estoqueAtual()));
        } else if (situacao == Situacao.EXCESSO) {
            int necessario = (int) Math.ceil(media * parametros.diasExcesso());
            capitalParado = p.custoUnitario().multiply(BigDecimal.valueOf(p.estoqueAtual() - necessario));
        }

        return new Sugestao(p, media, desvio, estoqueSeguranca, pontoPedido, cobertura, comprar,
                p.custoUnitario().multiply(BigDecimal.valueOf(comprar)), valorVendido, classe, situacao, capitalParado);
    }

    private Situacao classificar(Produto p, double media, int pontoPedido, double cobertura) {
        if (media == 0) {
            return p.estoqueAtual() > 0 ? Situacao.SEM_GIRO : Situacao.OK;
        }
        if (p.estoqueAtual() == 0) {
            return Situacao.RUPTURA;
        }
        if (p.estoqueAtual() <= pontoPedido) {
            return Situacao.COMPRAR;
        }
        if (cobertura > parametros.diasExcesso()) {
            return Situacao.EXCESSO;
        }
        if (p.estoqueAtual() <= pontoPedido + media * DIAS_ATENCAO) {
            return Situacao.ATENCAO;
        }
        return Situacao.OK;
    }

    /** 25 unidades com lote de 12 viram 36 (3 caixas). */
    static int arredondarParaLote(int quantidade, int lote) {
        if (quantidade <= 0) {
            return 0;
        }
        return ((quantidade + lote - 1) / lote) * lote;
    }
}
