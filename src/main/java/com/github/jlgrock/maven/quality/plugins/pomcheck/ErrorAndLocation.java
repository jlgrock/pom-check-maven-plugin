package com.github.jlgrock.maven.quality.plugins.pomcheck;

/**
 *
 */
public class ErrorAndLocation extends Exception {
    private final String output;
    private final String line;
    private final String column;

    public ErrorAndLocation(final String outputIn, final String lineIn, final String columnIn) {
        output = outputIn;
        line = lineIn;
        column = columnIn;
    }

    public String getOutput() {
        return output;
    }

    public String getLine() {
        return line;
    }

    public String getColumn() {
        return column;
    }
}
