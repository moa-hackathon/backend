package com.moa.backend.event.repository;

import com.moa.backend.event.entity.Event;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

	/** 행사가 해당 동아리 소유일 때만 조회된다. 관리 API의 권한 확인에 쓴다. */
	Optional<Event> findByIdAndClubId(Long id, Long clubId);

	/** 동아리의 행사 중 행사 일시가 기준 시각보다 뒤인 것을 행사 일시 오름차순(같으면 id 순)으로 조회한다. */
	List<Event> findAllByClubIdAndEventAtAfterOrderByEventAtAscIdAsc(Long clubId, LocalDateTime now);
}
