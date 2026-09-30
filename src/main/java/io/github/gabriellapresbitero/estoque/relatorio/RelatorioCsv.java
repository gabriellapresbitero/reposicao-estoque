package io.github.gabriellapresbitero.estoque.relatorio;

import io.github.gabriellapresbitero.estoque.Formatos;
import io.github.gabriellapresbitero.estoque.modelo.Sugestao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** CSV com a análise de todos os produtos, pronto para Excel ou Power BI. */
public class RelatorioCsv {

    static final String CABECALHO = "sku;nome;classe_abc;situacao;estoque_atual;demanda_media_dia;desvio_padrao;"
            + "lead_time_dias;estoque_seguranca;ponto_pedido;cobertura_dias;comprar;valor_compra;capital_parado";

    public List<String> linhas(List<Sugestao> sugestoes) {
        List<String> linhas = new ArrayList<>();
        linhas.add(CABECALHO);
        for (Sugestao s : sugestoes) {
            linhas.add(String.join(";",
                    s.produto().sku(),
                    texto(s.produto().nome()),
                    s.classe().name(),
                    s.situacao().name(),
                    String.valueOf(s.produto().estoqueAtual()),
                    decimal(s.demandaMedia()),
                    decimal(s.desvioPadrao()),
                    String.valueOf(s.produto().leadTimeDias()),
                    String.valueOf(s.estoqueSeguranca()),
                    String.valueOf(s.pontoPedido()),
                    Double.isInfinite(s.coberturaDias()) ? "" : decimal(s.coberturaDias()),
                    String.valueOf(s.comprar()),
                    Formatos.numeroCsv(s.valorCompra()),
                    Formatos.numeroCsv(s.capitalParado())));
        }
        return linhas;
    }

    public void salvar(List<Sugestao> sugestoes, Path destino) throws IOException {
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        Files.writeString(destino, "﻿" + String.join("\r\n", linhas(sugestoes)) + "\r\n", StandardCharsets.UTF_8);
    }

    private static String decimal(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor).replace('.', ',');
    }

    private static String texto(String valor) {
        return valor.contains(";") || valor.contains("\"") ? "\"" + valor.replace("\"", "\"\"") + "\"" : valor;
    }
}
