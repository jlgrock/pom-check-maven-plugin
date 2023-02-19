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
 * Basically checks that all versions are populated by variables, and not by hard-coded values.
 * This does this by Verifying that a Version matches the format "${...}" for dependency management
 * and that the version is not included in the dependencies sections.
 */
@Mojo(name = "version-check", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class VerifyVersionsMojo extends AbstractPomChecker {

    private static final String VERSION_ALLOWED_STRING = "(\\w*)(\\$\\{[a-zA-Z0-9\\-.]*\\})(.*)";
    private static final Pattern VERSION_ALLOWED_PATTERN = Pattern.compile(VERSION_ALLOWED_STRING);

    private static final String PROJECT_XPATH_PREFIX = "/project";
    private static final String PROFILE_XPATH_PREFIX = "/profiles/profile";
    private static final String DEPENDENCY_MANAGEMENT_XPATH_PREFIX = "/dependencyManagement";
    private static final String DEPENDENCY_VERSION_XPATH_LOC = "/dependencies/dependency/version";
    private static final String PLUGIN_VERSION_XPATH_LOC = "/build/plugins/plugin/version";

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
    public VerifyVersionsMojo() {

    }

    /**
     * Constructor used for testing.
     *
     * @param buildContextIn the buildContext for logging and such.
     * @param mavenProjectIn the maven mavenProject to execute on.
     */
    VerifyVersionsMojo(final BuildContext buildContextIn, final MavenProject mavenProjectIn) {
        buildContext = buildContextIn;
        mavenProject = mavenProjectIn;
    }

    /**
     * The entry point for a maven mojo.  This kicks off the maven scanning against the pom file.
     *
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkFile(mavenProject.getFile());
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

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            getLog().debug("Root element: " + doc.getDocumentElement().getNodeName());
            checkDependencyManagement(doc);
            checkDependencies(doc);
            checkProfilesDependencyManagement(doc);
            checkProfilesDependencies(doc);
            checkPlugins(doc);
            checkProfilePlugins(doc);
        } catch (MojoFailureException mfe) {
            buildContext.addMessage(pomFile, 0, 0, mfe.getMessage(), BuildContext.SEVERITY_ERROR, mfe);
            throw mfe;
        } catch (Exception e) {
            buildContext.addMessage(pomFile, 0, 0, e.getMessage(), BuildContext.SEVERITY_ERROR, e);
            throw new MojoFailureException("Exception processing", e);
        }
    }

    /**
     * Check the versions tag for every dependency in each dependency list provided.
     *
     * @param versionCollection a collection of version nodes
     * @param allowProperties   whether or not properties are allowed in this dependency list.
     *                          Version numbers are never allowed.
     * @throws ErrorAndLocationCollectionException whenever the version does not follow the defined standard.
     */
    private void checkVersions(final NodeList versionCollection, boolean allowProperties)
            throws ErrorAndLocationCollectionException {
        List<ErrorAndLocation> artifacts;
        String errStr;
        if (allowProperties) {
            //dependency management sections
            artifacts = versionAllowedCheck(versionCollection);
            errStr = "dependencyManagment sections must use variables."
                    + "These should likely be referenced in the dependency/plugin management mavenProject.\n"
                    + "The following artifacts are in error:\n";
        } else {
            artifacts = versioinedArtifactsBuilder(versionCollection);
            errStr = "Versions must be inherited from dependency/plugin management"
                    + "sections. \"version\" tag found where it was not allowed for the following artifacts:";
        }
        if (artifacts.size() > 0) {
            throw new ErrorAndLocationCollectionException(getMavenProject().getFile(), errStr, artifacts);
        }
    }

    private List<ErrorAndLocation> versionAllowedCheck(final NodeList versionCollection) {
        List<ErrorAndLocation> artifacts = new ArrayList<>();
        for (int i = 0; i < versionCollection.getLength(); i++) {
            Node node = versionCollection.item(i);
            String version = node.getTextContent();
            Matcher m = VERSION_ALLOWED_PATTERN.matcher(version);
            if (!m.matches()) {
                String lineNumber = getLineNumber(node);
                getLog().debug("adding artifact on line: [" + lineNumber + "]");
                artifacts.add(new ErrorAndLocation(artifactBuilder(node.getParentNode()), lineNumber, null));
            }
        }
        return artifacts;
    }

    /**
     * Based on the xml collection of versions, get the appropriate artifacts coordinates to print out.
     *
     * @param versionCollection the collection to print out the full artifact coordinates
     * @return the string representing a newline delimited list of artifact coordinates
     */
    private List<ErrorAndLocation> versioinedArtifactsBuilder(final NodeList versionCollection) {
        List<ErrorAndLocation> artifacts = new ArrayList<>();
        for (int i = 0; i < versionCollection.getLength(); i++) {
            Node node = versionCollection.item(i);
            String lineNumber = getLineNumber(node);
            getLog().debug("adding artifact on line: [" + lineNumber + "]");
            artifacts.add(new ErrorAndLocation(artifactBuilder(node.getParentNode()), lineNumber, null));
        }
        return artifacts;
    }

    /**
     * Check the dependencies section.
     *
     * @param doc the document to search
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    private void checkDependencies(final Document doc) throws MojoExecutionException, MojoFailureException {
        findAndCheckVersions(doc, PROJECT_XPATH_PREFIX + DEPENDENCY_VERSION_XPATH_LOC, false);
    }

    /**
     * Check the dependencies management section.
     *
     * @param doc the document to search
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    private void checkDependencyManagement(final Document doc) throws MojoExecutionException, MojoFailureException {
        findAndCheckVersions(doc, PROJECT_XPATH_PREFIX + DEPENDENCY_MANAGEMENT_XPATH_PREFIX + DEPENDENCY_VERSION_XPATH_LOC, true);
    }

    /**
     * Check the profiles dependencies section.
     *
     * @param doc the document to search
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    private void checkProfilesDependencies(final Document doc) throws MojoExecutionException, MojoFailureException {
        findAndCheckVersions(doc, PROJECT_XPATH_PREFIX + PROFILE_XPATH_PREFIX + DEPENDENCY_VERSION_XPATH_LOC, false);
    }

    /**
     * Check the profiles dependencies management section.
     *
     * @param doc the document to search
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    private void checkProfilesDependencyManagement(final Document doc) throws MojoExecutionException, MojoFailureException {
        findAndCheckVersions(doc, PROJECT_XPATH_PREFIX + PROFILE_XPATH_PREFIX + DEPENDENCY_MANAGEMENT_XPATH_PREFIX + DEPENDENCY_VERSION_XPATH_LOC, true);
    }

    /**
     * Check the plugins section.
     *
     * @param doc the document to search
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    private void checkPlugins(final Document doc) throws MojoExecutionException, MojoFailureException {
        findAndCheckVersions(doc, PROJECT_XPATH_PREFIX + PLUGIN_VERSION_XPATH_LOC, false);
    }

    /**
     * Check the profiles plugins section.
     *
     * @param doc the document to search
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    private void checkProfilePlugins(final Document doc) throws MojoExecutionException, MojoFailureException {
        findAndCheckVersions(doc, PROJECT_XPATH_PREFIX + PROFILE_XPATH_PREFIX + PLUGIN_VERSION_XPATH_LOC, false);
    }

    /**
     * Using an xpath expression, gather the appropriate collection of tags and pass it to checkVersions.
     *
     * @param doc             the document to search
     * @param path            the path to find the dependencies collection
     * @param allowProperties whether or not properties are allowed at this location
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    private void findAndCheckVersions(final Document doc, final String path, boolean allowProperties)
            throws MojoExecutionException, MojoFailureException {
        try {
            NodeList nodeCollection = findByPath(doc, path);
            checkVersions(nodeCollection, allowProperties);
        } catch (ErrorAndLocationCollectionException ealc) {
            throw new MojoExecutionException(ealc.getMessage());
        }
    }
}
