package com.github.jlgrock.maven.quality.plugins.pomcheck;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

public class VerifyVersionsMojoTest extends AbstractPomCheckTest {

    VerifyVersionsMojo verifyVersionsMojo = new VerifyVersionsMojo(buildContext, mavenProject);

    @Test
    public void testGoodFile() throws MojoFailureException, MojoExecutionException {
        verifyVersionsMojo.checkFile(loadFile("good.xml"));
    }


    @Test(expected = MojoFailureException.class)
    public void testNonPropNotAllowedInDepMgmtFailure() throws MojoFailureException, MojoExecutionException {
        verifyVersionsMojo.checkFile(loadFile("non-prop-not-allowed-in-depmgmt.xml"));
    }

    @Test(expected = MojoFailureException.class)
    public void testVersionNotAllowedInDepsFailure() throws MojoFailureException, MojoExecutionException {
        verifyVersionsMojo.checkFile(loadFile("version-not-allowed-in-deps.xml"));
    }

    @Test(expected = MojoFailureException.class)
    public void testPropertyNotAllowedInDepsFailure() throws MojoFailureException, MojoExecutionException {
        verifyVersionsMojo.checkFile(loadFile("prop-not-allowed-in-deps.xml"));
    }

    @Test(expected = MojoFailureException.class)
    public void testPropertyNotAllowedInPluginsFailure() throws MojoFailureException, MojoExecutionException {
        verifyVersionsMojo.checkFile(loadFile("version-not-allowed-in-plugins.xml"));
    }

    @Test(expected = MojoFailureException.class)
    public void testPropertyNotAllowedInProfileDepsFailure() throws MojoFailureException, MojoExecutionException {
        verifyVersionsMojo.checkFile(loadFile("version-not-allowed-in-profile-deps.xml"));
    }

    @Test(expected = MojoFailureException.class)
    public void testPropertyNotAllowedInProfilePluginsFailure() throws MojoFailureException, MojoExecutionException {
        verifyVersionsMojo.checkFile(loadFile("version-not-allowed-in-profile-plugins.xml"));
    }



}
