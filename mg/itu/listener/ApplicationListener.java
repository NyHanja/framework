package mg.itu.listener;

import jakarta.servlet.*;
import mg.itu.util.*;

import java.lang.annotation.ElementType;
import java.util.logging.*;
import java.util.*;

public class ApplicationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        List<String> listeControllers = new ArrayList<>();
        Map<UrlMethod, Mapping> urlMapping;

        try {
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

}
