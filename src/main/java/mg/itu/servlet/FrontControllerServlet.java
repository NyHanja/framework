package mg.itu.servlet;

import java.io.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import mg.itu.annotation.Controller;
import mg.itu.model.RouteMapping;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.itu.util.Utilitaire;

public class FrontControllerServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String url = request.getRequestURI().substring(request.getContextPath().length());

        RouteMapping mapping = routes.get(url);

        if (mapping == null) {
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<h3 style='color:red'>tsy fantatro io url io fa reto iany no url fantatra : " + url + "</h3>");
            out.println("<ul>");
            for (String u : routes.keySet()) {
                out.println("<li>" + u + "</li>");
            }
            out.println("</ul>");
            return;
        }

        request.setAttribute("route", mapping);
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private Map<String, RouteMapping> routes;

    @Override
    public void init() throws ServletException {

        try {

            List<Class<?>> classes = Utilitaire.getClasses(
                    "controller");

            List<Class<?>> controllers = Utilitaire.getAnnotatedClasses(
                    classes,
                    Controller.class);

            routes = Utilitaire.getUrlMappings(
                    controllers);

            System.out.println(
                    "=== URL disponibles ===");

            for (String url : routes.keySet()) {

                System.out.println(url);

            }

        } catch (Exception e) {

            throw new ServletException(e);

        }

    }
}