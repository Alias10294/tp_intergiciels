-- Création de la table pour l'archivage des messages
CREATE TABLE IF NOT EXISTS messages_archive (
    id SERIAL PRIMARY KEY,
    expediteur VARCHAR(255) NOT NULL,
    destinataire VARCHAR(255) NOT NULL,
    contenu TEXT,
    date_reception TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Création de la table pour l'état des clients
CREATE TABLE IF NOT EXISTS utilisateurs_connectes (
    username VARCHAR(255) PRIMARY KEY,
    est_connecte BOOLEAN DEFAULT TRUE
);