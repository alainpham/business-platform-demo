package demo.repo;

import org.springframework.data.repository.PagingAndSortingRepository;

import org.springframework.data.repository.CrudRepository;

import demo.model.Person;

public interface  PersonRepository extends PagingAndSortingRepository<Person, Long>, CrudRepository<Person,Long> {


}
