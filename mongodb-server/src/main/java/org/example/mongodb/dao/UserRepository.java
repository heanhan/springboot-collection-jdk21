package org.example.mongodb.dao;

import org.example.mongodb.entity.User;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends   PagingAndSortingRepository<User,Long> {
}
