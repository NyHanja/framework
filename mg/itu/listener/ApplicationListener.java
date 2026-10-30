package mg.itu.listener;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import mg.itu.session.Session;
import mg.itu.util.*;

import java.lang.annotation.ElementType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationListener implements ServletContextListener, HttpSessionListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();

        // Map unique pour toute l'application : id de la HttpSession -> Session du framework
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

            // Un seul objet par classe de contrôleur (singleton)
            // (ce bloc vient APRES l'affectation de urlMapping)
            Map<Class<?>, Object> controllers = new ConcurrentHashMap<>();
            for (Mapping m : urlMapping.values()) {
                Class<?> classe = m.getClasse();
                if (!controllers.containsKey(classe)) {
                    try {
                        controllers.put(classe, classe.getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        throw new Exception("Impossible d'instancier le controleur " + classe.getName()
                                + " : il faut un constructeur public sans argument (" + e + ")");
                    }
                }
            }
            servletContext.setAttribute("controllers", controllers);

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