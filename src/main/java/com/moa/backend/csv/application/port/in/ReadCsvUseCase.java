package com.moa.backend.csv.application.port.in;

import com.moa.backend.csv.domain.CsvTable;

/** CSV 파일 읽기 유스케이스. */
public interface ReadCsvUseCase {

    CsvTable read(String path);
}
