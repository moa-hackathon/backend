package com.moa.backend.csv.application.port.out;

import com.moa.backend.csv.domain.CsvTable;

/** CSV 저장소 접근 포트. */
public interface CsvFilePort {

    CsvTable load(String path);

    void save(String path, CsvTable table, boolean overwrite);
}
