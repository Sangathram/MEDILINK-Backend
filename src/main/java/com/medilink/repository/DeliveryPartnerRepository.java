
package com.medilink.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.medilink.entity.DeliveryPartner;

public interface DeliveryPartnerRepository
        extends JpaRepository<DeliveryPartner, Long> {

    Optional<DeliveryPartner> findByEmail(String email);

    @Query("""
        SELECT dp
        FROM DeliveryPartner dp
        WHERE LOWER(TRIM(dp.email)) = LOWER(TRIM(:email))
        """)
    Optional<DeliveryPartner> findByNormalizedEmail(
            @Param("email") String email);

    List<DeliveryPartner> findByAvailableTrue();
}
