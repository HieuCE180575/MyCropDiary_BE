package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmRepository extends JpaRepository<Farm, Long> {
}
