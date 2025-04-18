package com.taskify.servlets;

import com.taskify.dao.TaskDAO;
import com.taskify.model.Task;
import com.taskify.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@WebServlet("/export")
public class ExportServlet extends HttpServlet {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not logged in");
            return;
        }

        String type = request.getParameter("type");
        if (type == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Export type parameter missing");
            return;
        }

        TaskDAO taskDAO = new TaskDAO();
        List<Task> tasks = taskDAO.getTasksByUser(user, null, null, null, null);

        try {
            if ("csv".equalsIgnoreCase(type)) {
                exportCSV(response, tasks);
            } else if ("pdf".equalsIgnoreCase(type)) {
                exportPDF(response, tasks);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported export type");
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Export failed: " + e.getMessage());
        }
    }

    private void exportCSV(HttpServletResponse response, List<Task> tasks) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=tasks.csv");

        try (CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), 
                CSVFormat.DEFAULT.builder()
                    .setHeader("Title", "Description", "Due Date", "Priority", "Status", "Category")
                    .build())) {
            
            for (Task task : tasks) {
                csvPrinter.printRecord(
                    task.getTitle(),
                    task.getDescription(),
                    task.getDueDate() != null ? DATE_FORMAT.format(task.getDueDate()) : "",
                    task.getPriority().toString(),
                    task.getStatus().toString(),
                    task.getCategory().toString()
                );
            }
        }
    }

    private void exportPDF(HttpServletResponse response, List<Task> tasks) 
            throws IOException, DocumentException {
        
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=tasks.pdf");

        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            addPDFTitle(document);
            addPDFTable(document, tasks);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void addPDFTitle(Document document) throws DocumentException {
        Paragraph title = new Paragraph("Task Export Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);
    }

    private void addPDFTable(Document document, List<Task> tasks) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        addTableHeader(table);
        addTableRows(table, tasks);

        document.add(table);
    }

    private void addTableHeader(PdfPTable table) {
        String[] headers = {"Title", "Description", "Due Date", "Priority", "Status", "Category"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new BaseColor(220, 220, 220));
            table.addCell(cell);
        }
    }

    private void addTableRows(PdfPTable table, List<Task> tasks) {
        for (Task task : tasks) {
            table.addCell(safeString(task.getTitle()));
            table.addCell(safeString(task.getDescription()));
            table.addCell(safeDate(task.getDueDate()));
            table.addCell(safeEnum(task.getPriority()));
            table.addCell(safeEnum(task.getStatus()));
            table.addCell(safeEnum(task.getCategory()));
        }
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String safeDate(Date date) {
        return date != null ? DATE_FORMAT.format(date) : "";
    }

    private String safeEnum(Enum<?> value) {
        return value != null ? value.toString() : "";
    }
}