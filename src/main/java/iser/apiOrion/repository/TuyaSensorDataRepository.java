package iser.apiOrion.repository;

import iser.apiOrion.collection.TuyaSensorData;
import org.springframework.data.mongodb.repository.MongoRepository;


import org.springframework.stereotype.Repository;
@Repository
public interface TuyaSensorDataRepository extends MongoRepository<TuyaSensorData, String> {

}





