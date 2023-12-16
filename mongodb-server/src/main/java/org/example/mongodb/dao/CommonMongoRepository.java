package org.example.mongodb.dao;

import org.springframework.data.mongodb.repository.MongoRepository;


public interface CommonMongoRepository<T,O> extends MongoRepository<T,O> {
}
