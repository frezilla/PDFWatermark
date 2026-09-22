package eu.frezilla.pdfwatermark;

import java.nio.file.Path;

public class PdfWatermark {

    public static void main(String[] args) throws Exception {
        WatermarkConfig config = WatermarkConfig.createDefaultConfig("Confidentiel", "BaseRights", "9782330109646");
        PdfWatermarkGenerator.getInstance().addWatermark(
                Path.of("input.pdf"), 
                Path.of("output.pdf"), 
                config
        );

    }
}