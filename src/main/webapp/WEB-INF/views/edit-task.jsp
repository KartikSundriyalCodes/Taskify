<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Edit Task - Taskify</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="<c:url value='${pageContext.request.contextPath}/css/styles.css'/>">
</head>
<body class="bg-light">
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container">
            <a class="navbar-brand" href="<c:url value='/dashboard'/>">Taskify</a>
            <div class="d-flex align-items-center">
                <span class="text-light me-3">Welcome, ${user.name}</span>
                <a href="<c:url value='/logout'/>" class="btn btn-outline-light">Logout</a>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="row justify-content-center">
            <div class="col-md-8">
                <div class="card shadow">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0">Edit Task</h5>
                    </div>
                    <div class="card-body">
                        <c:if test="${not empty error}">
                            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                                ${error}
                                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                            </div>
                        </c:if>

                        <form action="<c:url value='/task'/>" method="post">
                            <input type="hidden" name="taskId" value="${task.id}">
                            
                            <div class="mb-3">
                                <label class="form-label">Title</label>
                                <input type="text" class="form-control" name="title" 
                                       value="${task.title}" required>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Description</label>
                                <textarea class="form-control" name="description" rows="3">${task.description}</textarea>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Due Date</label>
                                <input type="date" class="form-control" name="dueDate" 
                                       value="<fmt:formatDate value="${task.dueDate}" pattern="yyyy-MM-dd"/>" 
                                       required>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Priority</label>
                                <select class="form-select" name="priority" required>
                                    <c:forEach items="${priorities}" var="priority">
                                        <option value="${priority.name()}" 
                                            ${task.priority == priority ? 'selected' : ''}>
                                            ${priority.displayName}
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Status</label>
                                <select class="form-select" name="status" required>
                                    <c:forEach items="${statuses}" var="status">
                                        <option value="${status.name()}" 
                                            ${task.status == status ? 'selected' : ''}>
                                            ${status.displayName}
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Category</label>
                                <select class="form-select" name="category" required>
                                    <c:forEach items="${categories}" var="category">
                                        <option value="${category.name()}" 
                                            ${task.category == category ? 'selected' : ''}>
                                            ${category.displayName}
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="mb-3">
                                <label class="form-label">Assign To</label>
                                <select class="form-select" name="assignedTo">
                                    <option value="">Self</option>
                                    <c:forEach items="${users}" var="user">
                                        <option value="${user.id}" 
                                            ${task.assignedUser != null && task.assignedUser.id == user.id ? 'selected' : ''}>
                                            ${user.name}
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>

                            <div class="d-grid gap-2">
                                <button type="submit" class="btn btn-primary">Update Task</button>
                                <a href="<c:url value='/dashboard'/>" class="btn btn-secondary">Cancel</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Date validation
        document.querySelector('form').addEventListener('submit', function(e) {
            const dueDate = new Date(this.elements['dueDate'].value);
            const today = new Date();
            today.setHours(0, 0, 0, 0);

            if (dueDate < today) {
                alert('Due date cannot be in the past!');
                e.preventDefault();
            }
        });
    </script>
</body>
</html>