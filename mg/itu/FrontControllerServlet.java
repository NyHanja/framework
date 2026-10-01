package mg.itu;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import mg.itu.annotation.Api;
import mg.itu.util.*;
import java.util.*;
import mg.itu.view.*;

public class FrontControllerServlet extends HttpServlet {
    List<String> listeControllers;

    Map<UrlMethod, Mapping> urlMapping;

    Object applicationContext;

    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        listeControllers = (List<String>) getServletContext().getAttribute("listeControllers");
        urlMapping = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("urlMapping");

        if (getServletContext().getAttribute("springContext") != null) {
            applicationContext = getServletContext().getAttribute("springContext");
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String url = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();

        afficher(url, method, request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void afficher(String url, String method, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Mapping mapping = urlMapping.get(new UrlMethod(url, method));

        // ---------- URL inconnue ----------
        if (mapping == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("Url non trouvee : " + url);
            out.println("<h2>Liste des URL disponibles :</h2>");
            for (Map.Entry<UrlMethod, Mapping> e : urlMapping.entrySet()) {
                out.println("<p>URL: " + e.getKey().getUrl() + " avec la methode : " + e.getKey().getMethod()
                        + " | Classe: " + e.getValue().getClasse().getName()
                        + " | Fonction: " + e.getValue().getMethode().getName() + "</p>");
            }
            return;
        }

        try {
            Object instance = mapping.getClasse().getDeclaredConstructor().newInstance();
            Method methode = mapping.getMethode();

            Object[] arguments = new Object[methode.getParameters().length];
            if (applicationContext != null) {
                Utilitaire.creerArguments(methode, arguments, applicationContext);
            } else {
                Utilitaire.creerArguments(methode, arguments);
            }

            Object resultat = methode.invoke(instance, arguments);

            // ---------- Mode API : JSON ----------
            if (methode.isAnnotationPresent(Api.class)) {
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter out = response.getWriter();
                if (resultat instanceof String) {
                    out.print(resultat); // String : renvoye tel quel
                } else {
                    out.print(Utilitaire.convertToJson(resultat)); // autre : JSON
                }
                return;
            }

            // ---------- Mode vue ----------
            if (resultat instanceof ModelAndView) {
                ModelAndView mv = (ModelAndView) resultat;
                ViewResolver viewResolver = new ViewResolver();
                viewResolver.setNom_vue(mv.getNom_vue());
                viewResolver.setPrefix_vue(getServletContext().getInitParameter("prefixVue"));
                viewResolver.setExtension_vue(getServletContext().getInitParameter("suffixVue"));

                for (Map.Entry<String, Object> entry : mv.getAttributs().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }

                request.getRequestDispatcher(viewResolver.getCheminCompletVue()).forward(request, response);
            } else {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter()
                        .println("<p>Le resultat n'est pas un ModelAndView (utilisez @Api pour du JSON).</p>");
            }

        } catch (Exception e) {
            Throwable cause = (e instanceof InvocationTargetException && e.getCause() != null) ? e.getCause() : e;
            cause.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<p>Erreur lors de l'invocation de la methode : " + cause + "</p>");
        }
    }

}