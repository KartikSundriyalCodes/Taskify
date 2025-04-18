package com.taskify.dao;

import com.taskify.model.Task;
import com.taskify.model.Task.Priority;
import com.taskify.model.Task.Status;
import com.taskify.model.Task.Category;
import com.taskify.model.User;
import com.taskify.utils.HibernateUtil;
import jakarta.persistence.criteria.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.query.Query;

public class TaskDAO {

    // Create new task
    public Long createTask(Task task) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(task);
            transaction.commit();
            return task.getId();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return null;
        }
    }

    // Update existing task
    public boolean updateTask(Task task) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(task);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // Delete task
    public boolean deleteTask(Long taskId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Task task = session.get(Task.class, taskId);
            if (task != null) session.remove(task);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // Get single task by ID with relationships
    public Task getTaskById(Long taskId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "SELECT t FROM Task t " +
                "LEFT JOIN FETCH t.owner " +
                "LEFT JOIN FETCH t.assignedUser " +
                "WHERE t.id = :taskId", Task.class)
                .setParameter("taskId", taskId)
                .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Get all tasks for a user with filtering
    public List<Task> getTasksByUser(User user, Status status, Priority priority, 
                                    Category category, String searchQuery) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Task> cq = cb.createQuery(Task.class);
            Root<Task> task = cq.from(Task.class);
            
            // Add explicit fetch joins
            task.fetch("owner", JoinType.INNER);      // Always required
            task.fetch("assignedUser", JoinType.LEFT); // Optional relationship

            List<Predicate> predicates = new ArrayList<>();

            // Base predicate: task owner
            predicates.add(cb.equal(task.get("owner"), user));

            // Add filters
            if (status != null) {
                predicates.add(cb.equal(task.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(task.get("priority"), priority));
            }
            if (category != null) {
                predicates.add(cb.equal(task.get("category"), category));
            }
            if (searchQuery != null && !searchQuery.isEmpty()) {
                String likePattern = "%" + searchQuery.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(task.get("title")), likePattern),
                    cb.like(cb.lower(task.get("description")), likePattern),
                    cb.like(cb.lower(task.get("category").as(String.class)), likePattern)
                ));
            }

            cq.where(predicates.toArray(new Predicate[0]))
                .distinct(true).orderBy(cb.asc(task.get("dueDate")));

            return session.createQuery(cq).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Assign task to user
    public boolean assignTask(Long taskId, User assignedUser) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Task task = session.get(Task.class, taskId);
            if (task != null) {
                task.setAssignedUser(assignedUser);
                session.merge(task);
            }
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            return false;
        }
    }

    // Statistics methods using HQL
    public Long getTaskCountByStatus(User user, Status status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(t) FROM Task t WHERE t.owner = :user";

            if (status != null) {
                hql += " AND t.status = :status";
            }

            Query<Long> query = session.createQuery(hql, Long.class)
                .setParameter("user", user);

            if (status != null) {
                query.setParameter("status", status);
            }

            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public Long getTaskCountByPriority(User user, Priority priority) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "SELECT COUNT(t) FROM Task t " +
                "WHERE t.owner = :user AND t.priority = :priority", Long.class)
                .setParameter("user", user)
                .setParameter("priority", priority)
                .uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    // Additional: Get overdue tasks
    public List<Task> getOverdueTasks(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "FROM Task t WHERE t.owner = :user " +
                "AND t.dueDate < :now AND t.status != :completedStatus", Task.class)
                .setParameter("user", user)
                .setParameter("now", new Date())
                .setParameter("completedStatus", Status.COMPLETED)
                .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}