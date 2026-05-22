package com.tp.client_cons_db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "utilisateurs_connectes")
public class UtilisateurConnecte {

    @Id
    private String username;

    @Column(name = "est_connecte")
    private boolean estConnecte;

    public UtilisateurConnecte() {}

    public UtilisateurConnecte(String username, boolean estConnecte) {
        this.username = username;
        this.estConnecte = estConnecte;
    }

    // Getters et Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isEstConnecte() {
        return estConnecte;
    }

    public void setEstConnecte(boolean estConnecte) {
        this.estConnecte = estConnecte;
    }
}