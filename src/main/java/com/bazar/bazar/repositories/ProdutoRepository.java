package com.bazar.bazar.repositories;

import com.bazar.bazar.model.Categoria;
import com.bazar.bazar.model.Endereco;
import com.bazar.bazar.model.Produto;
import com.bazar.bazar.model.Usuario;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, UUID> {
    List<Produto> findByNomeContainingIgnoreCase(String nome);
    List<Produto> findByPrecoLessThan(double preco);
    List<Produto> findByQuantidadeGreaterThan(int quantidade);
    //Usuario findByEmail(String email);
    List<Produto> findByAutorId(UUID autorId);
    Page<Produto> findByCategoriaId(UUID categoriaId, Pageable pageable);



    @Query("SELECT DISTINCT p.categoria FROM Produto p WHERE p.categoria IS NOT NULL")
    List<Categoria> findDistinctCategoriasByProduto();


    @Modifying
    @Transactional
    @Query("UPDATE Produto p SET p.icone = :iconeUrl WHERE p.id = :id")
    void updateIconeUrlById(@Param("id") UUID id, @Param("iconeUrl") String iconeUrl);

}