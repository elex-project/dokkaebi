package com.elex_project.sample;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class SampleTest {

	@Test
	void getName() {
		Sample sample = new Sample("Charlie");

		System.out.println(sample);
		assertEquals("Charlie", sample.getName());
	}
}