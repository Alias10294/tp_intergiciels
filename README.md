# TP Intergiciels

## Equipe
22201911: KERBELLEC **Alphonse**, alphonse.kerbellec@uphf.fr
<br>
22101912: VERZELE **Florian**, florian.verzele@uphf.fr
<br>

## Utilisation
Pour lancer l'application, il suffit de suivre ces commandes:
```text
> cd [dossier-au-choix]
> git clone https://github.com/Alias10294/tp_intergiciels.git
> docker compose up --build -d
```
Une fois les conteneurs mis en place, pour lancer un client de messagerie, il suffit d'utiliser dans un terminal à la racine du projet:
- Sur Windows
```text
> .\start_chatting.bat [pseudo-au-choix] [groupe-au-choix](optionnel)
```
- Sur Linux/MacOS
```text
> ./start_chatting.sh [pseudo-au-choix] [group-au-choix](optionnel)
```

## Description
Ce projet met en place une messagerie utilisée via le terminal, offrant une fonctionnalité de traduction automatique anglais -> français pour ses utilisateurs.

Basé sur *Docker* ainsi que *Kafka* afin de prodiguer un service à la fois rapide et portable, le projet est composé de son application de messagerie via le terminal, d'un service de logging en connexion avec *Kafka* et une base de données *PostgreSQL*, et d'un service de traduction automatique utilisant un serveur *LibreTranslate* hosté en local.

## Architecture
L'architecture retenue repose sur une séparation claire entre les clients, le bus de messages, les services applicatifs et les services d'infrastructure.

Le client fourni est une application *Spring Boot* Shell permettant d'interagir avec la messagerie depuis un terminal. Chaque client est à la fois producteur et consommateur *Kafka*. Il publie ses messages applicatifs vers le topic `topicout`, lit les messages reçus depuis `topicin`, envoie ses messages techniques vers `topictechout`, et reçoit les réponses techniques depuis `topictechin`.

Les échanges sont centralisés autour d'un broker Apache *Kafka*. *Kafka* permet de découpler les clients des services : un client n'appelle jamais directement le service de traduction ni la base de données. Il se contente de publier des messages dans *Kafka*, puis les services spécialisés les consomment et produisent de nouveaux messages si nécessaire.

L'architecture se compose donc des éléments suivants :

```mermaid
%%{init: {"flowchart": {"curve": "bumpX"}} }%%
flowchart LR
    subgraph Clients["Clients"]
        direction TB
        ClientA["Client A<br/>Spring Boot Shell"]
        ClientB["Client B<br/>Spring Boot Shell"]
    end

    *Kafka*[(Apache Kafka<br/>topicin / topictechin<br/>topicout / topictechout)]

    subgraph Services["Services Spring Boot"]
        direction TB
        Translate["per-translate-service"]
        ConsDB["client-cons-db"]
    end

    subgraph Infra["Infrastructure"]
        direction TB
        Libre["LibreTranslate"]
        DB[(PostgreSQL)]
    end

    ClientA -->|"topicout / topictechout"| *Kafka*
    ClientB -->|"topicout / topictechout"| *Kafka*

    *Kafka* -->|"topicin / topictechin"| ClientA
    *Kafka* -->|"topicin / topictechin"| ClientB

    *Kafka* -->|"lecture topicout<br/>messages à traduire"| Translate
    Translate -->|"HTTP"| Libre
    Libre -->|"JSON"| Translate
    Translate -->|"publication topicin<br/>messages traduits"| *Kafka*

    *Kafka* -->|"lecture topictechout<br/>commandes clients"| ConsDB
    *Kafka* -->|"lecture topicout / topicin<br/>archivage messages"| ConsDB
    ConsDB -->|"publication topictechin<br/>réponses techniques"| *Kafka*
    ConsDB <-->|"clients + archives"| DB
```

## Rôles des topics *Kafka*
Quatre topics *Kafka* sont utilisés:

| Topic | Producteurs | Consommateurs | Rôle |
|---|---|---|---|
| `topicout` | Clients Shell | `per-translate-service`, `client-cons-db` | Messages applicatifs envoyés par les clients |
| `topicin` | `per-translate-service` | Clients Shell, `client-cons-db` | Messages traduits à destination des clients |
| `topictechout` | Clients Shell | `client-cons-db` | Messages techniques : connexion, déconnexion, demande de liste |
| `topictechin` | `client-cons-db` | Clients Shell | Réponses techniques envoyées aux clients |

Le choix de séparer les messages applicatifs et les messages techniques permet d'éviter de mélanger les conversations utilisateur avec les commandes internes du système. Les messages applicatifs concernent la discussion entre clients, tandis que les messages techniques concernent l'état du système : connexion d'un client, déconnexion, demande de liste des utilisateurs connectés ou vérification de disponibilité d'un destinataire.

## Service de traduction : `per-translate-service`
Le service `per-translate-service` est une application *Spring Boot* indépendante. Son rôle est limité à la traduction des messages applicatifs.

Son fonctionnement est le suivant :
1. le service écoute le topic *Kafka* `topicout` ;
2. lorsqu'un message est reçu, il extrait l'expéditeur, le destinataire et le contenu du message ;
3. il envoie le contenu textuel à *LibreTranslate* via une requête HTTP vers `/translate` ;
4. il récupère le champ `translatedText` de la réponse ;
5. il reconstruit un message au format attendu par le client ;
6. il publie le message traduit dans le topic `topicin`.

Le service ne communique pas directement avec *PostgreSQL*. Cette séparation permet de conserver une responsabilité unique : `per-translate-service` traduit, tandis que `client-cons-db` archive les messages et gère l'état des clients.

## Service d'archivage et de gestion des clients : `client-cons-db`
Le service `client-cons-db` est responsable de la gestion des clients connectés et de l'archivage des échanges transitant par la messagerie.

Il traite deux types d'informations :
- les messages techniques venant de `topictechout` ;
- les messages applicatifs venant de `topicout` et `topicin`.

Lorsqu'un client se connecte, il envoie un message du type :
```text
CONNECT:ClientA
```

Le service enregistre alors le client en base ou met à jour son statut à connecté. Lorsqu'un client quitte l'application avec la commande byebye, le client envoie :
```text
DISCONNECT:ClientA
```
Le service met alors à jour son statut à déconnecté.

Le service répond également aux demandes de liste des clients connectés :
```text
GET:ClientA
```
Dans ce cas, il interroge *PostgreSQL* et renvoie la liste des clients disponibles via `topictechin`.

Enfin, `client-cons-db` archive les messages applicatifs. Les messages lus depuis `topicout` correspondent aux messages originaux envoyés par les clients. Les messages lus depuis `topicin` correspondent aux messages traduits renvoyés aux destinataires.

## Déploiement *Docker*
Le projet utilise *Docker* Compose afin de lancer les services d'infrastructure et les services applicatifs.

Les principaux conteneurs sont :
| Conteneur | Rôle |
|---|---|
| `*Kafka*` | Broker de messages utilisé comme bus d'échange |
| `postgres-db` | Base *PostgreSQL* pour l'archivage et l'état des clients |
| `libretranslate` | Service local de traduction automatique |
| `per-translate-service` | Service *Spring Boot* chargé de traduire les messages |
| `client-cons-db` | Service *Spring Boot* chargé de la base de données et des messages techniques |

*Kafka* est configuré avec des topics auto-créés afin que les topics nécessaires soient disponibles au moment où les clients et services commencent à publier. *PostgreSQL* est initialisé à partir du script SQL placé dans le projet. *LibreTranslate* est lancé localement dans un conteneur afin d'éviter de dépendre d'une API externe.

Le client Shell n'est pas nécessairement conteneurisé, car il s'agit d'une application interactive en terminal. Il est lancé localement via les scripts fournis :
```text
> .\start_chatting.bat [pseudo-au-choix] [groupe-au-choix](optionnel)
> ./start_chatting.sh [pseudo-au-choix] [groupe-au-choix](optionnel)
```
Exemples:
```text
> .\start_chatting.bat ClientA 
> ./start_chatting.sh ClientA ClientA_Group
```

## Conclusion

Ce projet aboutit à une messagerie distribuée fonctionnelle reposant sur *Spring Boot*, *Kafka*, *PostgreSQL* et *LibreTranslate*. Les clients communiquent via *Kafka*, les messages sont traités par des services séparés, traduits automatiquement, puis transmis au destinataire tout en pouvant être archivés.

Ce TP a permis de mettre en pratique plusieurs notions centrales d'intergiciels : communication asynchrone, découplage entre composants, architecture orientée services, persistance des données et déploiement avec *Docker*. Il illustre ainsi l'intérêt d'une architecture modulaire, où chaque service possède une responsabilité claire.
<br>
L'application fournit une base cohérente pour comprendre le fonctionnement d'un système distribué extensible.