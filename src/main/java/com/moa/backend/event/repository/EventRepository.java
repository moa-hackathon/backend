package com.moa.backend.event.repository;

import com.moa.backend.event.entity.Event;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

	/** 행사가 해당 동아리 소유일 때만 조회된다. 관리 API의 권한 확인에 쓴다. */
	Optional<Event> findByIdAndClubId(Long id, Long clubId);
}
