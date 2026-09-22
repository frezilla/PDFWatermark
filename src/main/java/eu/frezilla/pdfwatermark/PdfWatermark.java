package eu.frezilla.pdfwatermark;

import eu.frezilla.pdfwatermark.watermark.Config;
import eu.frezilla.pdfwatermark.watermark.WatermarkGenerator;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

public class PdfWatermark {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfWatermark.class);

    public static void main(String[] args) throws Exception {
        PrintStream ps = System.out;
        
        Arguments arguments = new Arguments();
        try {
            new CommandLine(arguments).parseArgs(args);
            if (arguments.isHelpRequest()) {
                CommandLine.usage(arguments, ps);
                System.exit(0);
            }
            
            String documentId = arguments.getDocumentId();
            String inputFile = arguments.getInputFile();
            String label = arguments.getLabel();
            String ouputFile = arguments.getOutputFile();
            String recipient = arguments.getRecipient();
            
            LOGGER.info(
                    String.format(
                            """
                            Arguments de lancement du traitement :
                                Fichier source : %s
                                Fichier destination : %s
                                Texte principal du filigrane : %s
                                Destinataire du documenent : %s
                                Identifiant du document : %s
                            """,
                            inputFile,
                            ouputFile,
                            label, 
                            recipient,
                            documentId
                    )
            );
            
            Config config = Config.createDefaultConfig(label, recipient, documentId);
            
            try {
                WatermarkGenerator.getInstance().addWatermark(
                    Path.of("input.pdf"), 
                    Path.of("output.pdf"), 
                    config
                ); 
            } catch (IOException e) {
                LOGGER.error("Une erreur bloquante a été détectée au cours du traitement", e);
                ps.println("Une erreur bloquate a été détectée au cours du traitement; consultez les logs pour obtenir plus d'informations.");
                System.exit(-1);
            }
        } catch (ParameterException e) {
            CommandLine.usage(arguments, ps);
            System.exit(-1);
        }
    }
}