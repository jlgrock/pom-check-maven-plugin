package com.github.jlgrock.maven.quality.plugins.pomcheck;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

public class ReleaseVersionsMojoTest extends AbstractPomCheckTest {

    ReleaseVersionsMojo releaseVersionsMojo = new ReleaseVersionsMojo(buildContext, mavenProject);

    @Test
    public void testGoodFile() throws MojoFailureException, MojoExecutionException {
        releaseVersionsMojo.checkFile(loadFile("good.xml"));
    }

    @Test(expected = MojoFailureException.class)
    public void testBadVersionPropertyFailure() throws MojoFailureException, MojoExecutionException {
        releaseVersionsMojo.checkFile(loadFile("bad-property.xml"));
    }

    @Test(expected = MojoFailureException.class)
    public void testBadVersionProfilePropertyFailure() throws MojoFailureException, MojoExecutionException {
        releaseVersionsMojo.checkFile(loadFile("bad-profile-property.xml"));
    }

}
