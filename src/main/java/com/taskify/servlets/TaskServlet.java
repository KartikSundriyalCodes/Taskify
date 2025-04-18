package com.taskify.servlets;

import com.taskify.dao.TaskDAO;
import com.taskify.dao.UserDAO;
import com.taskify.model.Task;
import com.taskify.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        String redirect = "dashboard";
        
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        try {
            // Validate and parse parameters
            String title = validateNotEmpty(request.getParameter("title"), "Title");
            String description = request.getParameter("description");
            Date dueDate = validateDueDate(request.getParameter("dueDate"));
            
            // Convert string parameters to enums
            Task.Priority priority = Task.Priority.valueOf(
                request.getParameter("priority").toUpperCase()
            );
            Task.Category category = Task.Category.valueOf(
                request.getParameter("category").toUpperCase()
            );

            // Create new task
            Task newTask = new Task();
            newTask.setTitle(title);
            newTask.setDescription(description);
            newTask.setDueDate(dueDate);
            newTask.setPriority(priority);
            newTask.setCategory(category);
            newTask.setStatus(Task.Status.TODO);
            newTask.setOwner(currentUser);

            // Handle assigned user
            String assignedTo = request.getParameter("assignedTo");
            if (assignedTo != null && !assignedTo.isEmpty()) {
                User assignedUser = userDAO.findById(Long.parseLong(assignedTo));
                newTask.setAssignedUser(assignedUser);
            }

            // Save to database
            Long taskId = taskDAO.createTask(newTask);
            
            if (taskId != null) {
                session.setAttribute("success", "Task created successfully!");
            } else {
                throw new Exception("Failed to create task");
            }
            
        } catch (IllegalArgumentException e) {
            session.setAttribute("error", "Invalid input: " + e.getMessage());
            redirect = "dashboard?formError=true";
        } catch (Exception e) {
            session.setAttribute("error", "Error creating task: " + e.getMessage());
            redirect = "dashboard";
        }
        
        response.sendRedirect(redirect);
    }

    private String validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return value.trim();
    }

    private Date validateDueDate(String dueDateString) {
        if (dueDateString == null || dueDateString.isEmpty()) {
            throw new IllegalArgumentException("Due date is required");
        }
        
        Date dueDate = Date.valueOf(dueDateString);
        if (dueDate.toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
        return dueDate;
    }
}