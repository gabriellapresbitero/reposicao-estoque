package io.github.gabriellapresbitero.estoque.leitura;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Leitura de CSV separado por ";" (padrão do Excel em português), com cabeçalho.
 * Cada linha vira um objeto por meio de uma função de conversão, e qualquer erro
 * informa o arquivo e a linha.
 */
public final class Csv {

    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT));

    private Csv() {
    }

    /** Uma linha do CSV, acessada pelo nome da coluna. */
    public record Linha(Map<String, String> campos) {
        public String texto(String coluna) {
            return campos.getOrDefault(coluna, "").strip();
        }

        public int inteiro(String coluna) {
            String valor = texto(coluna);
            try {
                return Integer.parseInt(valor.replace(".", ""));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\"" + coluna + "\" não é um número inteiro: \"" + valor + "\"");
            }
        }

        public int inteiro(String coluna, int padrao) {
            return texto(coluna).isEmpty() ? padrao : inteiro(coluna);
        }

        /** Aceita "1.234,56", "1234,56" e "1234.56". */
        public BigDecimal dinheiro(String coluna) {
            String valor = texto(coluna).replace("R$", "").replace(" ", "");
            if (valor.contains(",")) {
                valor = valor.replace(".", "").replace(",", ".");
            }
            try {
                return new BigDecimal(valor);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("\"" + coluna + "\" não é um valor válido: \"" + texto(coluna) + "\"");
            }
        }

        public LocalDate data(String coluna) {
            String valor = texto(coluna);
            for (DateTimeFormatter formato : FORMATOS_DATA) {
                try {
                    return LocalDate.parse(valor, formato);
                } catch (DateTimeParseException ignorada) {
                    // tenta o próximo
                }
            }
            throw new IllegalArgumentException("\"" + coluna + "\" não é uma data válida: \"" + valor + "\"");
        }
    }

    public static <T> List<T> ler(Path arquivo, List<String> obrigatorias, Function<Linha, T> conversor) throws IOException {
        return ler(Files.readString(arquivo, StandardCharsets.UTF_8), arquivo.getFileName().toString(), obrigatorias, conversor);
    }

    public static <T> List<T> ler(String conteudo, String nomeArquivo, List<String> obrigatorias, Function<Linha, T> conversor) {
        String[] linhas = conteudo.replace("﻿", "").split("\\R");
        if (linhas.length == 0 || linhas[0].isBlank()) {
            throw new IllegalArgumentException(nomeArquivo + ": arquivo vazio");
        }
        List<String> cabecalho = dividir(linhas[0]).stream().map(Csv::normalizarNome).toList();
        for (String coluna : obrigatorias) {
            if (!cabecalho.contains(coluna)) {
                throw new IllegalArgumentException(nomeArquivo + ": falta a coluna \"" + coluna + "\" (encontradas: " + cabecalho + ")");
            }
        }

        List<T> resultado = new ArrayList<>();
        for (int i = 1; i < linhas.length; i++) {
            if (linhas[i].isBlank()) {
                continue;
            }
            List<String> valores = dividir(linhas[i]);
            Map<String, String> campos = new HashMap<>();
            for (int c = 0; c < cabecalho.size() && c < valores.size(); c++) {
                campos.put(cabecalho.get(c), valores.get(c));
            }
            try {
                resultado.add(conversor.apply(new Linha(campos)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(nomeArquivo + ", linha " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return resultado;
    }

    /** "Lead Time (dias)" -> "lead_time_dias"; "Descrição" -> "descricao". */
    static String normalizarNome(String nome) {
        String semAcento = Normalizer.normalize(nome.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    /** Divide pelo ";", respeitando campos entre aspas. */
    static List<String> dividir(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean dentroDeAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '"') {
                if (dentroDeAspas && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else {
                    dentroDeAspas = !dentroDeAspas;
                }
            } else if (c == ';' && !dentroDeAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }
}
