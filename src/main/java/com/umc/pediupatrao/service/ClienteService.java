package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Auditoria.CampoAlterado;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Service
public class ClienteService {

    private static final String ENTIDADE = "Cliente";

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Cliente novoCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        clienteRepository.deleteById(id);
        auditoriaService.registrar("EXCLUSAO_CLIENTE", ENTIDADE, id, null);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    /**
     * Cria ou atualiza o cliente. Em atualizacoes, compara o estado anterior
     * com o novo e audita os campos alterados (RF05), com destaque para
     * endereco e telefone.
     */
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null) {
            Cliente salvo = clienteRepository.save(cliente);
            auditoriaService.registrar("CRIACAO_CLIENTE", ENTIDADE, salvo.getId(), null);
            return salvo;
        }

        Cliente anterior = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado para atualização."));

        List<CampoAlterado> alteracoes = diff(anterior, cliente);
        Cliente salvo = clienteRepository.save(cliente);

        if (!alteracoes.isEmpty()) {
            auditoriaService.registrarAlteracoes(
                    "ALTERACAO_CLIENTE", ENTIDADE, salvo.getId(), null, alteracoes);
        }
        return salvo;
    }

    /** Compara campos cadastrais relevantes e monta a lista de alteracoes (RF05). */
    private List<CampoAlterado> diff(Cliente antigo, Cliente novo) {
        List<CampoAlterado> alteracoes = new ArrayList<>();
        comparar(alteracoes, "nome", antigo, novo, Cliente::getNome);
        comparar(alteracoes, "telefone", antigo, novo, Cliente::getTelefone);
        comparar(alteracoes, "cep", antigo, novo, Cliente::getCep);
        comparar(alteracoes, "logradouro", antigo, novo, Cliente::getLogradouro);
        comparar(alteracoes, "numero", antigo, novo, Cliente::getNumero);
        comparar(alteracoes, "complemento", antigo, novo, Cliente::getComplemento);
        comparar(alteracoes, "bairro", antigo, novo, Cliente::getBairro);
        comparar(alteracoes, "cidade", antigo, novo, Cliente::getCidade);
        comparar(alteracoes, "estado", antigo, novo, Cliente::getEstado);
        return alteracoes;
    }

    private void comparar(List<CampoAlterado> alteracoes, String campo,
                          Cliente antigo, Cliente novo, Function<Cliente, String> getter) {
        String valorAntigo = getter.apply(antigo);
        String valorNovo = getter.apply(novo);
        if (!Objects.equals(valorAntigo, valorNovo)) {
            alteracoes.add(new CampoAlterado(campo, valorAntigo, valorNovo));
        }
    }
}
