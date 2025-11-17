package com.xsh.trueused.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xsh.trueused.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
}