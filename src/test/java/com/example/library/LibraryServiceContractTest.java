package com.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every operation takes today: Pickup-window expiry is evaluated from the date passed in, never a
 * clock, so an operation without one can act on or report stale Hold state.
 */
class LibraryServiceContractTest {
    @Test
    void everyPublicOperationTakesToday() {
        List<String> missingToday = Arrays.stream(LibraryService.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> !Arrays.asList(m.getParameterTypes()).contains(LocalDate.class))
                .map(Method::getName)
                .toList();

        assertEquals(List.of(), missingToday, "Public LibraryService operations must take a LocalDate today");
    }
}
