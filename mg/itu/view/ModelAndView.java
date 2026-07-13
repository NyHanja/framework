package mg.itu.view;

import java.util.*;

public class ModelAndView {
    private String nom_vue;
    private Map<String, Object> attributs;

    public ModelAndView(String nom_vue) {
        this.nom_vue = nom_vue;
        this.attributs = new HashMap<>();
    }

    public String getNom_vue() {
        return nom_vue;
    }

    public void setNom_vue(String nom_vue) {
        this.nom_vue = nom_vue;
    }

    public Map<String, Object> getAttributs() {
        return attributs;
    }

    public void setAttributs(Map<String, Object> attributs) {
        this.attributs = attributs;
    }

    public Object getAttribut(String nom_attribut) {
        if (this.attributs == null) {
            return null;
        }
        if (nom_attribut == null || nom_attribut.isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'attribut ne peut pas être null ou vide.");
        }
        if (!this.attributs.containsKey(nom_attribut)) {
            throw new IllegalArgumentException("L'attribut " + nom_attribut + " n'existe pas.");
        }
        return this.attributs.get(nom_attribut);
    }

    public void addAttribut(String nom_attribut, Object valeur) {
        if (nom_attribut == null || nom_attribut.isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'attribut ne peut pas être null ou vide.");
        }
        if (this.attributs.containsKey(nom_attribut)) {
            throw new IllegalArgumentException("L'attribut " + nom_attribut + " existe déjà.");
        }
        this.attributs.put(nom_attribut, valeur);
    }
}
