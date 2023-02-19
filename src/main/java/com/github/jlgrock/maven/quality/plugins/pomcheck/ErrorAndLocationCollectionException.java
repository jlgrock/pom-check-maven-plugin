package com.github.jlgrock.maven.quality.plugins.pomcheck;

import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.List;

/**
 * simple class to collect error messages.
 */
public class ErrorAndLocationCollectionException extends Exception {
    private File file;
    private final List<ErrorAndLocation> errors;
    private final String description;

    /**
     * @param fileIn        The file that has the errors that we are tracking.
     * @param descriptionIn the description of the failure.
     * @param errorsIn      the errors that are received
     */
    public ErrorAndLocationCollectionException(
            final File fileIn,
            final String descriptionIn,
            final List<ErrorAndLocation> errorsIn) {
        file = fileIn;
        description = descriptionIn;
        errors = errorsIn;
    }

    @Override
    public String getMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(description).append(":\n");
        for (ErrorAndLocation error : errors) {
            stringBuilder.append(error.getOutput());
            if (!StringUtils.isEmpty(error.getLine())) {
                stringBuilder.append(" on line ").append(error.getLine());
            }
            if (!StringUtils.isEmpty(error.getColumn())) {
                stringBuilder.append(" in column ").append(error.getColumn());
            }
            if (file != null) {
                stringBuilder.append(" in file ").append(file.getAbsolutePath());
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
