package mg.itu.util;

import java.io.File;
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import com.google.gson.Gson;
import mg.itu.annotation.Param;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

public class Utilitaire {
    private String nom_package;
    private String annotation;
    private ElementType niveau;

    public Utilitaire(String nom_package, String annotation, ElementType niveau) {
        this.nom_package = nom_package;
        this.annotation = annotation;
        this.niveau = niveau;
    }

    public String getNom_package() {
        return nom_package;
    }

    public String getAnnotation() {
        return annotation;
    }

    public void setAnnotation(String annotation) {
        this.annotation = annotation;
    }

    public ElementType getNiveau() {
        return niveau;
    }

    public void setNiveau(ElementType niveau) {
        this.niveau = niveau;
    }

    public void setNom_package(String nom_package) {
        this.nom_package = nom_package;
    }

    public static void recupererClasses(String nomPackage, List<Class<?>> classes) throws Exception {

        String cheminDossier = nomPackage.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL ressource = classLoader.getResource(cheminDossier);

        if (ressource == null) {
            throw new IllegalArgumentException("Le package " + nomPackage + " n'existe pas.");
        }

        File dossier = new File(ressource.toURI());

        if (dossier.exists() && dossier.isDirectory()) {
            File[] fichiers = dossier.listFiles();
            if (fichiers != null) {
                for (File fichier : fichiers) {
                    if (fichier.isFile() && fichier.getName().endsWith(".class")) {
                        String nomClasse = nomPackage + '.'
                                + fichier.getName().substring(0, fichier.getName().length() - 6);

                        classes.add(Class.forName(nomClasse));
                    }
                }
            }
        }
    }

    public static void recupererClassesAvecAnnotation(Utilitaire utilitaire, List<String> listeAvecAnnotation)
            throws Exception {
        try {
            utilitaire.recupererElements(utilitaire, listeAvecAnnotation);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Erreur lors de la récupération des classes : " + e.getMessage());
        }
    }

    public void recupererElements(Utilitaire utilitaire, List<String> resultat) throws Exception {

        List<Class<?>> classes = new ArrayList<>();
        recupererClasses(utilitaire.getNom_package(), classes);

        Class<?> annotationClass = Class.forName(utilitaire.getAnnotation());

        if (!annotationClass.isAnnotation()) {
            throw new Exception("Ce n'est pas une annotation");
        }

        Class<? extends Annotation> annotation = annotationClass.asSubclass(Annotation.class);

        switch (utilitaire.getNiveau()) {

            case TYPE:
                for (Class<?> classe : classes) {
                    if (classe.isAnnotationPresent(annotation)) {
                        resultat.add(classe.toString());
                    }
                }
                break;

            case FIELD:
                for (Class<?> classe : classes) {
                    for (Field field : classe.getDeclaredFields()) {
                        if (field.isAnnotationPresent(annotation)) {
                            resultat.add(field.toString());
                        }
                    }
                }
                break;

            case METHOD:
                for (Class<?> classe : classes) {
                    for (Method method : classe.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(annotation)) {
                            resultat.add(method.toString());
                        }
                    }
                }
                break;
        }

    }

    public static Map<UrlMethod, Mapping> recupererUrlMapping(Utilitaire utilitaire) throws Exception {
        Map<UrlMethod, Mapping> urlMapping = new HashMap<>();

        List<Class<?>> classes = new ArrayList<>();
        recupererClasses(utilitaire.getNom_package(), classes);

        Class<?> annotationClass = Class.forName(utilitaire.getAnnotation());

        if (!annotationClass.isAnnotation()) {
            throw new Exception("Ce n'est pas une annotation");
        }

        Class<? extends Annotation> annotation = annotationClass.asSubclass(Annotation.class);

        Method valueMethod = annotation.getMethod("value");
        Method methodUrl = annotation.getMethod("method");

        for (Class<?> classe : classes) {
            for (Method method : classe.getDeclaredMethods()) {

                if (method.isAnnotationPresent(annotation)) {

                    Annotation ann = method.getAnnotation(annotation);

                    String url = (String) valueMethod.invoke(ann);
                    String methodOfUrl = (String) methodUrl.invoke(ann);

                    UrlMethod urlMethod = new UrlMethod(url, methodOfUrl);

                    if (urlMapping.containsKey(urlMethod)) {
                        throw new Exception("URL Deja utilise par un autre controller : " + urlMethod.getUrl()
                                + " avec la methode : " + urlMethod.getMethod());
                    }

                    urlMapping.put(urlMethod, new Mapping(classe, method));
                }
            }
        }

        return urlMapping;
    }

    public static void creerArguments(Method methode, Object[] arguments,
            Object applicationContext, HttpServletRequest request) {
        for (int i = 0; i < methode.getParameters().length; i++) {
            Parameter p = methode.getParameters()[i];

            if (applicationContext != null && p.getType().isAssignableFrom(applicationContext.getClass())) {
                arguments[i] = applicationContext;
            } else if (!estTypeSimple(p.getType())) {
                Object objet = creerObjet(p.getType());
                remplirObjet(objet, request);
                arguments[i] = objet;

            } else {
                String nomChamp = p.isAnnotationPresent(Param.class)
                        ? p.getAnnotation(Param.class).value()
                        : p.getName();
                arguments[i] = convertir(request.getParameter(nomChamp), p.getType());
            }
        }
    }

    public static String convertToJson(Object object) {
        return new Gson().toJson(object);
    }

    private static Object valeurParDefaut(Class<?> type) {
        // primitif : 0 / 0.0 / false ; objet : null
        return type.isPrimitive() ? Array.get(Array.newInstance(type, 1), 0) : null;
    }

    private static Object convertir(String valeur, Class<?> type) {
        if (type == String.class) {
            return valeur; // null si absent
        }

        if (valeur == null || valeur.trim().isEmpty()) {
            return valeurParDefaut(type);
        }
        valeur = valeur.trim();

        try {
            if (type == int.class || type == Integer.class)
                return Integer.parseInt(valeur);
            if (type == long.class || type == Long.class)
                return Long.parseLong(valeur);
            if (type == double.class || type == Double.class)
                return Double.parseDouble(valeur);
            if (type == float.class || type == Float.class)
                return Float.parseFloat(valeur);
            if (type == short.class || type == Short.class)
                return Short.parseShort(valeur);
            if (type == byte.class || type == Byte.class)
                return Byte.parseByte(valeur);
            if (type == boolean.class || type == Boolean.class) {
                // une case HTML cochée envoie "on", pas "true"
                return valeur.equalsIgnoreCase("true") || valeur.equalsIgnoreCase("on") || valeur.equals("1");
            }
            if (type == char.class || type == Character.class)
                return valeur.charAt(0);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Valeur invalide '" + valeur + "' pour le type " + type.getSimpleName());
        }

        return valeurParDefaut(type); // type non géré : on ne plante pas
    }

    // Vrai pour les types "simples" (int, String, Integer, List...), faux pour nos
    // propres classes
    private static boolean estTypeSimple(Class<?> type) {
        return type.isPrimitive() || type.getName().startsWith("java.");
    }

    // Crée un objet vide du type demandé
    private static Object creerObjet(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Impossible de créer un objet " + type.getSimpleName()
                            + " : il faut un constructeur sans argument",
                    e);
        }
    }

    // Remplit les attributs de l'objet avec les champs de même nom dans la requête
    private static void remplirObjet(Object objet, HttpServletRequest request) {
        for (Field f : objet.getClass().getDeclaredFields()) {

            // on ignore les constantes et les attributs static
            if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                continue;
            }
            // on ne gère que les types simples (String, int, double...)
            if (!estTypeSimple(f.getType())) {
                continue;
            }

            String valeur = request.getParameter(f.getName());
            if (valeur == null) {
                continue; // champ absent : l'attribut garde sa valeur actuelle
            }

            try {
                f.setAccessible(true); // nécessaire car les attributs sont private
                f.set(objet, convertir(valeur, f.getType()));
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(
                        "Impossible d'écrire l'attribut " + f.getName(), e);
            }
        }
    }

}