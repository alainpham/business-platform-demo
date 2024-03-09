package demo.repo;
import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import demo.model.Booking;

public interface  BookingRepository extends PagingAndSortingRepository<Booking, Long>, CrudRepository<Booking,Long> {

	List<Booking> findBycustomerName(@Param("customerName") String name);

}
