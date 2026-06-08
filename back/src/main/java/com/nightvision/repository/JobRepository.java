package com.nightvision.repository;

import com.nightvision.model.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT j FROM Job j ORDER BY j.createdAt DESC")
    List<Job> findJobsWithFilter(Pageable pageable);
}
