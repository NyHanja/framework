package mg.itu.session;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Session {
    private final Map<String, Object> attributs = new ConcurrentHashMap<>();
    private final HttpSession hs;     
    private final String id;          

    public Session(HttpSession hs) {
        this.hs = hs;
        this.id = hs.getId();
    }

    public String getId() {
        return id;
    }

    public Object get(String cle) {
        return cle == null ? null : attributs.get(cle);
    }

    public void set(String cle, Object valeur) {
        if (cle == null) {
            throw new IllegalArgumentException("La cle de session ne peut pas etre null");
        }
        if (valeur == null) {
            attributs.remove(cle);          
        } else {
            attributs.put(cle, valeur);
        }
    }

    public void remove(String cle) {
        if (cle != null) {
            attributs.remove(cle);
        }
    }
    public void invalider() {
        attributs.clear();
        try {
            hs.invalidate();
        } catch (IllegalStateException e) {
            // déjà invalidée : rien à faire
        }
    }
}