package com.orderms.saga.repository;

import com.orderms.saga.entity.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SagaStepRepository extends JpaRepository<SagaStep, Long> {

    List<SagaStep> findBySagaId(String sagaId);

    List<SagaStep> findBySagaIdOrderByCreatedAtAsc(String sagaId);
}