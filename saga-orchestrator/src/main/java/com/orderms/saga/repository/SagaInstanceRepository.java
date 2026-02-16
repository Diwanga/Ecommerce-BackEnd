package com.orderms.saga.repository;

import com.orderms.saga.entity.SagaInstance;
import com.orderms.saga.entity.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, Long> {

    Optional<SagaInstance> findBySagaId(String sagaId);

    Optional<SagaInstance> findByOrderId(Long orderId);

    List<SagaInstance> findByStatus(SagaStatus status);
}