package com.moa.backend.googlesheet.application;

import com.moa.backend.googlesheet.application.port.in.FetchGoogleSheetUseCase;
import com.moa.backend.googlesheet.application.port.out.GoogleSheetPort;
import com.moa.backend.googlesheet.domain.GoogleSheetRef;
import org.springframework.stereotype.Service;

@Service
public class GoogleSheetService implements FetchGoogleSheetUseCase {

    private final GoogleSheetPort googleSheetPort;

    public GoogleSheetService(GoogleSheetPort googleSheetPort) {
        this.googleSheetPort = googleSheetPort;
    }

    @Override
    public Result fetch(String sheetUrl) {
        GoogleSheetRef ref = GoogleSheetRef.parse(sheetUrl);
        return new Result(ref, googleSheetPort.fetch(ref));
    }
}
