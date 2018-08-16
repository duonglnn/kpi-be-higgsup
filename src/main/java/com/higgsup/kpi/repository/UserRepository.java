package com.higgsup.kpi.repository;

import org.springframework.data.repository.CrudRepository;

import com.higgsup.kpi.entity.KpiUser;

public interface UserRepository extends CrudRepository<KpiUser, String>{
	
	public KpiUser findByUserName(String username);
	
}
