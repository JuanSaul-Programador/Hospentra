package com.hotel.domain.repository;

import com.hotel.domain.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(Long userId, String type);

    @Query("SELECT v FROM VerificationCode v JOIN v.user u WHERE LOWER(u.email) = LOWER(:email) AND v.code = :code AND v.type = :type AND v.used = false ORDER BY v.createdAt DESC")
    List<VerificationCode> findActiveCodesByEmailAndCodeAndType(
            @Param("email") String email,
            @Param("code") String code,
            @Param("type") String type);

    @Query("SELECT v FROM VerificationCode v JOIN v.user u WHERE LOWER(u.email) = LOWER(:email) AND v.type = :type AND v.used = false")
    List<VerificationCode> findActiveCodesByEmailAndType(
            @Param("email") String email,
            @Param("type") String type);
}
