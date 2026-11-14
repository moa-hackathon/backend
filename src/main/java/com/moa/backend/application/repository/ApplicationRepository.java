package com.moa.backend.application.repository;

import com.moa.backend.application.entity.Application;
import com.moa.backend.application.entity.ApplicationStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

	/** 여러 행사의 신청 인원을 group by 한 번으로 센다. 해당 상태는 빼고 세며, 신청이 없는 행사는 결과에 없다. */
	@Query("select new com.moa.backend.application.repository.EventApplicantCount(a.event.id, count(a))"
			+ " from Application a"
			+ " where a.event.id in :eventIds and a.status <> :excludedStatus"
			+ " group by a.event.id")
	List<EventApplicantCount> countByEventIdsExcludingStatus(@Param("eventIds") Collection<Long> eventIds,
			@Param("excludedStatus") ApplicationStatus excludedStatus);
}
