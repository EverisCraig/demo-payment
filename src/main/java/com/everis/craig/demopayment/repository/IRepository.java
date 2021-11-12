package com.everis.craig.demopayment.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IRepository<T,ID>  extends ReactiveMongoRepository<T,ID> {
}
