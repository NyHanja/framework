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

            // 1. Injection du contexte Spring
            if (applicationContext != null && p.getType().isAssignableFrom(applicationContext.getClass())) {
                arguments[i] = applicationContext;

                // 2. Binding + conversion de type
            } else {
                String nomChamp = p.isAnnotationPresent(Param.class)
                        ? p.getAnnotation(Param.class).value()
                        : p.getName();
                arguments[i] = convertir(request.getParameter(nomChamp), p.getType());
            }
        }
    }

    // public static void creerArguments(Method methode, Object[] arguments) {
    // for (int i = 0; i < methode.getParameters().length; i++) {
    // Parameter p = methode.getParameters()[i];
    // }
    // }

    // SANS REFLEXION

    // public static Map<String, Mapping> recupererUrlMapping(Utilitaire utilitaire)
    // throws Exception {
    // Map<String, Mapping> urlMapping = new HashMap<>();

    // List<Class<?>> classes = recupererClasses(utilitaire.getNom_package());

    // for (Class<?> classe : classes) {
    // for (Method method : classe.getDeclaredMethods()) {

    // if (method.isAnnotationPresent(UrlMapping.class)) {

    // String url = (String) method.getAnnotation(UrlMapping.class).value();

    // urlMapping.put(url, new Mapping(classe, method));
    // }
    // }
    // }

    // return urlMapping;
    // }

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

}