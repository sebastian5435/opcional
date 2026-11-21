package co.edu.tdea.superventas.repository;

import co.edu.tdea.superventas.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
