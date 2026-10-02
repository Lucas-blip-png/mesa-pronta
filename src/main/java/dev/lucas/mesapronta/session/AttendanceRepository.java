package dev.lucas.mesapronta.session;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	long countBySessionId(Long sessionId);

	boolean existsBySessionIdAndUserId(Long sessionId, String userId);

	@Modifying
	@Query("delete from Attendance a where a.sessionId = :sessionId and a.userId = :userId")
	int deleteBySessionIdAndUserId(Long sessionId, String userId);

	@Query("select a.userId from Attendance a where a.sessionId = :sessionId order by a.id")
	List<String> findUserIdsBySessionId(Long sessionId);

}
