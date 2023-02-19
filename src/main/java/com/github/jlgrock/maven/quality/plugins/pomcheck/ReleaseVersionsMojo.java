package com.github.jlgrock.maven.quality.plugins.pomcheck;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies that you are only building using release versions.
 *
 * This plugin makes the assumption that all dependency and plugin versions are in the properties.
 * To guarantee this, please use the verify-versions plugin.
 *
 * This plugin then walks known properties (at any level) and
 * does this by verifying that nothing in the properties includes "*-SNAPSHOT"
 *
 */
@Mojo(name = "release-check", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class ReleaseVersionsMojo extends AbstractPomChecker {

    private static final String VERSION_NOT_ALLOWED_STRING = "\\w*.*(-SNAPSHOT)\\w*";
    private static final Pattern VERSION_NOT_ALLOWED_PATTERN = Pattern.compile(VERSION_NOT_ALLOWED_STRING);

    private static final String PROJECT_XPATH_PREFIX = "/project";
    private static final String PROFILE_XPATH_PREFIX = "/profiles/profile";
    private static final String PROPERTIES_XPATH_PREFIX = "/properties/*";

    @Component
    private BuildContext buildContext;

    /**
     * The Maven Project.
     */
    @Component
    protected MavenProject mavenProject;

    /**
     * Default constructor (for when not testing).
     */
    public ReleaseVersionsMojo() {
    }

    /**
     * Constructor used for testing.
     *
     * @param buildContextIn the buildContext for logging and such.
     * @param mavenProjectIn the maven mavenProject to execute on.
     */
    ReleaseVersionsMojo(final BuildContext buildContextIn, final MavenProject mavenProjectIn) {
        buildContext = buildContextIn;
        mavenProject = mavenProjectIn;
    }

    /**
     * Will allow you to check a particular file.  It expects that this file follows the maven
     * defined format.
     *
     * @param pomFile the file to scan
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    protected void checkFile(final File pomFile) throws MojoExecutionException, MojoFailureException {
        try {
            getLog().debug("file " + pomFile.getAbsolutePath() + " exists: " + pomFile.exists());
            Document doc = readXML(pomFile);
            checkProperties(doc);
            checkProfileProperties(doc);
        } catch (Exception e) {
            buildContext.addMessage(pomFile, 0, 0, e.getMessage(), BuildContext.SEVERITY_ERROR, e);
            throw new MojoFailureException("Exception processing", e);
        }
    }

    @Override
    protected MavenProject getMavenProject() {
        return mavenProject;
    }

    @Override
    protected BuildContext getBuildContext() {
        return buildContext;
    }

    /**
     * Using an xpath expression, gather the appropriate collection of tags and pass it to checkVersions.
     *
     * @param doc  the document to search
     * @param path the path to find the dependencies collection
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    private void findAndCheckVersions(final Document doc, final String path)
            throws MojoExecutionException, MojoFailureException {
        try {
            NodeList nodeCollection = findByPath(doc, path);
            checkVersions(nodeCollection);
        } catch (ErrorAndLocationCollectionException ealc) {
            throw new MojoExecutionException(ealc.getMessage());
        }
    }

    /**
     * Check the versions in the properties section and make sure that none of them end in "-SNAPSHOT"
     *
     * @param nodeCollection the collection of objects to check
     * @throws ErrorAndLocationCollectionException
     */
    public void checkVersions(final NodeList nodeCollection) throws ErrorAndLocationCollectionException {
        List<ErrorAndLocation> artifacts = new ArrayList<ErrorAndLocation>();
        for (int i = 0; i < nodeCollection.getLength(); i++) {
            Node node = nodeCollection.item(i);
            String version = node.getTextContent();
            Matcher m = VERSION_NOT_ALLOWED_PATTERN.matcher(version);
            if (m.matches()) {
                artifacts.add(new ErrorAndLocation(artifactBuilder(node.getParentNode()), getLineNumber(node), null));
            }
        }
        if (artifacts.size() > 0) {
            throw new ErrorAndLocationCollectionException(getMavenProject().getFile(), "You must not use snapshots in properties", artifacts);
        }
    }

    /**
     * Check the properties section of the project.
     *
     * @param doc the document to check
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    public void checkProperties(Document doc) throws MojoExecutionException, MojoFailureException {
        findAndCheckVersions(doc, PROJECT_XPATH_PREFIX + PROPERTIES_XPATH_PREFIX);
    }

    /**
     * Check the profile properties section of the project.
     *
     * @param doc the document to check
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    public void checkProfileProperties(Document doc) throws MojoExecutionException, MojoFailureException {
        findAndCheckVersions(doc, PROJECT_XPATH_PREFIX + PROFILE_XPATH_PREFIX + PROPERTIES_XPATH_PREFIX);
    }
}
