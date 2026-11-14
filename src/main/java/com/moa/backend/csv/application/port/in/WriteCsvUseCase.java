package com.moa.backend.csv.application.port.in;

import com.moa.backend.csv.domain.CsvTable;

/** CSV 파일 쓰기 유스케이스. */
public interface WriteCsvUseCase {

    /** @return 기록한 데이터 행 수 (헤더 제외) */
    int write(String path, CsvTable table, boolean overwrite);
}
