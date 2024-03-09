package demo.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import demo.model.Availability;
import demo.model.Booking;
import demo.model.Notification;
import demo.repo.BookingRepository;

@RestController
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;

    private RestTemplate restTemplate = new RestTemplate();
    
    @Value("${availability.service.url}")
    private String availabilityServiceUrl;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private static final AtomicInteger counter = new AtomicInteger(0);

    @PostMapping("/book")
    public ResponseEntity<Booking> book(@RequestBody Booking input){

        Availability availability = new Availability();
        availability.setHotel(input.getHotel());
        availability.setRoom(input.getRoom());
        availability.setBookingStartDate(input.getBookingStartDate());
        availability.setBookingEndDate(input.getBookingEndDate());

        

        /* check availability */
        Availability availResponse = restTemplate.postForObject(availabilityServiceUrl + "/check-availability", availability, Availability.class);
        logger.info(availResponse.toString());

        if (counter.get() > 500){
            logger.info("reset DB");
            bookingRepository.deleteAll();
            restTemplate.getForObject(availabilityServiceUrl + "/reset", String.class);
        }

        /* if available save the booking*/
        if (availResponse != null && !availResponse.isBooked()){
            Booking  b = bookingRepository.save(input);
            logger.info("saved " + counter.incrementAndGet() + " bookings since reset");
            logger.info("booking saved. ID : " + b.getId());

            /* update availability */
            logger.info("updating availability");
            availability.setBooked(true);
            Availability availBlockResponse = restTemplate.postForObject(availabilityServiceUrl + "/availabilities", availability, Availability.class);
            logger.info(availBlockResponse.toString());

            /* notify */
            Notification notif = new Notification(input.getId(),input.getCustomerName(),"all");
            logger.info(notificationServiceUrl+"/notify");
            Notification notifResponse = restTemplate.postForObject(notificationServiceUrl + "/notify", notif, Notification.class);
            logger.info(notifResponse.toString());
        }else {
            input.setRoom(null);
            input.setHotel(null);;
            logger.info("dates a booked, abandoning booking");
        }

        return new ResponseEntity<>(input, HttpStatus.OK);
    }

    @Scheduled(fixedRate = 60000*5) // 10000 milliseconds = 10 seconds
    public void myScheduledMethod() {
        logger.info("reset DB");
        bookingRepository.deleteAll();
    }
}
