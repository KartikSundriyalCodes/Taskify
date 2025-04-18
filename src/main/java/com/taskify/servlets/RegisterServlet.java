package com.taskify.servlets;

import com.taskify.dao.UserDAO;
import com.taskify.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final UserDAO userDao = new UserDAO();

    // Show registration form
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    // Handle form submission
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Get form parameters
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            // Validate input
            if (name == null || name.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
                throw new IllegalArgumentException("All fields are required");
            }

            // Create user object
            User newUser = new User(name.trim(), email.trim(), password.trim());

            // Save user to database
            User savedUser = userDao.saveUser(newUser);

            if (savedUser != null) {
                // Registration successful - redirect to login with success message
                request.getSession().setAttribute("success", "Registration successful! Please login.");
//                response.sendRedirect("login");
            } else {
                throw new Exception("Registration failed due to server error");
            }
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            request.setAttribute("error", e.getMessage());
            request.setAttribute("name", name);
            request.setAttribute("email", email);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        } catch (Exception e) {
            // Handle other exceptions
            e.printStackTrace();
            request.setAttribute("error", "Registration failed: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }
}