package dev.lucas.mesapronta;

import org.springframework.boot.SpringApplication;

public class TestMesaProntaApplication {

	public static void main(String[] args) {
		SpringApplication.from(MesaProntaApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
