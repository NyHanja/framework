package mg.itu.listener;

import jakarta.servlet.*;
import mg.itu.session.Session;
import mg.itu.util.*;
import jakarta.servlet.http.HttpSessionListener;
import jakarta.servlet.*;

import java.lang.annotation.ElementType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpSessionEvent;

public class ApplicationListener implements ServletContextListener, HttpSessionListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        servletContext.setAttribute("sessions", new ConcurrentHashMap<String, Session>());

        List<String> listeControllers = new ArrayList<>();
        Map<UrlMethod, Mapping> urlMapping;

        try {
            try {
                Class<?> utils = Class.forName(
                        "org.springframework.web.context.support.WebApplicationContextUtils");

                Object ctx = utils
                        .getMethod("getWebApplicationContext", ServletContext.class)
                        .invoke(null, servletContext);

                servletContext.setAttribute("springContext", ctx);
            } catch (Exception e) {
                System.err.println("================================");
                System.err.println("Spring non detecte: " + e.getMessage());
                System.err.println("================================");
            }

            String nom_package = servletContext.getInitParameter("nomPackage");

            Utilitaire.recupererClassesAvecAnnotation(new Utilitaire(nom_package,
                    "mg.itu.annotation.Controller", ElementType.METHOD), listeControllers);

            servletContext.setAttribute("listeControllers", listeControllers);

            urlMapping = Utilitaire.recupererUrlMapping(
                    new Utilitaire(nom_package, "mg.itu.annotation.UrlMapping", ElementType.METHOD));

            servletContext.setAttribute("urlMapping", urlMapping);
        } catch (Exception e) {

            System.err.println("================================");
            System.err.println("Erreur lors de l'initialisation: " + e.getMessage());
            System.err.println("================================");

            throw new RuntimeException(e);

        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        servletContextEvent.getServletContext().log("## Arrêt de l'application ##");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sessionDestroyed(HttpSessionEvent se) {
        String id = se.getSession().getId();
        Map<String, Session> sessions = (Map<String, Session>) se.getSession().getServletContext()
                .getAttribute("sessions");

        if (sessions != null) {
            sessions.remove(id);
            System.out.println("Session retiree de la Map : " + id);
        }
    }

}
