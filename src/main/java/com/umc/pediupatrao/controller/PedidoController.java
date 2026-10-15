package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<Pedido> criarPedido(@RequestBody Pedido pedido) {
        // Valores (total/desconto) sao ignorados do request e calculados no servidor
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(pedido));
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> buscarPorId(@PathVariable String id) {
        return pedidoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam String status) {
        return pedidoService.atualizarStatus(id, converterStatus(status));
    }

    /** RF04 - desconto exclusivo do GERENTE (garantido no service via @PreAuthorize). */
    @PutMapping("/{id}/desconto")
    public Pedido aplicarDesconto(@PathVariable String id, @RequestParam double percentual) {
        return pedidoService.aplicarDesconto(id, percentual);
    }

    /** RF04 - cancelamento exclusivo do GERENTE, exige justificativa. */
    @PutMapping("/{id}/cancelar")
    public Pedido cancelarPedido(@PathVariable String id, @RequestBody Map<String, String> body) {
        String justificativa = (body != null) ? body.get("justificativa") : null;
        return pedidoService.cancelarPedido(id, justificativa);
    }

    private StatusPedido converterStatus(String status) {
        try {
            return StatusPedido.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido: " + status);
        }
    }
}
