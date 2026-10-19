package mg.itu.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MapFile {
    private String nom_fichier;   // sans l'extension : "photo"
    private String extension;     // sans le point : "png"
    private byte[] bytes;         // contenu du fichier
    private transient String dossier;   // renseigné par le framework (context-param uploadDir)

    public MapFile() {
    }

    public MapFile(String nom_fichier, String extension, byte[] bytes) {
        this.nom_fichier = nom_fichier;
        this.extension = extension;
        this.bytes = bytes;
    }

    public String getNom_fichier() { return nom_fichier; }
    public void setNom_fichier(String nom_fichier) { this.nom_fichier = nom_fichier; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public byte[] getBytes() { return bytes; }
    public void setBytes(byte[] bytes) { this.bytes = bytes; }

    public String getDossier() { return dossier; }
    public void setDossier(String dossier) { this.dossier = dossier; }

    // "photo" + "png" -> "photo.png" ; sans extension -> "photo"
    public String getNomComplet() {
        return (extension == null || extension.isEmpty()) ? nom_fichier : nom_fichier + "." + extension;
    }

    // Enregistre dans le dossier configuré (context-param "uploadDir" du web.xml)
    public Path enregistrer() throws IOException {
        if (dossier == null || dossier.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Dossier d'upload non configure : ajoutez le context-param 'uploadDir' dans web.xml");
        }
        return enregistrer(dossier);
    }

    // Enregistre dans un dossier donné ; ne remplace jamais un fichier existant
    public Path enregistrer(String dossierDestination) throws IOException {
        Path base = Paths.get(dossierDestination).toAbsolutePath().normalize();
        Files.createDirectories(base);

        Path cible = base.resolve(getNomComplet()).normalize();
        if (!cible.startsWith(base)) {
            throw new SecurityException("Nom de fichier refuse : " + getNomComplet());
        }

        // fichier déjà présent : photo.png -> photo_1.png -> photo_2.png ...
        int n = 1;
        while (Files.exists(cible)) {
            String suffixe = (extension == null || extension.isEmpty()) ? "" : "." + extension;
            cible = base.resolve(nom_fichier + "_" + n++ + suffixe).normalize();
        }

        Files.write(cible, bytes);
        return cible;
    }
}