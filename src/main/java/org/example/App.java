package org.example;



import org.example.model.Equipement;
import org.example.model.Reservation;
import org.example.model.Salle;
import org.example.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;

public class App {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-reservations");

        try {
            System.out.println("\n=== Test des relations et des opérations en cascade ===");
            testRelationsEtCascade(emf);

            System.out.println("\n=== Test de la suppression orpheline ===");
            testSuppressionOrpheline(emf);

            System.out.println("\n=== Test de la relation ManyToMany avec Équipement ===");
            testRelationManyToMany(emf);

        } finally {
            emf.close();
        }
    }

    private static void testRelationsEtCascade(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            System.out.println("Création des entités...");

            Utilisateur utilisateur = new Utilisateur("Dupont", "Jean", "jean.dupont@example.com");

            Salle salle = new Salle("Salle A101", 30);
            salle.setDescription("Salle de réunion équipée d'un projecteur");

            Reservation reservation = new Reservation(
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(1).plusHours(2),
                    "Réunion d'équipe"
            );

            utilisateur.addReservation(reservation);
            salle.addReservation(reservation);

            em.persist(utilisateur);
            em.persist(salle);

            em.getTransaction().commit();
            System.out.println("Entités créées et liées avec succès !");

            em.clear();

            System.out.println("\nVérification des entités persistées :");
            Utilisateur utilisateurPersiste = em.find(Utilisateur.class, utilisateur.getId());
            System.out.println("Utilisateur : " + utilisateurPersiste);
            System.out.println("Nombre de réservations : " + utilisateurPersiste.getReservations().size());

            Salle sallePersistee = em.find(Salle.class, salle.getId());
            System.out.println("Salle : " + sallePersistee);
            System.out.println("Nombre de réservations : " + sallePersistee.getReservations().size());

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private static void testSuppressionOrpheline(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Utilisateur utilisateur = new Utilisateur("Martin", "Sophie", "sophie.martin@example.com");

            Salle salle1 = new Salle("Salle B102", 20);
            em.persist(salle1);

            Salle salle2 = new Salle("Salle C103", 15);
            em.persist(salle2);

            Reservation reservation1 = new Reservation(
                    LocalDateTime.now().plusDays(2),
                    LocalDateTime.now().plusDays(2).plusHours(1),
                    "Entretien"
            );

            Reservation reservation2 = new Reservation(
                    LocalDateTime.now().plusDays(3),
                    LocalDateTime.now().plusDays(3).plusHours(2),
                    "Formation"
            );

            utilisateur.addReservation(reservation1);
            utilisateur.addReservation(reservation2);
            salle1.addReservation(reservation1);
            salle2.addReservation(reservation2);

            em.persist(utilisateur);

            em.getTransaction().commit();
            System.out.println("Utilisateur avec deux réservations créé !");

            em.getTransaction().begin();

            Utilisateur utilisateurAModifier = em.find(Utilisateur.class, utilisateur.getId());
            System.out.println("Nombre de réservations avant suppression : " + utilisateurAModifier.getReservations().size());

            Reservation reservationASupprimer = utilisateurAModifier.getReservations().get(0);
            utilisateurAModifier.removeReservation(reservationASupprimer);

            em.getTransaction().commit();

            em.clear();
            Utilisateur utilisateurApresModification = em.find(Utilisateur.class, utilisateur.getId());
            System.out.println("Nombre de réservations après suppression : " + utilisateurApresModification.getReservations().size());

            Long reservationId = reservationASupprimer.getId();
            Reservation reservationSupprimee = em.find(Reservation.class, reservationId);
            System.out.println("La réservation existe-t-elle encore ? " + (reservationSupprimee != null));

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    private static void testRelationManyToMany(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Equipement projecteur = new Equipement("Projecteur", "Projecteur HD");
            Equipement ecran = new Equipement("Écran interactif", "Écran tactile 65 pouces");
            Equipement visioconference = new Equipement("Système de visioconférence", "Système complet avec caméra HD");

            Salle salleReunion = new Salle("Salle de réunion D104", 25);
            Salle salleFormation = new Salle("Salle de formation E205", 40);

            salleReunion.addEquipement(projecteur);
            salleReunion.addEquipement(visioconference);

            salleFormation.addEquipement(projecteur);
            salleFormation.addEquipement(ecran);

            em.persist(salleReunion);
            em.persist(salleFormation);

            em.getTransaction().commit();
            System.out.println("Salles et équipements créés avec succès !");

            em.clear();

            System.out.println("\nVérification des relations ManyToMany :");

            Salle salleReunionPersistee = em.find(Salle.class, salleReunion.getId());
            System.out.println("Salle : " + salleReunionPersistee.getNom());
            System.out.println("Équipements :");
            for (Equipement equipement : salleReunionPersistee.getEquipements()) {
                System.out.println("- " + equipement.getNom());
            }

            Salle salleFormationPersistee = em.find(Salle.class, salleFormation.getId());
            System.out.println("\nSalle : " + salleFormationPersistee.getNom());
            System.out.println("Équipements :");
            for (Equipement equipement : salleFormationPersistee.getEquipements()) {
                System.out.println("- " + equipement.getNom());
            }

            Equipement projecteurPersiste = em.createQuery(
                            "SELECT e FROM Equipement e WHERE e.nom = :nom", Equipement.class)
                    .setParameter("nom", "Projecteur")
                    .getSingleResult();

            System.out.println("\nÉquipement : " + projecteurPersiste.getNom());
            System.out.println("Salles équipées :");
            for (Salle salle : projecteurPersiste.getSalles()) {
                System.out.println("- " + salle.getNom());
            }

            em.getTransaction().begin();

            salleReunionPersistee.removeEquipement(projecteurPersiste);

            em.getTransaction().commit();

            em.clear();

            Salle salleApresModification = em.find(Salle.class, salleReunion.getId());
            System.out.println("\nSalle après suppression d'un équipement : " + salleApresModification.getNom());
            System.out.println("Équipements restants :");
            for (Equipement equipement : salleApresModification.getEquipements()) {
                System.out.println("- " + equipement.getNom());
            }

            Equipement projecteurApresModification = em.find(Equipement.class, projecteurPersiste.getId());
            System.out.println("\nL'équipement existe-t-il encore ? " + (projecteurApresModification != null));

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}