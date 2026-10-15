package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de auditoria persistente (RF05).
 *
 * Cada evento guarda: usuario autenticado e perfil, data/hora, tipo de operacao,
 * entidade afetada e seu identificador, campos modificados (valor anterior/novo)
 * e justificativa quando exigida. Os registros sao imutaveis: nao ha operacoes
 * de alteracao/exclusao expostas. Senhas, tokens e credenciais nunca sao gravados.
 */
@Document(collection = "auditorias")
public class Auditoria {

    @Id
    private String id;

    private String usuario;          // username autenticado
    private String perfil;           // ADMIN / GERENTE / ATENDENTE
    private LocalDateTime dataHora;   // momento da operacao
    private String operacao;         // ex.: CRIACAO_PEDIDO, ALTERACAO_STATUS, DESCONTO, CANCELAMENTO, ALTERACAO_CLIENTE
    private String entidade;         // ex.: Pedido, Cliente
    private String entidadeId;       // identificador do registro afetado
    private String justificativa;    // quando exigida (ex.: cancelamento)
    private List<CampoAlterado> alteracoes = new ArrayList<>();

    public Auditoria() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getOperacao() {
        return operacao;
    }

    public void setOperacao(String operacao) {
        this.operacao = operacao;
    }

    public String getEntidade() {
        return entidade;
    }

    public void setEntidade(String entidade) {
        this.entidade = entidade;
    }

    public String getEntidadeId() {
        return entidadeId;
    }

    public void setEntidadeId(String entidadeId) {
        this.entidadeId = entidadeId;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public List<CampoAlterado> getAlteracoes() {
        return alteracoes;
    }

    public void setAlteracoes(List<CampoAlterado> alteracoes) {
        this.alteracoes = (alteracoes != null) ? alteracoes : new ArrayList<>();
    }

    /** Representa um campo modificado, com valor anterior e novo (RF05). */
    public static class CampoAlterado {
        private String campo;
        private String valorAnterior;
        private String valorNovo;

        public CampoAlterado() {
        }

        public CampoAlterado(String campo, String valorAnterior, String valorNovo) {
            this.campo = campo;
            this.valorAnterior = valorAnterior;
            this.valorNovo = valorNovo;
        }

        public String getCampo() {
            return campo;
        }

        public void setCampo(String campo) {
            this.campo = campo;
        }

        public String getValorAnterior() {
            return valorAnterior;
        }

        public void setValorAnterior(String valorAnterior) {
            this.valorAnterior = valorAnterior;
        }

        public String getValorNovo() {
            return valorNovo;
        }

        public void setValorNovo(String valorNovo) {
            this.valorNovo = valorNovo;
        }
    }
}
