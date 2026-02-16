package com.orderms.saga.controller;

import com.orderms.saga.entity.SagaInstance;
import com.orderms.saga.entity.SagaStep;
import com.orderms.saga.repository.SagaInstanceRepository;
import com.orderms.saga.repository.SagaStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sagas")
@RequiredArgsConstructor
public class SagaController {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;

    @GetMapping("/{sagaId}")
    public ResponseEntity<SagaInstance> getSaga(@PathVariable String sagaId) {
        log.info("Fetching saga: {}", sagaId);

        return sagaInstanceRepository.findBySagaId(sagaId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<SagaInstance> getSagaByOrderId(@PathVariable Long orderId) {
        log.info("Fetching saga for orderId: {}", orderId);

        return sagaInstanceRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{sagaId}/steps")
    public ResponseEntity<List<SagaStep>> getSagaSteps(@PathVariable String sagaId) {
        log.info("Fetching steps for saga: {}", sagaId);

        List<SagaStep> steps = sagaStepRepository.findBySagaIdOrderByCreatedAtAsc(sagaId);
        return ResponseEntity.ok(steps);
    }

    @GetMapping
    public ResponseEntity<List<SagaInstance>> getAllSagas() {
        log.info("Fetching all sagas");

        List<SagaInstance> sagas = sagaInstanceRepository.findAll();
        return ResponseEntity.ok(sagas);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSagaStats() {
        log.info("Fetching saga statistics");

        List<SagaInstance> allSagas = sagaInstanceRepository.findAll();

        long completed = allSagas.stream()
                .filter(s -> s.getStatus().name().equals("COMPLETED"))
                .count();

        long compensated = allSagas.stream()
                .filter(s -> s.getStatus().name().equals("COMPENSATED"))
                .count();

        long inProgress = allSagas.stream()
                .filter(s -> !s.getStatus().name().equals("COMPLETED")
                        && !s.getStatus().name().equals("COMPENSATED")
                        && !s.getStatus().name().equals("FAILED"))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allSagas.size());
        stats.put("completed", completed);
        stats.put("compensated", compensated);
        stats.put("inProgress", inProgress);
        stats.put("successRate", allSagas.size() > 0 ?
                (double) completed / allSagas.size() * 100 : 0);

        return ResponseEntity.ok(stats);
    }
}