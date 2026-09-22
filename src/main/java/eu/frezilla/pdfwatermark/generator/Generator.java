package eu.frezilla.pdfwatermark.generator;

import static eu.frezilla.pdfwatermark.generator.Utils.createSeed;
import static eu.frezilla.pdfwatermark.generator.Utils.interpolate;
import static eu.frezilla.pdfwatermark.generator.Utils.randomRange;
import static eu.frezilla.pdfwatermark.generator.Utils.sanitizeForStandardFont;
import static eu.frezilla.pdfwatermark.generator.Utils.validatePaths;
import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Generator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    private final DateTimeFormatter date_format;
    private final PDFont main_font;
    private final PDFont micro_font;

    private Generator() {
        this.date_format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.FRANCE);
        this.main_font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        this.micro_font = new PDType1Font(Standard14Fonts.FontName.COURIER);
    }
    
    /**
     * Ajoute une empreinte dans les quatres coins de la page.
     * 
     * @param cs  Flux PDF
     * @param page Page PDF
     * @param pageNumber Numéro de la page
     * @param fingerprint Contenu de l'empreinte
     * @param gs Etat graphique sur lequel l'empreinte va être dessinée
     * @Throws IOException
     */
    private void addCornerFingerprints(PDPageContentStream cs, PDPage page, int pageNumber, String fingerprint, PDExtendedGraphicsState gs) throws IOException {
        PDRectangle box = page.getMediaBox();

        float left = box.getLowerLeftX() + 12.0f;
        float bottom = box.getLowerLeftY() + 12.0f;
        float right = box.getUpperRightX() - 12.0f;
        float top = box.getUpperRightY() - 12.0f;

        String text = sanitizeForStandardFont("DOC:" + fingerprint + " PAGE:" + pageNumber);

        cs.setGraphicsStateParameters(gs);
        cs.setNonStrokingColor(new Color(66, 75, 86));

        float fontSize = 4.5f;
        float width = micro_font.getStringWidth(text) / 1000.0f * fontSize;

        drawSimpleText(cs, text, left, bottom, fontSize);
        drawSimpleText(cs, text, right - width, bottom, fontSize);
        drawSimpleText(cs, text, left, top, fontSize);
        drawSimpleText(cs, text, right - width, top, fontSize);
    }
    
    /**
     * Ajoute un filigrane (et des méta-données si spécifié) au document
     * 
     * @param inputFile
     * @param outputFile
     * @param config
     * @throws IOException 
     */
    public void addWatermark(Path inputFile, Path outputFile, Config config) throws IOException {
        validatePaths(inputFile, outputFile);
        Objects.requireNonNull(config, "config ne doit être null");
        
        Path parent = outputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        
        try (RandomAccessReadBufferedFile source = new RandomAccessReadBufferedFile(inputFile)) {
            PDDocument document = Loader.loadPDF(source);
            
            if (document.isEncrypted()) {
                throw new IOException("Le pdf est chiffré. Le filigrane ne peut pas être ajouté");
            }
            
            String fingerprint = createDocumentFingerprint(inputFile, config.documentId());
            
            int pageNumber = 0;
            for (PDPage page : document.getPages()) {
                pageNumber++;
                
                addWaterMarkToPage(
                        document,
                        page,
                        pageNumber,
                        fingerprint,
                        config
                );
            }
            
            if (config.updateMetadata()) {
                updateMetadatas(document, fingerprint, config);
            }
            
            document.save(outputFile.toFile());
        }

    }
    
    /**
     * Ajoute le filigrane sur une page du document.
     * 
     * @param document
     * @param page
     * @param pageNumber
     * @param fingerprint
     * @param cfg 
     */
    private void addWaterMarkToPage(PDDocument document, PDPage page, int pageNumber, String fingerprint, Config cfg) throws IOException {
        PDRectangle mediaBox = page.getMediaBox();

        // La graine dépend du document et du numéro de page
        Random random = new Random(
                createSeed(
                        fingerprint, 
                        cfg.documentId(), 
                        pageNumber
                )
        );
        
        float pageWidth = mediaBox.getWidth();
        float pageHeight = mediaBox.getHeight();
        float originX = mediaBox.getLowerLeftX();
        float originY = mediaBox.getLowerLeftY();
        
        // Plusieurs états graphiques sont créés pour géré des niveaux d'opacité différents
        PDExtendedGraphicsState lowOpacity = createGraphicsState(cfg.minimumOpacity());
        PDExtendedGraphicsState mediumOpacity = createGraphicsState(
                interpolate(
                        cfg.minimumOpacity(),
                        cfg.maximumOpacity(),
                        0.55f
                )
        );
        PDExtendedGraphicsState highOpacity = createGraphicsState(cfg.maximumOpacity());

        String visibleText = buildVisibleText(cfg);
        String microText = buildMicroText(cfg, fingerprint, pageNumber);

        float rotationRadians = (float) Math.toRadians(cfg.rotationDegrees());
        
        // Ajoute le filigrane à la page
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
        )) {
            contentStream.saveGraphicsState();

            // Gris légèrement bleuté.
            contentStream.setNonStrokingColor(new Color(77, 89, 104));

            int row = 0;

            for (float y = originY - cfg.verticalSpacing(); y <= originY + pageHeight + cfg.verticalSpacing(); y += cfg.verticalSpacing()) {
                // Décalage d'une ligne sur deux afin d'éviter une grille parfaitement régulière.
                float rowOffset = (row % 2 == 0) ? 0 : cfg.horizontalSpacing() / 2.0f;

                int column = 0;
                for (float x = originX - cfg.horizontalSpacing(); x <= originX + pageWidth + cfg.horizontalSpacing(); x += cfg.horizontalSpacing()) {

                    float jitterX = randomRange(random, -cfg.positionJitter(), cfg.positionJitter());
                    float jitterY = randomRange(random, -cfg.positionJitter(), cfg.positionJitter());
                    float angleVariation = (float) Math.toRadians(randomRange(random, -2.2f, 2.2f));
                    float textX = x + rowOffset + jitterX;
                    float textY = y + jitterY;

                    PDExtendedGraphicsState graphicsState =
                            selectGraphicsState(
                                    row,
                                    column,
                                    lowOpacity,
                                    mediumOpacity,
                                    highOpacity
                            );

                    contentStream.setGraphicsStateParameters(graphicsState);
                    drawRotatedCenteredText(
                            contentStream,
                            main_font,
                            cfg.fontSize(),
                            visibleText,
                            textX,
                            textY,
                            rotationRadians + angleVariation
                    );

                    // Ajout d'une seconde couche de microtexte positionnée près du filigrane principal
                    if (cfg.addMicroText()) {
                        contentStream.setGraphicsStateParameters(lowOpacity);
                        drawRotatedCenteredText(
                                contentStream,
                                micro_font,
                                5.5f,
                                microText,
                                textX + 12.0f,
                                textY - 12.0f,
                                rotationRadians + angleVariation
                        );
                    }

                    column++;
                }

                row++;
            }

            // Ajout d'une empreinte dans les coins du document.
            addCornerFingerprints(
                    contentStream,
                    page,
                    pageNumber,
                    fingerprint,
                    lowOpacity
            );

            contentStream.restoreGraphicsState();
        }
    }
    
    /**
     * Construit le micro-texte du filigrane.
     * @param config
     * @param fingerprint
     * @param pageNumber
     * @return 
     */
    private String buildMicroText(Config config, String fingerprint, int pageNumber) {
        return config.documentId() + " -" + fingerprint + " - p" + pageNumber + " - " + config.generationDate().format(date_format);
    }
    
    /**
     * Construit le texte principal du filigrane.
     * 
     * @param config
     * @return 
     */
    private String buildVisibleText(Config config) {
        return config.label() + " | " + config.recipient() + " | " + config.documentId();
    }    

    /**
     * Génère une empreinte sur 16 caractères à partir du fichier source et de 
     * l'identifiant fonctionnel
     * 
     * @param inputFile
     * @param documentId
     * @return
     * @throws IOException 
     */
    private String createDocumentFingerprint(Path inputFile, String documentId) throws IOException {
       try {
           MessageDigest digest = MessageDigest.getInstance("SHA-256");
           digest.update(documentId.getBytes(StandardCharsets.UTF_8));
           digest.update(Files.readAllBytes(inputFile));
           String hexaDeximal = HexFormat.of().formatHex(digest.digest());
           return StringUtils.substring(hexaDeximal, 0, 16).toUpperCase(Locale.ROOT);
       } catch (NoSuchAlgorithmException exception) {
           throw new IllegalStateException("L'algorihme SHA-256 n'est pas disponible", exception);
       }
    }
    
    /**
     * Créé un état graphique transparent en mode Multiply.
     * 
     * @param opacity
     * @return 
     */
    private static PDExtendedGraphicsState createGraphicsState(float opacity) {
        PDExtendedGraphicsState state = new PDExtendedGraphicsState();

        state.setNonStrokingAlphaConstant(opacity);
        state.setStrokingAlphaConstant(opacity);
        state.setBlendMode(BlendMode.MULTIPLY);
        state.getCOSObject().setBoolean(COSName.AIS, false);

        return state;
    }
    
    /**
     * Dessine un texte avec rotation centré autour d'un point.
     * @param contentStream
     * @param font
     * @param fontSize
     * @param text
     * @param centerX
     * @param centerY
     * @param angleRadians
     * @throws IOException 
     */
    private void drawRotatedCenteredText(
            PDPageContentStream contentStream,
            PDFont font,
            float fontSize,
            String text,
            float centerX,
            float centerY,
            float angleRadians
    ) throws IOException {
        String safeText = sanitizeForStandardFont(text);

        float textWidth = font.getStringWidth(safeText) / 1000.0f * fontSize;
        float textHeight = (font.getFontDescriptor().getAscent() - font.getFontDescriptor().getDescent()) / 1000.0f * fontSize;

        contentStream.beginText();
        contentStream.setFont(font, fontSize);

        Matrix rotation = Matrix.getRotateInstance(angleRadians, centerX, centerY);
        rotation.translate(-textWidth / 2.0f, -textHeight / 2.0f);

        contentStream.setTextMatrix(rotation);
        contentStream.showText(safeText);
        contentStream.endText();
    }
    
    /**
     * Dessine un texte simple aux coordonées indiquées.
     * 
     * @param cs
     * @param text
     * @param x
     * @param y
     * @param fontSize
     * @throws IOException 
     */
    private void drawSimpleText(PDPageContentStream cs, String text, float x, float y, float fontSize) throws IOException {
        cs.beginText();
        cs.setFont(micro_font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizeForStandardFont(text));
        cs.endText();
    }

    /**
     * Retourne l'instance unique du générateur de filigrane pour PDF.
     * 
     * @return 
     */
    public static Generator getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * "Sélectionne" l'état graphisue en fonction de la ligne et de la colonne.
     * 
     * @param row
     * @param column
     * @param low
     * @param medium
     * @param high
     * @return 
     */
    private PDExtendedGraphicsState selectGraphicsState(
            int row,
            int column,
            PDExtendedGraphicsState low,
            PDExtendedGraphicsState medium,
            PDExtendedGraphicsState high
    ) {
        int value = Math.floorMod(row + column, 3);

        return switch (value) {
            case 0 -> low;
            case 1 -> medium;
            default -> high;
        };
    }
    
    /**
     * Ajoute des informations de traçabilité aux métadonnées du PDF.
     * 
     * @param doc Document Pdf
     * @param fingerprint Empreinte du document
     * @param cfg Configuration du traitement
     */
    private void updateMetadatas(PDDocument doc, String fingerprint, Config cfg) {
        LOGGER.trace("Mise à jour des métadonnées du document");
        
        PDDocumentInformation information = doc.getDocumentInformation();
        information.setCustomMetadataValue("WatermarkLabel", cfg.label());
        information.setCustomMetadataValue("WatermarkRecipient", cfg.recipient());
        information.setCustomMetadataValue("WatermarkDocumentId", cfg.documentId());
        information.setCustomMetadataValue("WatermarkFingerprint", fingerprint);
        information.setCustomMetadataValue("WatermarkGenerationDate", cfg.generationDate().format(date_format));
    }

    private static class Holder {
        private static final Generator INSTANCE = new Generator();
    }

}