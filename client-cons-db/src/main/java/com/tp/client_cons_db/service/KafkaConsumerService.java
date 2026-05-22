package com.tp.client_cons_db.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.tp.client_cons_db.model.MessageArchive;
import com.tp.client_cons_db.model.UtilisateurConnecte;
import com.tp.client_cons_db.repository.MessageArchiveRepository;
import com.tp.client_cons_db.repository.UtilisateurConnecteRepository;

@Service
public class KafkaConsumerService {

    @Autowired
    private MessageArchiveRepository messageRepo;

    @Autowired
    private UtilisateurConnecteRepository userRepo;

    // Ajout du producteur Kafka pour renvoyer des messages à la CLI
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 1. Écoute des messages sur topicin et topicout pour archivage
     */
    @KafkaListener(topics = {"topicin", "topicout"}, groupId = "db_archive_group")
    public void listenAndArchive(String messageBrut) {
        try {
            String[] parts = messageBrut.split("#");
            if (parts.length >= 3) {
                String expediteur = parts[0].replace("FROM:", "");
                String destinataire = parts[1].replace("TO:", "");
                String contenu = parts[2];

                MessageArchive archive = new MessageArchive(expediteur, destinataire, contenu);
                messageRepo.save(archive);
                System.out.println(" [Archivage] Message sauvegardé de " + expediteur + " vers " + destinataire);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing du message d'échange : " + messageBrut);
        }
    }

    /**
     * 2. Écoute du topic technique pour gérer les connexions, déconnexions et requêtes GET
     */
    @KafkaListener(topics = "topictechout", groupId = "db_tech_group")
    public void listenTechCommands(String commandeBrute) {
        try {
            System.out.println(" [Tech IN] Commande reçue : " + commandeBrute);

            // Scénario 1 : CONNECT:ClientA
            if (commandeBrute.startsWith("CONNECT:")) {
                String username = commandeBrute.replace("CONNECT:", "").trim();
                UtilisateurConnecte user = new UtilisateurConnecte(username, true);
                userRepo.save(user);
                System.out.println(" [Bdd State] " + username + " est marqué comme CONNECTÉ.");
            } 
            
            // Scénario 7 : DISCONNECT:ClientA (via byebye)
            else if (commandeBrute.startsWith("DISCONNECT:")) {
                String username = commandeBrute.replace("DISCONNECT:", "").trim();
                UtilisateurConnecte user = new UtilisateurConnecte(username, false);
                userRepo.save(user);
                System.out.println(" [Bdd State] " + username + " est marqué comme DÉCONNECTÉ.");
            }

            // Scénario 2 : GET:ClientA (Demande de la liste des clients)
            else if (commandeBrute.startsWith("GET:")) {
                String demandeur = commandeBrute.replace("GET:", "").trim();
                System.out.println(" [Tech GET] " + demandeur + " demande la liste des connectés.");

                // 1. Chercher les clients connectés en BDD
                List<UtilisateurConnecte> connectes = userRepo.findAll();
                
                // 2. Filtrer pour ne garder que ceux en ligne et joindre leurs noms
                String listeClients = connectes.stream()
                        .filter(UtilisateurConnecte::isEstConnecte)
                        .map(UtilisateurConnecte::getUsername)
                        .collect(Collectors.joining(", ")); // Exemple: "ClientA, ClientB"

                // 3. Réponse dans le format attendu par la CLI
                String reponse = "FROM:SERVEUR#TO:" + demandeur + "#\n" +
                                "===================================\n" +
                                "  LISTE DES CLIENTS CONNECTÉS      \n" +
                                "===================================\n" +
                                " -> " + listeClients + "\n" +
                                "===================================";

                // 4. Envoi de la réponse sur le topic technique de la CLI
                kafkaTemplate.send("topictechin", reponse);
                System.out.println(" [Tech OUT] Liste envoyée sur topictechin : " + reponse);
            }

            else if (commandeBrute.startsWith("ISCONNECTED:")) {
                // On extrait la partie après le préfixe (ex: "ClientA#ClientB") 
                String contenu = commandeBrute.replace("ISCONNECTED:", "");
                String[] clients = contenu.split("#");
                
                if (clients.length >= 2) {
                    String demandeur = clients[0].trim();
                    String cible = clients[1].trim(); 
                    
                    System.out.println(" [Tech ISCONNECTED] " + demandeur + " demande si " + cible + " est en ligne.");

                    // 1. Chercher la cible en BDD
                    java.util.Optional<UtilisateurConnecte> userCible = userRepo.findById(cible);
                    
                    // 2. Vérifier si l'utilisateur existe et s'il est marqué connecté
                    boolean estEnLigne = userCible.isPresent() && userCible.get().isEstConnecte();
                    
                    // 3. Formater la réponse pour la CLI
                    // La CLI utilise généralement le même format d'extraction technique
                    String reponse = "FROM:SERVEUR#TO:" + demandeur + "#\n" +
                                "===================================\n" +
                                "  STATUT DE CONNEXION DE " + cible + "  \n" +
                                "===================================\n" +
                                " -> " + (estEnLigne ? "EN LIGNE" : "DÉCONNECTÉ") + "\n" +
                                "===================================";

                    // 4. Envoi sur le topic d'écoute technique de la CLI 
                    kafkaTemplate.send("topictechin", demandeur, reponse);
                    System.out.println(" [Tech OUT] Statut envoyé sur topictechin : " + reponse);
                }
            }          
        } catch (Exception e) {
            System.err.println("Erreur de traitement sur topictechout : " + commandeBrute);
        }
    }
}