package com.github.jlgrock.maven.quality.plugins.pomcheck;

import org.apache.maven.project.MavenProject;
import org.mockito.Mockito;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;

public abstract class AbstractPomCheckTest {
    protected BuildContext buildContext = Mockito.mock(BuildContext.class);
    protected MavenProject mavenProject = Mockito.mock(MavenProject.class);

    protected File loadFile(final String filename) {
        return new File("src/test/resources/" + filename);
    }
}
