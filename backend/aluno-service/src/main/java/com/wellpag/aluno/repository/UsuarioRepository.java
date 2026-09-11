package com.wellpag.aluno.repository;

import com.wellpag.aluno.model.Usuario;
import org.springframework.data.repository.Repository;

/**
 * Repositorio read-only temporario sobre a collection "usuarios" (ver Usuario.java).
 * Estende Repository (interface marcadora, sem CRUD) em vez de MongoRepository
 * para garantir em nivel de tipo que nenhum metodo de escrita (save/delete) fica
 * disponivel por acidente — auth-service e o dono real desta entidade.
 */
public interface UsuarioRepository extends Repository<Usuario, String> {
    boolean existsById(String id);
}
