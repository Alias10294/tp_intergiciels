package com.tp.client_cons_db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tp.client_cons_db.model.UtilisateurConnecte;

@Repository
public interface UtilisateurConnecteRepository extends JpaRepository<UtilisateurConnecte, String> {
}