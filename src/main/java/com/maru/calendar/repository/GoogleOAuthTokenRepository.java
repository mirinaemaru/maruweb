package com.maru.calendar.repository;

import com.maru.calendar.entity.GoogleOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleOAuthTokenRepository extends JpaRepository<GoogleOAuthToken, Long> {

    Optional<GoogleOAuthToken> findByUserIdentifier(String userIdentifier);

    void deleteByUserIdentifier(String userIdentifier);
}
