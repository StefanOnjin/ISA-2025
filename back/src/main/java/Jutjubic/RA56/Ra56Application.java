package Jutjubic.RA56;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;

@EnableAsync
@EnableCaching
@SpringBootApplication
public class Ra56Application {

	public static void main(String[] args) {
		SpringApplication.run(Ra56Application.class, args);
	}

}
