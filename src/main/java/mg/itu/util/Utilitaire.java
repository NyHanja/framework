package mg.itu.util;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;
import java.lang.annotation.Annotation;

public class Utilitaire {

    public static List<Class<?>> getClasses(String packageName)
            throws Exception {

        List<Class<?>> classes = new ArrayList<>();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String path = packageName.replace('.', '/');

        URL resource = classLoader.getResource(path);

        if (resource == null) {
            throw new Exception(
                    "Package introuvable : " + packageName);
        }

        File directory = new File(resource.getFile());

        File[] files = directory.listFiles();

        if (files == null) {
            return classes;
        }

        for (File file : files) {

            if (file.getName().endsWith(".class")) {

                String className = packageName + "."
                        + file.getName().replace(".class", "");

                Class<?> clazz = Class.forName(className);

                classes.add(clazz);
            }
        }

        return classes;
    }

    public static List<Class<?>> getAnnotatedClasses(
            List<Class<?>> classes,
            Class<? extends Annotation> annotationClass) {

        List<Class<?>> result = new ArrayList<>();

        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(
                    annotationClass)) {

                result.add(clazz);
            }
        }

        return result;
    }
}