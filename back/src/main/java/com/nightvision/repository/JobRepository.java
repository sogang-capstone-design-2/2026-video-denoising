package com.nightvision.repository;

import com.nightvision.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findTop10ByOrderByCreatedAtDesc();
}
