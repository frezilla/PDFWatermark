package eu.frezilla.pdfwatermark.watermark;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

public record Config(
        String label,
        String recipient,
        String documentId,
        LocalDateTime generationDate,
        float fontSize,
        float horizontalSpacing,
        float verticalSpacing,
        float rotationDegrees,
        float minimumOpacity,
        float maximumOpacity,
        float positionJitter,
        boolean addMicroText,
        boolean updateMetadata) {

    public Config {
        Objects.requireNonNull(label, "label ne doit pas être null");
        Objects.requireNonNull(recipient, "recipient ne doit pas être null");
        Objects.requireNonNull(documentId, "documentId ne doit pas être null");
        Objects.requireNonNull(generationDate, "generationDate ne doit pas être null");

        if (label.isBlank()) {
            throw new IllegalArgumentException("label ne doit pas être vide");
        }

        if (fontSize <= 0) {
            throw new IllegalArgumentException("fontSize doit être supérieur à 0");
        }

        if (horizontalSpacing <= 0 || verticalSpacing <= 0) {
            throw new IllegalArgumentException("Les espacements doivent être supérieurs à 0");
        }

        if (minimumOpacity < 0 || minimumOpacity > 1) {
            throw new IllegalArgumentException("minimumOpacity doit être compris entre 0 et 1");
        }

        if (maximumOpacity < 0 || maximumOpacity > 1) {
            throw new IllegalArgumentException("maximumOpacity doit être compris entre 0 et 1");
        }

        if (minimumOpacity > maximumOpacity) {
            throw new IllegalArgumentException("minimumOpacity ne peut pas dépasser maximumOpacity");
        }

        if (positionJitter < 0) {
            throw new IllegalArgumentException("positionJitter ne peut pas être négatif");
        }
    }
    
    public static Config createDefaultConfig(
                String label,
                String recipient,
                String documentId
        ) {
            return new Config(
                    label,
                    recipient,
                    documentId,
                    LocalDateTime.now(ZoneId.systemDefault()),
                    24.0f,
                    245.0f,
                    145.0f,
                    35.0f,
                    0.07f,
                    0.16f,
                    16.0f,
                    true,
                    true
            );
        }

}
