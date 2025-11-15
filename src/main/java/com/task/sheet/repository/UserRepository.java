package com.task.sheet.repository;

import com.task.sheet.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {
    Users findByUsernameAndPassword(String username, String encrypt);

    Users findByMailId(String mailId);
}
