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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(name = "TaskManagementServlet", urlPatterns = {"/task", "/task/*"})
public class TaskManagementServlet extends HttpServlet {
    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    private Long extractTaskId(HttpServletRequest request) {
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && !pathInfo.isEmpty()) {
                String[] pathParts = pathInfo.split("/");
                return Long.parseLong(pathParts[1]);
            }
            return Long.parseLong(request.getParameter("id"));
        } catch (Exception e) {
            return null;
        }
    }
    
    private void redirectWithFilters(HttpServletRequest request, HttpServletResponse response) 
        throws IOException {
        
        HttpSession session = request.getSession();
        Map<String, String> filters = (Map<String, String>) session.getAttribute("dashboardFilters");
        
        StringBuilder redirect = new StringBuilder(request.getContextPath() + "/dashboard");
        
        if (filters != null && !filters.isEmpty()) {
            redirect.append("?");
            List<String> params = new ArrayList<>();
            
            if (filters.get("status") != null) 
                params.add("status=" + filters.get("status"));
            if (filters.get("priority") != null) 
                params.add("priority=" + filters.get("priority"));
            if (filters.get("category") != null) 
                params.add("category=" + filters.get("category"));
            if (filters.get("search") != null) 
                params.add("search=" + URLEncoder.encode(filters.get("search"), StandardCharsets.UTF_8));
            
            redirect.append(String.join("&", params));
        }
        
        response.sendRedirect(redirect.toString());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        
        if (currentUser == null) {
            response.sendRedirect("login");
            return;
        }

        try {
            Long taskId = extractTaskId(request);
            if (taskId == null) throw new IllegalArgumentException("Invalid task ID");

            Task task = taskDAO.getTaskById(taskId);
            if (task == null) throw new IllegalArgumentException("Task not found");

            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new SecurityException("You don't have permission to access this task");
            }

            request.setAttribute("task", task);
            request.setAttribute("users", userDAO.getAllUsersExcept(currentUser.getId()));
            request.setAttribute("priorities", Task.Priority.values());
            request.setAttribute("statuses", Task.Status.values());
            request.setAttribute("categories", Task.Category.values());
            
            request.getRequestDispatcher("/WEB-INF/views/edit-task.jsp").forward(request, response);

        } catch (Exception e) {
            session.setAttribute("error", e.getMessage());
            response.sendRedirect("dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        if ("complete".equals(action)) {
            handleCompleteAction(request, response);
        } else {
            handleUpdateAction(request, response);
        }
    }

    private void handleCompleteAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        
        try {
            Long taskId = Long.parseLong(request.getParameter("taskId"));
            Task task = taskDAO.getTaskById(taskId);

            if (task == null) throw new IllegalArgumentException("Task not found");
            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new SecurityException("Unauthorized operation");
            }

            // Toggle completion status
            if (task.getStatus() == Task.Status.COMPLETED) {
                task.setStatus(Task.Status.TODO);
            } else {
                task.setStatus(Task.Status.COMPLETED);
            }

            if (taskDAO.updateTask(task)) {
                session.setAttribute("success", "Task status updated successfully!");
            } else {
                throw new Exception("Failed to update task status");
            }
            
        } catch (Exception e) {
            session.setAttribute("error", e.getMessage());
        }
        
//        response.sendRedirect("dashboard");
        redirectWithFilters(request, response);
    }

    private void handleUpdateAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");
        String redirect = "dashboard";

        try {
            Long taskId = Long.parseLong(request.getParameter("taskId"));
            Task task = taskDAO.getTaskById(taskId);

            if (task == null) throw new IllegalArgumentException("Task not found");
            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new SecurityException("Unauthorized modification attempt");
            }

            // Update task properties
            task.setTitle(validateNotEmpty(request.getParameter("title"), "Title"));
            task.setDescription(request.getParameter("description"));
            task.setDueDate(validateDueDate(request.getParameter("dueDate")));
            task.setPriority(Task.Priority.valueOf(request.getParameter("priority")));
            task.setStatus(Task.Status.valueOf(request.getParameter("status")));
            task.setCategory(Task.Category.valueOf(request.getParameter("category")));

            // Update assigned user
            String assignedTo = request.getParameter("assignedTo");
            if (assignedTo != null && !assignedTo.isEmpty()) {
                User assignedUser = userDAO.findById(Long.parseLong(assignedTo));
                task.setAssignedUser(assignedUser);
            } else {
                task.setAssignedUser(null);
            }

            if (taskDAO.updateTask(task)) {
                session.setAttribute("success", "Task updated successfully!");
            } else {
                throw new Exception("Failed to update task");
            }

        } catch (Exception e) {
            session.setAttribute("error", "Error updating task: " + e.getMessage());
        }
        
//        response.sendRedirect(redirect);
        redirectWithFilters(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user");

        try {
            Long taskId = extractTaskId(request);
            if (taskId == null) throw new IllegalArgumentException("Invalid task ID");

            Task task = taskDAO.getTaskById(taskId);
            if (task == null) throw new IllegalArgumentException("Task not found");
            if (!task.getOwner().getId().equals(currentUser.getId())) {
                throw new SecurityException("Unauthorized operation");
            }

            if (taskDAO.deleteTask(taskId)) {
                session.setAttribute("success", "Task deleted successfully");
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to delete task");
            }
            
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // Validation methods
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