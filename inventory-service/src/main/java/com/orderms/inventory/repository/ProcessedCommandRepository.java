package com.orderms.inventory.repository;

import com.orderms.inventory.entity.ProcessedCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, Long> {

    Optional<ProcessedCommand> findByCommandId(String commandId);

    boolean existsByCommandId(String commandId);
}