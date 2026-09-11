package com.wellpag.pagamento.repository;

import com.wellpag.pagamento.model.Usuario;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Repositorio read-only temporario sobre a collection "usuarios" (ver Usuario.java).
 * Estende Repository (interface marcadora, sem CRUD) em vez de MongoRepository
 * para garantir em nivel de tipo que nenhum metodo de escrita (save/delete) fica
 * disponivel por acidente. Expoe apenas o metodo usado por BancoInterService.
 */
public interface UsuarioRepository extends Repository<Usuario, String> {
    Optional<Usuario> findById(String id);
}
