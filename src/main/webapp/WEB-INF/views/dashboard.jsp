<%@ page import="com.taskify.model.User" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ page import="java.util.UUID" %>
<%
    User user = (User) session.getAttribute("user");
    if (user == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
        return;
    }
%>
<% 
    // Generate CSRF token for forms
    String csrfToken = UUID.randomUUID().toString();
    session.setAttribute("csrfToken", csrfToken);
%>
<% 
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setHeader("Expires", "0"); // Proxies
%>
<!DOCTYPE html>
<html>
<head>
    <title>Taskify - Dashboard</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
    <link rel="stylesheet" href="<c:url value='/css/styles.css'/>">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:ital,wght@0,100..800;1,100..800&family=National+Park:wght@200..800&display=swap" rel="stylesheet">
</head>
<body class="bg-light">
    <!-- Navigation -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container">
            <a class="navbar-brand" href="<c:url value='/dashboard'/>">Taskify</a>
            <div class="d-flex align-items-center">
                <span class="text-light me-3">Welcome, ${user.name}</span>
                <form action="<c:url value='/logout'/>" method="post" style="display: inline;">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <button type="submit" class="btn btn-light">Logout</button>
                </form>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
         <!-- Alert Messages -->
        <c:if test="${not empty success}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                ${success}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
            <c:remove var="success" scope="session"/>
        </c:if>

        <c:if test="${not empty error}">
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                ${error}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
            <c:remove var="error" scope="session"/>
        </c:if>

        <!-- Filters and Actions -->
        <div class="card mb-4">
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-8">
                        <form action="<c:url value='/dashboard'/>" method="get" class="row g-2">
                            <input type="hidden" name="filterSubmitted" value="true">
                            <div class="col-md-2">
                                <select class="form-select" name="status">
                                    <option value="">All Status</option>
                                    <c:forEach items="${statuses}" var="status">
                                        <option value="${status.name()}" 
                                            ${param.status eq status.name() ? 'selected' : ''}>
                                            ${status.displayName}
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <select class="form-select" name="priority">
                                    <option value="">All Priorities</option>
                                    <c:forEach items="${priorities}" var="priority">
                                        <option value="${priority.name()}" 
                                            ${param.priority eq priority.name() ? 'selected' : ''}>
                                            ${priority.displayName}
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-md-2">
                                <select class="form-select" name="category">
                                    <option value="">All Categories</option>
                                    <c:forEach items="${categories}" var="category">
                                        <option value="${category.name()}" 
                                            ${param.category eq category.name() ? 'selected' : ''}>
                                            ${category.displayName}
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <input type="text" class="form-control" name="search" 
                                    placeholder="Search tasks..." value="${param.search}">
                            </div>
                            <div class="col-md-3">
                                <button type="submit" class="btn btn-primary w-100">Apply Filters</button>
                            </div>
                            <div class="col-md-3">
                                <a href="<c:url value='/dashboard'/>" class="btn btn-secondary w-100">Clear Filters</a>
                            </div>
                        </form>
                    </div>
                    <div class="col-md-4 text-end">
                        <a href="<c:url value='/export?type=csv'/>" class="btn btn-success me-2">Export CSV</a>
                        <a href="<c:url value='/export?type=pdf'/>" class="btn btn-danger">Export PDF</a>
                    </div>
                </div>
            </div>
        </div>

        <!-- Stats and Charts -->
        <div class="row mb-4">
            <div class="col-md-4">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">Task Progress</h5>
                        <div class="progress" style="height: 25px;">
                            <div class="progress-bar bg-success" role="progressbar"
                                style="width: ${(completedTasks / totalTasks) * 100}%">
                                <c:choose>
                                    <c:when test="${totalTasks > 0}">
                                        <fmt:formatNumber 
                                            value="${(completedTasks * 100.0) / totalTasks}" 
                                            maxFractionDigits="0"/>%
                                    </c:when>
                                </c:choose>
                            </div>
                        </div>
                        <div class="mt-2">
                            <span class="text-muted">Completed: ${completedTasks}/${totalTasks}</span>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-8">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">Tasks by Priority</h5>
                        <canvas id="priorityChart" style="height: 200px;"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- Add Task and Task List -->
        <div class="row">
            <div class="col-md-3 mb-4">
                <div class="card">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0">Add New Task</h5>
                    </div>
                    <div class="card-body">
                        <form action="<c:url value='/tasks'/>" method="post" id="taskForm">
                            <div class="mb-3">
                                <label class="form-label">Title</label>
                                <input type="text" class="form-control" name="title" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Description</label>
                                <textarea class="form-control" name="description" rows="3"></textarea>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Due Date</label>
                                <input type="date" class="form-control" name="dueDate" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Priority</label>
                                <select class="form-select" name="priority" required>
                                    <option value="">Select Priority</option>
                                    <c:forEach items="${priorities}" var="priority">
                                        <option value="${priority.name()}">${priority.displayName}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Category</label>
                                <select class="form-select" name="category" required>
                                    <option value="">Select Category</option>
                                    <c:forEach items="${categories}" var="category">
                                        <option value="${category.name()}">${category.displayName}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Assign To</label>
                                <select class="form-select" name="assignedTo">
                                    <option value="">Self</option>
                                    <c:forEach items="${users}" var="user">
                                        <option value="${user.id}">${user.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <button type="submit" class="btn btn-primary w-100">Add Task</button>
                        </form>
                    </div>
                </div>
            </div>

            <div class="col-md-9">
                <div class="card">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0">Your Tasks</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-hover">
                                <thead>
                                    <tr>
                                        <th>Title</th>
                                        <th>Due Date</th>
                                        <th>Priority</th>
                                        <th>Status</th>
                                        <th>Category</th>
                                        <th>Assigned To</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach items="${tasks}" var="task">
                                        <tr class="${task.status == 'COMPLETED' ? 'table-success' : ''} clickable-row" 
                                            data-task-id="${task.id}"
                                            data-task-title="${task.title}"
                                            data-task-description="${task.description}"
                                            data-task-due-date="<fmt:formatDate value="${task.dueDate}" pattern="dd MMM yyyy"/>"
                                            data-task-priority="${task.priority.displayName}"
                                            data-task-status="${task.status.displayName}"
                                            data-task-category="${task.category.displayName}"
                                            data-task-assigned-to="${task.assignedUser != null ? task.assignedUser.name : 'Self'}"
                                            data-task-created-at="<fmt:formatDate value="${task.createdAt}" pattern="dd MMM yyyy HH:mm"/>"
                                        >
                                            <td>${task.title}</td>
                                            <td><fmt:formatDate value="${task.dueDate}" pattern="dd MMM yyyy"/></td>
                                            <td>
                                                <span class="badge
                                                    ${task.priority == 'HIGH' ? 'bg-danger' :
                                                       task.priority == 'MEDIUM' ? 'bg-warning' : 'bg-secondary'}">
                                                    ${task.priority.displayName}
                                                </span>
                                            </td>
                                            <td>
                                                <span class="badge
                                                    ${task.status == 'COMPLETED' ? 'bg-success' : 'bg-primary'}">
                                                    ${task.status.displayName}
                                                </span>
                                            </td>
                                            <td>${task.category.displayName}</td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${task.assignedUser == null}">
                                                        Self
                                                    </c:when>
                                                    <c:otherwise>
                                                        ${task.assignedUser.name}
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <form action="<c:url value='/task'/>" method="post" style="display: inline;">
                                                    <input type="hidden" name="action" value="complete">
                                                    <input type="hidden" name="taskId" value="${task.id}">
                                                    <button type="submit" 
                                                        class="btn btn-sm ${task.status == 'COMPLETED' ? 'btn-primary' : 'btn-success'}">
                                                        ${task.status == 'COMPLETED' ? 'Mark Incomplete' : 'Mark Complete'}
                                                    </button>
                                                </form>
                                                <a href="<c:url value='/task?id=${task.id}'/>" class="btn btn-warning btn-sm">Edit</a>
                                                <a href="javascript:void(0)" onclick="deleteTask(${task.id})" 
                                                   class="btn btn-danger btn-sm">Delete</a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
        <!-- Task Details Modal -->
    <div class="modal fade" id="taskDetailsModal" tabindex="-1">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title">Task Details</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-6">
                            <h5 class="mb-3">Basic Info</h5>
                            <dl class="row">
                                <dt class="col-sm-4">Title:</dt>
                                <dd class="col-sm-8" id="detail-title"></dd>

                                <dt class="col-sm-4">Description:</dt>
                                <dd class="col-sm-8" id="detail-description"></dd>

                                <dt class="col-sm-4">Due Date:</dt>
                                <dd class="col-sm-8" id="detail-dueDate"></dd>
                            </dl>
                        </div>
                        <div class="col-md-6">
                            <h5 class="mb-3">Additional Info</h5>
                            <dl class="row">
                                <dt class="col-sm-4">Priority:</dt>
                                <dd class="col-sm-8" id="detail-priority"></dd>

                                <dt class="col-sm-4">Status:</dt>
                                <dd class="col-sm-8" id="detail-status"></dd>

                                <dt class="col-sm-4">Category:</dt>
                                <dd class="col-sm-8" id="detail-category"></dd>

                                <dt class="col-sm-4">Assigned To:</dt>
                                <dd class="col-sm-8" id="detail-assignedTo"></dd>

                                <dt class="col-sm-4">Created At:</dt>
                                <dd class="col-sm-8" id="detail-createdAt"></dd>
                            </dl>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Scripts -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        
        document.addEventListener('DOMContentLoaded', function() {
            // Add click handler to table rows
            document.querySelectorAll('.clickable-row').forEach(row => {
                row.addEventListener('click', function(e) {
                    if (e.target.closest('button, a')) return;

                    const dataset = this.dataset;

                    document.getElementById('detail-title').textContent = dataset.taskTitle;
                    document.getElementById('detail-description').textContent = dataset.taskDescription || 'No description';
                    document.getElementById('detail-dueDate').textContent = dataset.taskDueDate;  // Now hyphenated
                    document.getElementById('detail-priority').textContent = dataset.taskPriority;
                    document.getElementById('detail-status').textContent = dataset.taskStatus;
                    document.getElementById('detail-category').textContent = dataset.taskCategory;
                    document.getElementById('detail-assignedTo').textContent = dataset.taskAssignedTo;  // Now hyphenated
                    document.getElementById('detail-createdAt').textContent = dataset.taskCreatedAt;    // Now hyphenated

                    new bootstrap.Modal(document.getElementById('taskDetailsModal')).show();
                });
            });

            // Add hover effect to clickable rows
            const style = document.createElement('style');
            style.textContent = `
                .clickable-row { cursor: pointer; transition: background-color 0.2s; }
                .clickable-row:hover { background-color: rgba(0, 0, 0, 0.05); }
            `;
            document.head.appendChild(style);
        });
        // Get context path
        const contextPath = '<%= request.getContextPath() %>';

        // Priority Chart
        const priorityData = {
            labels: ['High', 'Medium', 'Low'],
            datasets: [{
                data: [${highPriorityCount}, ${mediumPriorityCount}, ${lowPriorityCount}],
                backgroundColor: ['#dc3545', '#ffc107', '#6c757d']
            }]
        };

        new Chart(document.getElementById('priorityChart'), {
            type: 'doughnut',
            data: priorityData,
            options: {
                responsive: true,
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return `${context.label}: ${context.raw} tasks`;
                            }
                        }
                    }
                }
            }
        });

        // Form Validation
        document.getElementById('taskForm').addEventListener('submit', function(e) {
            const dueDate = new Date(this.elements['dueDate'].value);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            if (dueDate < today) {
                alert('Due date cannot be in the past!');
                e.preventDefault();
            }
        });

        // Delete Task Function
        function deleteTask(taskId) {
            if (confirm('Are you sure you want to delete this task?')) {
                fetch(contextPath+"/task/"+taskId, {
                    method: 'DELETE'
                })
                .then(response => {
                    if (response.ok) {
                        window.location.reload();
                    } else {
                        response.text().then(text => alert(`Error: ${text}`));
                    }
                })
                .catch(error => alert(`Error: ${error.message}`));
            }
        }
        
        // Auto-dismiss alerts after 5 seconds
        document.querySelectorAll('.alert-dismissible').forEach(alert => {
            setTimeout(() => {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            }, 5000); // 5000 milliseconds = 5 seconds
        });
    </script>
</body>
</html>