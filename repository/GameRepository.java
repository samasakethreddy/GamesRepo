package com.Games.deployment.repository;

import com.Games.deployment.entity.GameMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<GameMetadata, Long> {
}
