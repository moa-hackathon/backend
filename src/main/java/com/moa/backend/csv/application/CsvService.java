package com.moa.backend.csv.application;

import com.moa.backend.csv.application.port.in.ReadCsvUseCase;
import com.moa.backend.csv.application.port.in.WriteCsvUseCase;
import com.moa.backend.csv.application.port.out.CsvFilePort;
import com.moa.backend.csv.domain.CsvTable;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CsvService implements ReadCsvUseCase, WriteCsvUseCase {

    private final CsvFilePort csvFilePort;

    public CsvService(CsvFilePort csvFilePort) {
        this.csvFilePort = csvFilePort;
    }

    @Override
    public CsvTable read(String path) {
        validatePath(path);
        return csvFilePort.load(path);
    }

    @Override
    public int write(String path, CsvTable table, boolean overwrite) {
        validatePath(path);
        Objects.requireNonNull(table, "CSV 데이터가 없습니다.");
        csvFilePort.save(path, table, overwrite);
        return table.rows().size();
    }

    private void validatePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("CSV 파일 경로가 비어 있습니다.");
        }
        if (!path.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("CSV 파일 경로는 .csv로 끝나야 합니다: " + path);
        }
    }
}
