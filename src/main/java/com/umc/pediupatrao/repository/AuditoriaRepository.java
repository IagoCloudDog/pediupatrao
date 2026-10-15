package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Auditoria;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends MongoRepository<Auditoria, String> {

    List<Auditoria> findByEntidadeAndEntidadeId(String entidade, String entidadeId, Sort sort);
}
