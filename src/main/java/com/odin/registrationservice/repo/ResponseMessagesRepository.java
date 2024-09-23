package com.odin.registrationservice.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.odin.registrationservice.entity.ResponseMessages;


@Repository
public interface ResponseMessagesRepository extends JpaRepository<ResponseMessages, Integer> {

}
