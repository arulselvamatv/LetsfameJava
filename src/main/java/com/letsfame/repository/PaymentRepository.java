package com.letsfame.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.letsfame.bean.Payments;

@Repository
public interface PaymentRepository extends MongoRepository<Payments, String> {

}
