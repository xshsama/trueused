package com.xsh.trueused.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xsh.trueused.dto.InspectionFlowDTO;
import com.xsh.trueused.dto.InspectionItemResultDTO;

public class InspectionPdfServiceTest {

    @Test
    public void testGeneratePdf() throws Exception {
        InspectionPdfService service = new InspectionPdfService();
        
        InspectionFlowDTO dto = new InspectionFlowDTO();
        dto.setInspectionId(1L);
        dto.setProductTitle("Test Product");
        dto.setGrade("A");
        dto.setUpdatedAt(Instant.now());
        dto.setResultSummary("Test Summary");
        
        List<InspectionItemResultDTO> items = new ArrayList<>();
        InspectionItemResultDTO item1 = new InspectionItemResultDTO();
        item1.setItemName("Screen");
        item1.setItemDescription("Check screen");
        item1.setStatus("PASSED");
        items.add(item1);
        
        dto.setItems(items);
        
        System.out.println("Generating PDF...");
        try (InputStream is = new ByteArrayInputStream(service.generateInspectionReportPdf(dto))) {
            File f = new File("test_report.pdf");
            try (FileOutputStream fos = new FileOutputStream(f)) {
                is.transferTo(fos);
            }
            System.out.println("PDF generated at " + f.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
