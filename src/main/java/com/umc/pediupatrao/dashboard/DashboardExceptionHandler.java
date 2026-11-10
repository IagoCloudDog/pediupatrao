package com.umc.pediupatrao.dashboard;

import com.umc.pediupatrao.dashboard.DashboardDtos.ErroDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

/**
 * Tratamento de erros da API do dashboard (RF07): sempre JSON padronizado, sem stacktrace.
 * Restrito ao {@link DashboardRestController} para nao alterar o comportamento das demais telas.
 */
@RestControllerAdvice(assignableTypes = DashboardRestController.class)
public class DashboardExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DashboardExceptionHandler.class);

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroDTO> dadosInvalidos(DadosInvalidosException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    /** Ex.: inicio=31-12-2026 ou limite=abc (tipo/formato incorreto). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroDTO> tipoInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        String esperado = (e.getRequiredType() != null) ? e.getRequiredType().getSimpleName() : "valor valido";
        String msg = "Parametro '" + e.getName() + "' invalido: '" + e.getValue() + "'. Esperado: " + esperado
                + (e.getRequiredType() == java.time.LocalDate.class ? " no formato yyyy-MM-dd." : ".");
        return resposta(HttpStatus.BAD_REQUEST, msg, req);
    }

    /** Acesso negado pelo @PreAuthorize do service: relanca como 403 JSON. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroDTO> acessoNegado(AccessDeniedException e, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado: perfil sem permissao para este recurso.", req);
    }

    /** Falhas inesperadas (ex.: MongoDB fora do ar): mensagem generica, detalhe apenas no log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroDTO> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado na API do dashboard ({})", req.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Nao foi possivel obter os dados do dashboard no momento. Tente novamente.", req);
    }

    private ResponseEntity<ErroDTO> resposta(HttpStatus status, String mensagem, HttpServletRequest req) {
        ErroDTO corpo = new ErroDTO(LocalDateTime.now().toString(), status.value(), status.getReasonPhrase(),
                mensagem, req.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
