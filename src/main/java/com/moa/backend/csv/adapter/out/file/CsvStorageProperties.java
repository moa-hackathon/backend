package com.moa.backend.csv.adapter.out.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** CSV 파일 저장소 설정. 모든 CSV 경로는 baseDir 기준 상대 경로로 해석된다. */
@ConfigurationProperties("moa.csv")
public record CsvStorageProperties(@DefaultValue("./data/csv") String baseDir) {
}
