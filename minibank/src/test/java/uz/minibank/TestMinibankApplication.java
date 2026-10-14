package uz.minibank;

import org.springframework.boot.SpringApplication;

public class TestMinibankApplication {

	public static void main(String[] args) {
		SpringApplication.from(MinibankApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
