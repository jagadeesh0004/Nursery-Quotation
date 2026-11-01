package com.nursery.customer;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @Query("select c from Customer c where c.userId = :userId order by lower(c.name)")
    List<Customer> findForUser(@Param("userId") UUID userId);

    @Query("select c from Customer c where c.userId = :userId and (lower(c.name) like lower(concat('%', :search, '%')) or lower(coalesce(c.phone, '')) like lower(concat('%', :search, '%')) or lower(coalesce(c.address, '')) like lower(concat('%', :search, '%'))) order by lower(c.name)")
    List<Customer> findForUserWithSearch(@Param("userId") UUID userId, @Param("search") String search);

    Optional<Customer> findByIdAndUserId(Long id, UUID userId);

    List<Customer> findByUserIdAndNameIgnoreCase(UUID userId, String name);
}
