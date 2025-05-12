package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the Main class.
 * This is a simple example to demonstrate how to write tests for this project.
 */
public class MainTest {

    /**
     * A simple test that always passes.
     * This demonstrates the basic structure of a JUnit 5 test.
     */
    @Test
    public void testMainClassExists() {
        // Simply verify that the Main class exists and can be instantiated
        Main main = new Main();
        assertNotNull(main);
    }

    /**
     * A test that demonstrates different assertion methods.
     */
    @Test
    public void testAssertions() {
        // String assertions
        String message = "Hello and welcome!";
        assertEquals("Hello and welcome!", message);
        assertTrue(message.startsWith("Hello"));
        assertFalse(message.isEmpty());
        
        // Numeric assertions
        assertEquals(5, 5);
        assertTrue(10 > 5);
        
        // Array assertions
        int[] numbers = {1, 2, 3, 4, 5};
        assertEquals(5, numbers.length);
        assertEquals(3, numbers[2]);
    }
}