package com.ivandejesus.prueba_java.application.services;

import com.ivandejesus.prueba_java.domain.models.DuplicateMatch;
import com.ivandejesus.prueba_java.infrastructure.helpers.DuplicateHelper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.ivandejesus.prueba_java.domain.models.Contacts;
import com.ivandejesus.prueba_java.domain.ports.in.DuplicateService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
public class DuplicateServiceImpl implements DuplicateService {

    private static final DataFormatter FORMATTER = new DataFormatter(Locale.ROOT);
    private final DuplicateHelper duplicateHelper;

    public DuplicateServiceImpl(DuplicateHelper duplicateHelper) {
        this.duplicateHelper = duplicateHelper;
    }

    public byte[] findMatchs(InputStream in, String sheetName) {
        List<Contacts> contacts = this.transformModel(in, sheetName);
        System.out.println(contacts.size());
        List<DuplicateMatch> matches = this.detectDuplicates(contacts);

        return this.exportMatchesToExcel(matches);
    }

    private List<Contacts> transformModel(InputStream in, String sheetName) {
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = (sheetName != null && !sheetName.isBlank())
                    ? wb.getSheet(sheetName)
                    : (wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null);

            if (sheet == null) return Collections.emptyList();

            // 1) Leer encabezados y mapear nombre -> índice
            int headerRowIdx = sheet.getFirstRowNum();
            Row header = sheet.getRow(headerRowIdx);
            if (header == null) return Collections.emptyList();

            Map<String, Integer> colIndex = new HashMap<>();
            short lastHeaderCell = header.getLastCellNum();
            for (int c = 0; c < lastHeaderCell; c++) {
                Cell hc = header.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (hc == null) continue;
                String key = FORMATTER.formatCellValue(hc).trim().toLowerCase();
                if (!key.isEmpty()) colIndex.put(key, c);
            }

            // Columnas esperadas
            Integer iContactId = colIndex.get("contactid");
            Integer iName = colIndex.get("name");
            Integer iName1 = colIndex.get("name1");
            Integer iEmail = colIndex.get("email");
            Integer iPostalZip = colIndex.get("postalzip");
            Integer iAddress = colIndex.get("address");

            // 2) Reservar capacidad aproximada
            int firstDataRow = headerRowIdx + 1;
            int lastRow = sheet.getLastRowNum();
            int estimated = Math.max(0, lastRow - firstDataRow + 1);
            List<Contacts> out = new ArrayList<>(estimated);

            // 3) Iterar filas de datos
            for (int r = firstDataRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Contacts c = new Contacts();

                // id (Long)
                if (iContactId != null) {
                    String v = duplicateHelper.getCellString(row, iContactId);
                    Long id = duplicateHelper.parseLongSafe(v);
                    if (id == null) {
                        id = duplicateHelper.getNumericLong(row, iContactId);
                    }
                    c.setId(id);
                }

                // firstName / lastName
                c.setFirstName(iName != null ? duplicateHelper.getCellString(row, iName) : null);
                c.setLastName(iName1 != null ? duplicateHelper.getCellString(row, iName1) : null);

                // email
                String email = (iEmail != null ? duplicateHelper.getCellString(row, iEmail) : null);
                c.setEmail(email != null ? email.trim() : null);

                // address
                c.setAddress(iAddress != null ? duplicateHelper.getCellString(row, iAddress) : null);

                // zipCode (int)
                if (iPostalZip != null) {
                    String zipTxt = duplicateHelper.getCellString(row, iPostalZip).replaceAll("[^0-9-]", "").trim();
                    c.setZipCode(duplicateHelper.parseIntSafe(zipTxt));
                }

                out.add(c);
            }

            return out;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<DuplicateMatch> detectDuplicates(List<Contacts> contacts) {
        if (contacts == null || contacts.size() < 2) return Collections.emptyList();

        List<DuplicateMatch> out = new ArrayList<>();

        // Comparación 1 a N
        for (int i = 0; i < contacts.size(); i++) {
            Contacts source = contacts.get(i);
            if (source == null || source.getId() == null) continue;

            for (int j = i + 1; j < contacts.size(); j++) {
                Contacts target = contacts.get(j);
                if (target == null || target.getId() == null) continue;

                int precision = 0;

                // name (firstName)
                String firstNameMatch = (duplicateHelper.equalsNormalized(source.getFirstName(), target.getFirstName())) ? "SI" : "NO";
                if ("SI".equals(firstNameMatch)) precision++;

                // name1 (lastName)
                String lastNameMatch = (duplicateHelper.equalsNormalized(source.getLastName(), target.getLastName())) ? "SI" : "NO";
                if ("SI".equals(lastNameMatch)) precision++;

                // email
                String emailMatch = (duplicateHelper.equalsNormalized(source.getEmail(), target.getEmail())) ? "SI" : "NO";
                if ("SI".equals(emailMatch)) precision++;

                // postalZip (zipCode)
                String zipMatch = (source.getZipCode() == target.getZipCode()) ? "SI" : "NO";
                if ("SI".equals(zipMatch)) precision++;

                // address
                String addressMatch = (duplicateHelper.equalsNormalized(source.getAddress(), target.getAddress())) ? "SI" : "NO";
                if ("SI".equals(addressMatch)) precision++;

                // Si hay coincidencia en al menos 1 campo, agregar a la lista de coincidencias
                if (precision > 0) {
                    out.add(new DuplicateMatch(
                            source.getId(),
                            target.getId(),
                            precision,
                            firstNameMatch,
                            lastNameMatch,
                            emailMatch,
                            zipMatch,
                            addressMatch
                    ));
                }
            }
        }

        return out;
    }

    private static void addPairsFromBlocks(Map<String, List<Integer>> block,
                                           List<int[]> candidatePairs,
                                           Set<Long> seenPairs) {
        for (List<Integer> idxs : block.values()) {
            int n = idxs.size();
            if (n < 2) continue;
            for (int a = 0; a < n; a++) {
                for (int b = a + 1; b < n; b++) {
                    int i = idxs.get(a), j = idxs.get(b);
                    int min = Math.min(i, j);
                    int max = Math.max(i, j);
                    long key = (((long) min) << 32) ^ (long) max;
                    if (seenPairs.add(key)) {
                        candidatePairs.add(new int[]{min, max});
                    }
                }
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ------------ 3) exportar resultados a Excel (.xlsx) ------------
    private byte[] exportMatchesToExcel(List<DuplicateMatch> matches) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            wb.setCompressTempFiles(true);
            Sheet sh = wb.createSheet("matches");

            // header
            Row h = sh.createRow(0);
            h.createCell(0).setCellValue("sourceId");
            h.createCell(1).setCellValue("targetId");
            h.createCell(2).setCellValue("precision");
            h.createCell(3).setCellValue("name");
            h.createCell(4).setCellValue("name1");
            h.createCell(5).setCellValue("email");
            h.createCell(6).setCellValue("postalZip");
            h.createCell(7).setCellValue("address");

            int r = 1;
            for (DuplicateMatch m : matches) {
                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(m.getSourceId() == null ? "" : String.valueOf(m.getSourceId()));
                row.createCell(1).setCellValue(m.getTargetId() == null ? "" : String.valueOf(m.getTargetId()));
                row.createCell(2).setCellValue(m.getPrecision());
                row.createCell(3).setCellValue(m.getFirstNameMatch());
                row.createCell(4).setCellValue(m.getLastNameMatch());
                row.createCell(5).setCellValue(m.getEmailMatch());
                row.createCell(6).setCellValue(m.getZipMatch());
                row.createCell(7).setCellValue(m.getAddressMatch());
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                wb.write(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
