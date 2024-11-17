package br.com.alura.screematch.service;

public interface IConverteDados {
    <T> T obterDados(String JSON, Class<T> classe);
}
