package mg.itu;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import mg.itu.annotation.Api;
import jakarta.servlet.annotation.MultipartConfig;
import mg.itu.annotation.Auth;
import mg.itu.util.*;
import java.util.*;
import mg.itu.view.*;
import mg.itu.session.Session;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 10 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class FrontControllerServlet extends HttpServlet {
    List<String> listeControllers;

    Map<UrlMethod, Mapping> urlMapping;
    String cleAuth;

    Object applicationContext;

    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        listeControllers = (List<String>) getServletContext().getAttribute("listeControllers");
        urlMapping = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("urlMapping");
        cleAuth = getServletContext().getInitParameter("sessionAuth");

        boolean aDesMethodesAuth = false;
        if (urlMapping != null) {
            for (Mapping m : urlMapping.values()) {
                if (m.getMethode().isAnnotationPresent(Auth.class)) {
                    aDesMethodesAuth = true;
                    break;
                }
            }
        }
        if (aDesMethodesAuth && (cleAuth == null || cleAuth.trim().isEmpty())) {
            throw new ServletException(
                    "Des methodes @Auth existent mais l'init-param 'sessionAuth' est absent dans web.xml");
        }

        if (getServletContext().getAttribute("springContext") != null) {
            applicationContext = getServletContext().getAttribute("springContext");
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

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
        if (mapping.getMethode().isAnnotationPresent(Auth.class) && !estAuthentifie(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<p>Authentification requise.</p>");
            return; // la méthode n'est jamais exécutée
        }

        try {
            Object instance = mapping.getClasse().getDeclaredConstructor().newInstance();
            Method methode = mapping.getMethode();

            Object[] arguments = new Object[methode.getParameters().length];
            Utilitaire.creerArguments(methode, arguments, applicationContext, request);

            Object resultat = methode.invoke(instance, arguments);

            if (methode.isAnnotationPresent(Api.class)) {
                response.setContentType("application/json;charset=UTF-8");
                PrintWriter out = response.getWriter();
                if (resultat instanceof String) {
                    out.print(resultat);
                } else {
                    out.print(Utilitaire.convertToJson(resultat));
                }
                return;
            }

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

    @SuppressWarnings("unchecked")
    private boolean estAuthentifie(HttpServletRequest request) {
        // 1. Session Tomcat existante ? (false = ne pas en créer une)
        HttpSession hs = request.getSession(false);
        if (hs == null) {
            return false;
        }

        // 2. Map des sessions du framework
        Map<String, Session> sessions = (Map<String, Session>) getServletContext().getAttribute("sessions");
        if (sessions == null) {
            return false;
        }

        // 3. Notre Session pour cet identifiant
        Session session = sessions.get(hs.getId());
        if (session == null) {
            return false;
        }

        // 4. La clé configurée est-elle présente ?
        return session.get(cleAuth) != null;
    }

}