package io.github.inankova.triage;

import org.springframework.boot.SpringApplication;

public class TestTriageApplication {

	public static void main(String[] args) {
		SpringApplication.from(TriageApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
