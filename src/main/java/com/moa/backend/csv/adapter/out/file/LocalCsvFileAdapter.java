package com.moa.backend.csv.adapter.out.file;

import com.moa.backend.csv.application.port.out.CsvFilePort;
import com.moa.backend.csv.domain.CsvTable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/** 로컬 파일 시스템에 CSV를 읽고 쓰는 어댑터. */
@Component
public class LocalCsvFileAdapter implements CsvFilePort {

    private static final char BOM = '﻿';

    private final Path baseDir;

    public LocalCsvFileAdapter(CsvStorageProperties properties) {
        this.baseDir = Path.of(properties.baseDir()).toAbsolutePath().normalize();
    }

    @Override
    public CsvTable load(String path) {
        Path target = resolve(path);
        if (!Files.isRegularFile(target)) {
            throw new IllegalArgumentException("CSV 파일을 찾을 수 없습니다: " + path);
        }
        try (BufferedReader reader = Files.newBufferedReader(target, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {
            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new IllegalArgumentException("CSV 파일에 헤더가 없습니다: " + path);
            }
            List<String> headers = new ArrayList<>(records.get(0).toList());
            String first = headers.get(0);
            if (!first.isEmpty() && first.charAt(0) == BOM) {
                headers.set(0, first.substring(1));
            }
            List<List<String>> rows = new ArrayList<>(records.size() - 1);
            for (CSVRecord record : records.subList(1, records.size())) {
                rows.add(record.toList());
            }
            return new CsvTable(headers, rows);
        } catch (IOException e) {
            throw new UncheckedIOException("CSV 파일을 읽는 중 오류가 발생했습니다: " + path, e);
        }
    }

    @Override
    public void save(String path, CsvTable table, boolean overwrite) {
        Path target = resolve(path);
        if (Files.exists(target) && !overwrite) {
            throw new IllegalStateException("이미 존재하는 파일입니다 (overwrite=true로 덮어쓰기 가능): " + path);
        }
        Path temp = null;
        try {
            Files.createDirectories(target.getParent());
            temp = Files.createTempFile(target.getParent(), ".csv-", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                printer.printRecord(table.headers());
                printer.printRecords(table.rows());
            }
            move(temp, target);
        } catch (IOException e) {
            throw new UncheckedIOException("CSV 파일을 쓰는 중 오류가 발생했습니다: " + path, e);
        } finally {
            deleteQuietly(temp);
        }
    }

    /** baseDir 기준으로 경로를 해석하고, baseDir 밖을 가리키면 거부한다. */
    private Path resolve(String path) {
        Path relative;
        try {
            relative = Path.of(path);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("올바르지 않은 경로입니다: " + path, e);
        }
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("절대 경로는 허용되지 않습니다: " + path);
        }
        Path target = baseDir.resolve(relative).normalize();
        if (!target.startsWith(baseDir) || target.equals(baseDir)) {
            throw new IllegalArgumentException("CSV 저장 디렉터리 밖의 경로는 허용되지 않습니다: " + path);
        }
        return target;
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일 정리 실패는 무시한다.
        }
    }
}
