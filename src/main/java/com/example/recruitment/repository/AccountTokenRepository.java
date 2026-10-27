package com.example.recruitment.repository;

import com.example.recruitment.entity.AccountToken;
import com.example.recruitment.entity.enums.AccountTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

	Optional<AccountToken> findByTokenHashAndPurpose(String tokenHash, AccountTokenPurpose purpose);

	@Modifying
	@Query("update AccountToken t set t.usedAt = :now where t.user.id = :userId and t.purpose = :purpose and t.usedAt is null")
	int invalidateActiveByUserIdAndPurpose(@Param("userId") Long userId,
										   @Param("purpose") AccountTokenPurpose purpose,
										   @Param("now") LocalDateTime now);
}
