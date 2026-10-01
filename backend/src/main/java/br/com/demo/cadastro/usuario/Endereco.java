package br.com.demo.cadastro.usuario;

import jakarta.persistence.Embeddable;

@Embeddable
public class Endereco {

    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;

    protected Endereco() {}

    Endereco(String cep, String logradouro, String numero, String complemento,
             String bairro, String cidade, String uf) {
        this.cep = cep;
        this.logradouro = logradouro.trim();
        this.numero = numero.trim();
        this.complemento = complemento == null || complemento.isBlank() ? null : complemento.trim();
        this.bairro = bairro.trim();
        this.cidade = cidade.trim();
        this.uf = uf;
    }

    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getUf() { return uf; }
}
