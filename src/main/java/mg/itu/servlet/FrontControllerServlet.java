package mg.itu.servlet;

import java.io.*;
import java.util.List;
import mg.itu.annotation.Controller;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.itu.util.Utilitaire;

public class FrontControllerServlet extends HttpServlet {
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<h1>Front Controller</h1>");
        out.println("<p>URL recue : " + request.getRequestURL() + "</p>");

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public void init() throws ServletException {

        try {

            List<Class<?>> classes = Utilitaire.getClasses(
                    "controller");

            List<Class<?>> controllers = Utilitaire.getAnnotatedClasses(
                    classes,
                    Controller.class);

            System.out.println(
                    "=== Controllers trouvés ===");

            for (Class<?> controller : controllers) {

                System.out.println(
                        controller.getName());
            }

        } catch (Exception e) {

            throw new ServletException(e);
        }
    }

}