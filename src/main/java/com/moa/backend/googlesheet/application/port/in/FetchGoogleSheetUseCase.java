package com.moa.backend.googlesheet.application.port.in;

import com.moa.backend.googlesheet.domain.GoogleSheetRef;
import com.moa.backend.googlesheet.domain.SheetTable;

public interface FetchGoogleSheetUseCase {

    /**
     * 공개된 Google 시트 탭 하나를 가져온다.
     *
     * @param sheetUrl 시트 공유 링크 (https://docs.google.com/spreadsheets/d/{ID}/edit#gid={탭ID}) 또는 시트 ID
     */
    Result fetch(String sheetUrl);

    record Result(GoogleSheetRef ref, SheetTable table) {
    }
}
