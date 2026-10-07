package org.example.bookverse.repository;

import org.example.bookverse.entity.EstanteUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EstanteUsuarioRepository extends JpaRepository<EstanteUsuario, UUID> {

	List<EstanteUsuario> findByUsuarioId(UUID usuarioId);
}
