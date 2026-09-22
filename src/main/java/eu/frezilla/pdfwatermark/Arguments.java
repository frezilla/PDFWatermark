package eu.frezilla.pdfwatermark;

import lombok.Getter;
import picocli.CommandLine.Option;

@Getter 
public class Arguments {
    
    @Option(names = {"--documentId"}, description = "identifiant du document", required = true)
    private String documentId;
    
    @Option(names = {"-?", "--help"}, description = "affiche l'aide", usageHelp = true)
    private boolean helpRequest;
    
    @Option(names = {"--input"}, description = "fichier pdf existant", paramLabel = "INPUTFILE", required = true)
    private String inputFile;
    
    @Option(names = {"--label"}, description = "texte principal du filigrane", required = true)
    private String label;
    
    @Option(names = {"--output"}, description = "fichier pdf à créer", paramLabel = "OUTPUTFILE", required = true)
    private String outputFile;
    
    @Option(names = {"--recipient"}, description = "destinataire du document", required = true)
    private String recipient;
    
}
