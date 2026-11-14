package com.moa.backend.googlesheet.application.port.out;

import com.moa.backend.googlesheet.domain.GoogleSheetRef;
import com.moa.backend.googlesheet.domain.SheetTable;

/** Google 시트 내용을 가져오는 곳(공개 CSV 내보내기, Sheets API 등)에 대한 출력 포트. */
public interface GoogleSheetPort {

    SheetTable fetch(GoogleSheetRef ref);
}
