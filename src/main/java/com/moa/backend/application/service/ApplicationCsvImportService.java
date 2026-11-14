package com.moa.backend.application.service;

import com.moa.backend.application.csv.CsvDecoder;
import com.moa.backend.application.csv.CsvReadException;
import com.moa.backend.application.csv.FormCsvParser;
import com.moa.backend.application.csv.FormResponseRow;
import com.moa.backend.application.csv.ParsedFormCsv;
import com.moa.backend.application.dto.CsvRowFailureResponse;
import com.moa.backend.application.dto.CsvUploadResponse;
import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationSource;
import com.moa.backend.application.entity.ApplicationStatus;
import com.moa.backend.application.repository.ApplicationRepository;
import com.moa.backend.club.entity.Club;
import com.moa.backend.common.exception.ApiException;
import com.moa.backend.common.exception.ErrorCode;
import com.moa.backend.event.entity.Event;
import com.moa.backend.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicationCsvImportService {

	private final EventRepository eventRepository;
	private final ApplicationRepository applicationRepository;

	/**
	 * 구글폼 응답 CSV를 적재한다. 같은 파일이나 응답이 늘어난 파일을 다시 올려도 이미 있는 행은 건너뛴다.
	 * 적재 후 행사 전체의 확정/대기를 다시 매기고 마지막 업로드 시각을 갱신한다.
	 */
	@Transactional
	public CsvUploadResponse importCsv(Club club, Long eventId, byte[] content) {
		Event event = eventRepository.findByIdAndClubId(eventId, club.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.EVENT_NOT_FOUND));
		ParsedFormCsv parsed = parse(content);

		Set<String> knownHashes = new HashSet<>(applicationRepository.findRowHashesByEventId(event.getId()));
		List<Application> newApplications = new ArrayList<>();
		for (FormResponseRow row : parsed.rows()) {
			if (knownHashes.add(row.rowHash())) {
				newApplications.add(toApplication(event, row));
			}
		}
		int skipped = parsed.rows().size() - newApplications.size();
		applicationRepository.saveAll(newApplications);

		List<Application> all = applicationRepository.findAllByEventIdOrderByAppliedAtAscIdAsc(event.getId());
		ApplicationStatusAllocator.Result allocation = ApplicationStatusAllocator.allocate(all, event.getCapacity());

		LocalDateTime syncedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
		event.markSynced(syncedAt);

		List<CsvRowFailureResponse> failures = parsed.errors().stream()
				.map(CsvRowFailureResponse::from)
				.toList();
		return new CsvUploadResponse(parsed.totalRows(), newApplications.size(), skipped, failures,
				allocation.confirmedCount(), allocation.waitingCount(), syncedAt);
	}

	private ParsedFormCsv parse(byte[] content) {
		try {
			return FormCsvParser.parse(CsvDecoder.decode(content));
		} catch (CsvReadException e) {
			ErrorCode errorCode = switch (e.getReason()) {
				case EMPTY -> ErrorCode.CSV_EMPTY;
				case UNREADABLE -> ErrorCode.CSV_UNREADABLE;
				case COLUMN_MISSING -> ErrorCode.CSV_COLUMN_MISSING;
			};
			throw new ApiException(errorCode, e.getMessage());
		}
	}

	private Application toApplication(Event event, FormResponseRow row) {
		return Application.builder()
				.event(event)
				.name(row.name())
				.studentId(row.studentId())
				.contact(row.contact())
				// 재계산 전 임시 상태. 아래 ApplicationStatusAllocator가 바로 덮어쓴다.
				.status(ApplicationStatus.WAITING)
				.source(ApplicationSource.CSV)
				.answers(new LinkedHashMap<>(row.answers()))
				.rowHash(row.rowHash())
				.appliedAt(row.appliedAt())
				.build();
	}
}
