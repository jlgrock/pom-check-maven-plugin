package com.github.jlgrock.maven.quality.plugins.pomcheck;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ErrorAndLocationCollectionExceptionTest {

    @Test
    public void testBasicCreate() {
        File file = Mockito.mock(File.class);
        Mockito.when(file.getAbsolutePath()).thenReturn("/example/pom.xml");
        String description = "basic description";
        List<ErrorAndLocation> errors = new ArrayList<>();
        ErrorAndLocation error = Mockito.mock(ErrorAndLocation.class);
        Mockito.when(error.getOutput()).thenReturn("example error");
        Mockito.when(error.getColumn()).thenReturn("5");
        Mockito.when(error.getLine()).thenReturn("6");
        errors.add(error);

        ErrorAndLocationCollectionException ex = new ErrorAndLocationCollectionException(file, description, errors);
        Assertions.assertEquals(ex.getMessage(), "basic description:\nexample error on line 6 in column 5 in file /example/pom.xml\n");
    }

}
