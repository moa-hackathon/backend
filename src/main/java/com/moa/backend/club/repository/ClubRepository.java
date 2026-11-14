package com.moa.backend.club.repository;

import com.moa.backend.club.entity.Club;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {

	Optional<Club> findByInviteCode(String inviteCode);
}
