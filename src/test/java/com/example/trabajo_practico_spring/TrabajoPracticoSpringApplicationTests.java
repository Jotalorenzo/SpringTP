package com.example.trabajo_practico_spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:trabajo_practico;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password="
})
class TrabajoPracticoSpringApplicationTests {

	@Test
	void contextLoads() {
	}

}
