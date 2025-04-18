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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
    
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login");
            return;
        }

        User user = (User) session.getAttribute("user");
        Map<String, String> filters = new HashMap<>();

        try {
            // Get filter parameters
            String statusParam = request.getParameter("status");
            String priorityParam = request.getParameter("priority");
            String categoryParam = request.getParameter("category");
            String searchQuery = request.getParameter("search");
            
            boolean isReset = false;
            if(statusParam == null && priorityParam == null && categoryParam == null && searchQuery == null) {
                isReset = true;
            }
            
            // Check if new filters are applied
            boolean newFilters = request.getParameterMap().containsKey("status") ||
                                request.getParameterMap().containsKey("priority") ||
                                request.getParameterMap().containsKey("category") ||
                                request.getParameterMap().containsKey("search");

            if (newFilters || isReset) {
                // Store new filters in session
                filters.put("status", statusParam);
                filters.put("priority", priorityParam);
                filters.put("category", categoryParam);
                filters.put("search", searchQuery);
                session.setAttribute("dashboardFilters", filters);
            } else {
                // Use existing filters from session
                filters = (Map<String, String>) session.getAttribute("dashboardFilters");
                if (filters != null && !isReset) {
                    statusParam = filters.get("status");
                    priorityParam = filters.get("priority");
                    categoryParam = filters.get("category");
                    searchQuery = filters.get("search");
                }
            }

            // Convert to enums
            Task.Status status = parseEnum(statusParam, Task.Status.class);
            Task.Priority priority = parseEnum(priorityParam, Task.Priority.class);
            Task.Category category = parseEnum(categoryParam, Task.Category.class);

            // Get filtered tasks
            List<Task> tasks = taskDAO.getTasksByUser(
                user, 
                status,
                priority,
                category,
                searchQuery
            );
            
            // Fetch all users EXCEPT the current user
            List<User> allUsers = userDAO.getAllUsersExcept(user.getId());

            // Add users to the request attributes
            request.setAttribute("users", allUsers);


            long totalTasks = tasks.size();
            long completedTasks = tasks.stream()
                .filter(t -> t.getStatus() == Task.Status.COMPLETED)
                .count();

            Map<Task.Priority, Long> priorityCounts = tasks.stream()
                .collect(Collectors.groupingBy(
                    Task::getPriority,
                    Collectors.counting()
                ));

            long highPriorityCount = priorityCounts.getOrDefault(Task.Priority.HIGH, 0L);
            long mediumPriorityCount = priorityCounts.getOrDefault(Task.Priority.MEDIUM, 0L);
            long lowPriorityCount = priorityCounts.getOrDefault(Task.Priority.LOW, 0L);

            // Set request attributes
            request.setAttribute("tasks", tasks);
            request.setAttribute("totalTasks", totalTasks);
            request.setAttribute("completedTasks", completedTasks);
            request.setAttribute("highPriorityCount", highPriorityCount);
            request.setAttribute("mediumPriorityCount", mediumPriorityCount);
            request.setAttribute("lowPriorityCount", lowPriorityCount);

            // Set enums for dropdowns
            request.setAttribute("statuses", Task.Status.values());
            request.setAttribute("priorities", Task.Priority.values());
            request.setAttribute("categories", Task.Category.values());
            

            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", "Error loading tasks: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
        }
    }

// Helper method for enum conversion
    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Enum.valueOf(enumType, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}