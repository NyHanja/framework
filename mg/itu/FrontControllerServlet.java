package mg.itu;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import mg.itu.annotation.Api;
import jakarta.servlet.annotation.MultipartConfig;
import mg.itu.annotation.Auth;
import mg.itu.annotation.UrlMapping;
import mg.itu.util.*;
import java.util.*;
import mg.itu.view.*;
import mg.itu.session.Session;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 10 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class FrontControllerServlet extends HttpServlet {
    List<String> listeControllers;

    Map<UrlMethod, Mapping> urlMapping;
    String cleAuth;
    String cleRole;

    Object applicationContext;

    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        listeControllers = (List<String>) getServletContext().getAttribute("listeControllers");
        urlMapping = (Map<UrlMethod, Mapping>) getServletContext().getAttribute("urlMapping");
        cleAuth = getServletContext().getInitParameter("sessionAuth");
        cleRole = getServletContext().getInitParameter("sessionRole");

        boolean besoinAuth = false;
        boolean besoinRole = false;
        if (urlMapping != null) {
            for (Mapping m : urlMapping.values()) {
                Auth a = m.getMethode().getAnnotation(Auth.class);
                if (a != null) {
                    besoinAuth = true;
                    if (!a.role().isEmpty()) {
                        besoinRole = true;
                    }
                }
            }
        }
        if (besoinAuth && (cleAuth == null || cleAuth.trim().isEmpty())) {
            throw new ServletException(
                    "Des methodes @Auth existent mais l'init-param 'sessionAuth' est absent dans web.xml");
        }
        if (besoinRole && (cleRole == null || cleRole.trim().isEmpty())) {
            throw new ServletException(
                    "Des methodes @Auth(role=...) existent mais l'init-param 'sessionRole' est absent dans web.xml");
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
        // ---------- Contrôle d'authentification et de rôle (@Auth) ----------
        Auth auth = mapping.getMethode().getAnnotation(Auth.class);
        if (auth != null) {
            Session s = trouverSession(request);

            // 1. Authentifié ? (un rôle seul ne suffit jamais)
            if (s == null || s.get(cleAuth) == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println("<p>Authentification requise.</p>");
                return;
            }

            // 2. Rôle exigé ?
            String roleRequis = auth.role();
            if (!roleRequis.isEmpty()) {
                Object roleUtilisateur = s.get(cleRole);
                if (roleUtilisateur == null || !roleRequis.equals(roleUtilisateur.toString())) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().println("<p>Acces refuse : role insuffisant.</p>");
                    return;
                }
            }
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
    private Session trouverSession(HttpServletRequest request) {
        HttpSession hs = request.getSession(false);
        if (hs == null) {
            return null;
        }
        Map<String, Session> sessions = (Map<String, Session>) getServletContext().getAttribute("sessions");
        if (sessions == null) {
            return null;
        }
        return sessions.get(hs.getId());
    }

    // Réservée au rôle admin
    @UrlMapping(value = "/admin-only", method = "GET")
    @Auth(role = "admin")
    public ModelAndView adminOnly(Session session) {
        System.out.println(">>> /admin-only EXECUTE pour : " + session.get("user"));
        ModelAndView mv = new ModelAndView("admin");
        mv.addAttribut("user", session.get("user"));
        return mv;
    }

    // API réservée au rôle admin, sans paramètre Session
    @UrlMapping(value = "/api/admin-secret", method = "GET")
    @Auth(role = "admin")
    @Api
    public String adminSecret() {
        return "{\"secret\":\"admin\"}";
    }

    // Range un rôle SANS utilisateur : sert à tester le cas "rôle sans
    // authentification"
    @UrlMapping(value = "/faux-role", method = "GET")
    public ModelAndView fauxRole(Session session) {
        session.set("role", "admin");
        ModelAndView mv = new ModelAndView("profil");
        mv.addAttribut("user", session.get("user"));
        mv.addAttribut("visites", session.get("visites"));
        return mv;
    }

}