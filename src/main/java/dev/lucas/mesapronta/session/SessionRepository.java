package dev.lucas.mesapronta.session;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SessionRepository extends JpaRepository<Session, Long> {

	/** SELECT ... FOR UPDATE: serializes concurrent joins on the same session. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from Session s where s.id = :id")
	Optional<Session> findByIdForUpdate(Long id);

	/**
	 * Sessions that still owe a 24h or 1h reminder. SKIP LOCKED lets several app
	 * instances poll at once without sending the same reminder twice.
	 */
	@Query(value = """
			select * from session
			where starts_at > :now
			  and ((not reminded_24h and starts_at <= :in24h)
			    or (not reminded_1h and starts_at <= :in1h))
			order by starts_at
			limit 50
			for update skip locked
			""", nativeQuery = true)
	List<Session> lockDueReminders(Instant now, Instant in24h, Instant in1h);

}
