package eu.frezilla.pdfwatermark.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;

final class Utils {
    
    private Utils() {}
    
    /**
     * Créé la graine alétoire de la page du document.
     * 
     * @param fingerprint
     * @param documentId
     * @param pageNumber
     * @return 
     */
    public static long createSeed(String fingerprint, String documentId, int pageNumber) {
        String value = fingerprint + "|" + documentId + "|" + pageNumber;
        long result = 1125899906842597L;
        for (int index = 0; index < value.length(); index++) {
            result = 31L * result + value.charAt(index);
        }
        return result;
    }    
    
    /**
     * Interpolation des valeurs.
     * 
     * @param min
     * @param max
     * @param factor
     * @return 
     */
    public static float interpolate(float  min, float  max, float factor) {
        return min + (max - min) * factor;
    }
    
    /**
     * Retourne une valeur aléatoire comprise entre deux valeurs.
     * 
     * @param random
     * @param minimum
     * @param maximum
     * @return 
     */
    public static float randomRange(Random random, float minimum, float maximum) {
        Objects.requireNonNull(random, "random ne doit pas être null");
        return minimum + random.nextFloat() * (maximum - minimum);
    }    

    /**
     * Remplace les caractères incompatibles avec les polices standards PDF.
     *
     * @param value
     * @return
     */
    public static String sanitizeForStandardFont(String value) {
        if (value == null) {
            return StringUtils.EMPTY;
        }

        return value
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('ë', 'e')
                .replace('à', 'a')
                .replace('â', 'a')
                .replace('ä', 'a')
                .replace('ù', 'u')
                .replace('û', 'u')
                .replace('ü', 'u')
                .replace('ô', 'o')
                .replace('ö', 'o')
                .replace('î', 'i')
                .replace('ï', 'i')
                .replace('ç', 'c')
                .replace('É', 'E')
                .replace('È', 'E')
                .replace('Ê', 'E')
                .replace('À', 'A')
                .replace('Ç', 'C')
                .replace('œ', 'o')
                .replace('Œ', 'O')
                .replace('’', '\'')
                .replace('–', '-')
                .replace('—', '-');
    }
    
    /**
     * Valide les chemins des ressources.
     * 
     * @param inputFile
     * @param outputFile
     * @throws IOException 
     */
    public static void validatePaths(Path inputFile, Path outputFile) throws IOException {
        Objects.requireNonNull(inputFile, "inputFile ne doit pas être null");
        Objects.requireNonNull(outputFile, "outputFile ne doit pas être null");
        
        if (!Files.exists(inputFile)) {
            throw new IOException("Le fichier source n'existe pas");
        }
        
        if (!Files.isRegularFile(inputFile)) {
            throw new IOException("Le chemin source n'est pas un fichier");
        }
        
        if (inputFile.toAbsolutePath().normalize().equals(outputFile.toAbsolutePath().normalize())) {
            throw new IOException("Le fichier de sortie doit être différent du fichier source");
        }
    }    
    
}