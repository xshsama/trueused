package com.xsh.trueused.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.xsh.trueused.inspection.dto.InspectionFlowDTO;
import com.xsh.trueused.inspection.dto.InspectionItemResultDTO;
import com.xsh.trueused.inspection.service.InspectionPdfService;

import static org.junit.jupiter.api.Assertions.assertTrue;

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

        byte[] pdfBytes = service.generateInspectionReportPdf(dto);
        assertTrue(pdfBytes.length > 4);
        assertTrue(pdfBytes[0] == '%' && pdfBytes[1] == 'P' && pdfBytes[2] == 'D' && pdfBytes[3] == 'F');
    }
}
