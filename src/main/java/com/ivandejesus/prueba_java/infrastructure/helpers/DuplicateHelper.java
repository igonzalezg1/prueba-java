package com.ivandejesus.prueba_java.infrastructure.helpers;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

import java.util.Locale;

@Component
public class DuplicateHelper {
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.ROOT);


    public String getCellString(Row row, Integer idx) {
        if (row == null || idx == null || idx < 0) return "";
        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        // DataFormatter respeta formato visible (ej. ZIP "01234")
        return FORMATTER.formatCellValue(cell).trim();
    }
    /**
     * Obtiene un valor Long de una celda Excel
     * @param row Fila de Excel
     * @param idx Índice de la celda
     * @return Valor Long o null si no es convertible
     */
    public Long getNumericLong(Row row, Integer idx) {
        if (row == null || idx == null || idx < 0) return null;

        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            double d = cell.getNumericCellValue();
            // Evita decimales tipo "1234.0"
            long l = (long) d;
            return l;
        }

        return parseLongSafe(FORMATTER.formatCellValue(cell).trim());
    }

    /**
     * Convierte de forma segura un String a Long
     * @param s String a convertir
     * @return Valor Long o null si no es convertible
     */
    public Long parseLongSafe(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            // Quita separadores comunes si vinieran desde Excel export
            String cleaned = s.replaceAll("[,\\s]", "");
            return Long.parseLong(cleaned);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Convierte de forma segura un String a int
     * @param s String a convertir
     * @return Valor int o 0 si no es convertible
     */
    public int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            String cleaned = s.replaceAll("[,\\s]", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * Obtiene un valor String de una celda Excel
     * @param row Fila de Excel
     * @param idx Índice de la celda
     * @return Valor String o null si está vacío
     */
    public String getStringValue(Row row, Integer idx) {
        if (row == null || idx == null || idx < 0) return null;

        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        return FORMATTER.formatCellValue(cell).trim();
    }

    /**
     * Obtiene un valor Double de una celda Excel
     * @param row Fila de Excel
     * @param idx Índice de la celda
     * @return Valor Double o null si no es convertible
     */
    public Double getNumericDouble(Row row, Integer idx) {
        if (row == null || idx == null || idx < 0) return null;

        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return cell.getNumericCellValue();
        }

        try {
            String value = FORMATTER.formatCellValue(cell).trim();
            String cleaned = value.replaceAll("[,\\s]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public String normalizeBasic(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase();
        // remueve acentos
        t = Normalizer.normalize(t, Normalizer.Form.NFD);
        t = t.replaceAll("\\p{M}+", "");
        // colapsa espacios
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    public String normalizeAlphaNum(String s) {
        String t = normalizeBasic(s);
        return t.replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    public String normalizeEmail(String s) {
        String t = normalizeBasic(s);
        // si quieres manejar "plus addressing":  user+tag@dominio -> user@dominio
        int at = t.indexOf('@');
        if (at > 0) {
            String user = t.substring(0, at);
            String dom  = t.substring(at + 1);
            int plus = user.indexOf('+');
            if (plus > -1) user = user.substring(0, plus);
            return user + "@" + dom;
        }
        return t;
    }

    public boolean equalsNormalized(String a, String b) {
        return normalizeBasic(a).equals(normalizeBasic(b));
    }
}
