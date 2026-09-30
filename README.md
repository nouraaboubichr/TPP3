# Gestion Réservations — TP JPA/Hibernate (Relations & Cascade)

Projet Maven de démonstration JPA/Hibernate avec base H2 en mémoire.
Il modélise un système de réservation de salles reliant 4 entités (`Utilisateur`, `Salle`, `Reservation`, `Equipement`) et illustre les relations JPA (`OneToMany`, `ManyToOne`, `ManyToMany`), les opérations en cascade et la suppression orpheline (`orphanRemoval`).

## Objectifs du TP

- Créer les entités `Salle`, `Reservation` et `Utilisateur` avec leurs relations
- Implémenter une relation `ManyToMany` entre `Salle` et `Equipement`
- Configurer et tester différentes stratégies de cascade
- Expérimenter la suppression orpheline (`orphanRemoval`)

## Prérequis

- JDK 8 ou supérieur
- Maven 3.6+
- Un IDE (IntelliJ IDEA, Eclipse, NetBeans...)

## Structure du projet

```
gestion-reservations/
├── pom.xml
└── src/
    └── main/
        ├── java/com/example/
        │   ├── App.java                   # Classe principale (tests relations/cascade)
        │   └── model/
        │       ├── Utilisateur.java       # Entité : 1 utilisateur → N réservations
        │       ├── Salle.java             # Entité : 1 salle → N réservations, N↔N équipements
        │       ├── Reservation.java       # Entité : N réservations → 1 utilisateur, 1 salle
        │       └── Equipement.java        # Entité : N↔N avec Salle
        └── resources/
            └── META-INF/
                └── persistence.xml        # Configuration JPA / Hibernate / H2
```

## Stack technique

| Composant | Version |
|---|---|
| JPA API (javax.persistence) | 2.2 |
| Hibernate Core | 5.6.5.Final |
| Hibernate Validator | 6.2.0.Final |
| Jakarta EL (requis par Hibernate Validator) | 3.0.4 |
| Base de données H2 (en mémoire) | 2.1.214 |
| SLF4J (logs) | 1.7.36 |
| JUnit | 4.13.2 |

## Configuration (persistence.xml)

- **Base** : H2 en mémoire (`jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1`)
- **Utilisateur / mot de passe** : `sa` / *(vide)*
- **hibernate.hbm2ddl.auto = create-drop** : le schéma (et les tables de jointure) est créé au démarrage et supprimé à l'arrêt
- **hibernate.show_sql = true** : affiche les requêtes SQL générées dans la console
- Les 4 entités sont déclarées explicitement via `<class>` dans `persistence.xml`

## Modèle de données et relations

```
Utilisateur (1) ─────────< (N) Reservation (N) >───────── (1) Salle
                                                                │
                                                                │ (N)
                                                            ManyToMany
                                                                │
                                                                (N)
                                                           Equipement
```

### Utilisateur
| Champ | Type | Description |
|---|---|---|
| id | Long | clé primaire (IDENTITY) |
| nom, prenom | String | obligatoires |
| email | String | obligatoire, unique, format valide |
| reservations | List\<Reservation\> | `@OneToMany(mappedBy="utilisateur", cascade=ALL, orphanRemoval=true)` |

### Salle
| Champ | Type | Description |
|---|---|---|
| id | Long | clé primaire (IDENTITY) |
| nom | String | obligatoire |
| capacite | Integer | obligatoire, ≥ 1 |
| description | String | max 500 caractères |
| reservations | List\<Reservation\> | `@OneToMany(mappedBy="salle", cascade=ALL)` |
| equipements | Set\<Equipement\> | `@ManyToMany` via table de jointure `salle_equipement` |

### Reservation
| Champ | Type | Description |
|---|---|---|
| id | Long | clé primaire (IDENTITY) |
| dateDebut, dateFin | LocalDateTime | obligatoires |
| motif | String | max 500 caractères |
| utilisateur | Utilisateur | `@ManyToOne(fetch=LAZY)` |
| salle | Salle | `@ManyToOne(fetch=LAZY)` |

### Equipement
| Champ | Type | Description |
|---|---|---|
| id | Long | clé primaire (IDENTITY) |
| nom | String | obligatoire |
| description | String | max 500 caractères |
| salles | Set\<Salle\> | `@ManyToMany(mappedBy="equipements")` (côté inverse) |

## Installation et exécution

### 1. Récupérer les dépendances

```bash
mvn clean install
```

Dans IntelliJ : clic droit sur `pom.xml` → **Maven → Reload project**.

### 2. Lancer l'application

```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.App"
```

Ou directement dans l'IDE : clic droit sur `App.java` → **Run**.

## Ce que fait `App.java`

Le programme exécute 3 scénarios de test successifs :

**1. Relations et cascade (`testRelationsEtCascade`)**
Crée un `Utilisateur`, une `Salle` et une `Reservation` liés entre eux, puis persiste uniquement l'utilisateur et la salle. Grâce à `cascade = CascadeType.ALL`, la réservation est automatiquement enregistrée sans appel `persist()` explicite.

**2. Suppression orpheline (`testSuppressionOrpheline`)**
Crée un utilisateur avec deux réservations, puis retire une réservation de sa liste (`removeReservation`). Grâce à `orphanRemoval = true`, cette réservation est automatiquement supprimée de la base — vérifié en confirmant qu'elle n'existe plus via `em.find()`.

**3. Relation ManyToMany (`testRelationManyToMany`)**
Crée des équipements et des salles, les associe via `addEquipement`, puis vérifie la relation dans les deux sens (salle → équipements, équipement → salles). Teste aussi le retrait d'un équipement d'une salle **sans** supprimer l'équipement lui-même (pas d'`orphanRemoval` sur une `ManyToMany`).

## Concepts clés illustrés

### 1. Relations bidirectionnelles
Chaque relation a un côté **propriétaire** (celui qui porte `@JoinColumn` ou `@JoinTable`) et un côté **inverse** (`mappedBy`). Les deux côtés doivent être synchronisés manuellement en mémoire — d'où les méthodes utilitaires `addReservation`/`removeReservation` et `addEquipement`/`removeEquipement`, qui mettent à jour les deux collections en même temps.

### 2. Cascade
| Type | Effet |
|---|---|
| `CascadeType.ALL` | propage persist, merge, remove, refresh, detach (utilisé Utilisateur → Reservation) |
| `CascadeType.PERSIST, MERGE` | propage seulement la création/mise à jour (utilisé Salle → Equipement, car on ne veut pas supprimer un équipement partagé par une autre salle) |

### 3. orphanRemoval
Utilisé uniquement sur `Utilisateur.reservations`. Quand une réservation est retirée de la liste d'un utilisateur, elle est supprimée de la base — car une réservation n'a de sens qu'attachée à un utilisateur. **Volontairement absent** sur la relation `Salle ↔ Equipement`, car un équipement retiré d'une salle doit continuer à exister (il peut équiper d'autres salles).

## Résultat attendu (extrait console)

<img width="1270" height="674" alt="1" src="image/Capture d'écran 2026-09-30 000924.png" />

<img width="1270" height="674" alt="1" src="image/Capture d'écran 2026-09-30 000934.png" />

<img width="1270" height="674" alt="1" src="image/Capture d'écran 2026-09-30 000950.png" />

<img width="1270" height="674" alt="1" src="image/Capture d'écran 2026-09-30 000958.png" />
