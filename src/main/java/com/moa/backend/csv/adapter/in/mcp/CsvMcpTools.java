package com.moa.backend.csv.adapter.in.mcp;

import com.moa.backend.csv.application.port.in.ReadCsvUseCase;
import com.moa.backend.csv.application.port.in.WriteCsvUseCase;
import com.moa.backend.csv.domain.CsvTable;
import com.moa.backend.mcp.McpToolSet;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** CSV 기능을 MCP 도구로 노출하는 인바운드 어댑터. */
@Component
public class CsvMcpTools implements McpToolSet {

    private final ReadCsvUseCase readCsvUseCase;
    private final WriteCsvUseCase writeCsvUseCase;

    public CsvMcpTools(ReadCsvUseCase readCsvUseCase, WriteCsvUseCase writeCsvUseCase) {
        this.readCsvUseCase = readCsvUseCase;
        this.writeCsvUseCase = writeCsvUseCase;
    }

    @Tool(name = "read_csv", description = """
            Read a UTF-8 CSV file from the server's CSV storage directory. \
            The first line is treated as the header. \
            Returns the headers, the number of data rows, and each row as an object keyed by header name.""")
    public ReadCsvResult readCsv(
            @ToolParam(description = "Path of the CSV file, relative to the CSV storage directory "
                    + "(e.g. 'reports/sales.csv'). Must end with .csv. Absolute paths and '..' are not allowed.")
            String path) {
        CsvTable table = readCsvUseCase.read(path);
        return new ReadCsvResult(path, table.headers(), table.rows().size(), table.toRecords());
    }

    @Tool(name = "write_csv", description = """
            Write a UTF-8 CSV file into the server's CSV storage directory. \
            Parent directories are created automatically. \
            Every row must have exactly as many values as there are headers. \
            Fails if the file already exists unless overwrite is true.""")
    public WriteCsvResult writeCsv(
            @ToolParam(description = "Path of the CSV file, relative to the CSV storage directory "
                    + "(e.g. 'reports/sales.csv'). Must end with .csv. Absolute paths and '..' are not allowed.")
            String path,
            @ToolParam(description = "Column names for the header line, in order.")
            List<String> headers,
            @ToolParam(description = "Data rows. Each row is a list of string values in the same order as headers.")
            List<List<String>> rows,
            @ToolParam(description = "true to replace an existing file, false to fail if the file already exists.")
            boolean overwrite) {
        int written = writeCsvUseCase.write(path, new CsvTable(headers, rows), overwrite);
        return new WriteCsvResult(path, written);
    }

    public record ReadCsvResult(String path, List<String> headers, int rowCount, List<Map<String, String>> records) {
    }

    public record WriteCsvResult(String path, int writtenRows) {
    }
}
