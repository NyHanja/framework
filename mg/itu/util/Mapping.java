package mg.itu.util;

import java.lang.reflect.Method;

public class Mapping {
    private Class<?> classe;
    private Method methode;

    public Mapping(Class<?> classe, Method methode) {
        this.classe = classe;
        this.methode = methode;
    }

    public Mapping(){}

    public Class<?> getClasse() {
        return classe;
    }

    public void setClasse(Class<?> classe) {
        this.classe = classe;
    }

    public Method getMethode() {
        return methode;
    }

    public void setMethode(Method methode) {
        this.methode = methode;
    }

    
}
