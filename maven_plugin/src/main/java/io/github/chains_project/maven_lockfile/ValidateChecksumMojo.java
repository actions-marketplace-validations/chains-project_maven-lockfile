package io.github.chains_project.maven_lockfile;

import static io.github.chains_project.maven_lockfile.LockFileFacade.getLockFilePath;

import io.github.chains_project.maven_lockfile.data.LockFile;
import io.github.chains_project.maven_lockfile.graph.DependencyNode;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyCollectorBuilder;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;

/**
 * Plugin goal that validates the checksums of the dependencies of a project against a lock file.
 *
 */
@Mojo(
        name = "validate",
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        requiresOnline = true)
public class ValidateChecksumMojo extends AbstractMojo {
    /**
     * The Maven project for which we are generating a lock file.
     */
    /**
     * The Maven project for which we are generating a lock file.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * The dependency collector builder to use.
     */
    @Component(hint = "default")
    private DependencyCollectorBuilder dependencyCollectorBuilder;

    @Component
    private DependencyResolver dependencyResolver;

    /**
     * Validate the local copies of the dependencies against the project's lock file.
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {
        getLog().info("Validating lock file ...");
        try {
            LockFile lockFileFromFile = LockFile.readLockFile(getLockFilePath(project));
            LockFile lockFileFromProject = LockFileFacade.generateLockFileFromProject(
                    session, project, dependencyCollectorBuilder, dependencyResolver);
            if (!lockFileFromFile.isEquivalentTo(lockFileFromProject)) {
                var missing = new ArrayList<DependencyNode>(lockFileFromProject.getDependencies());
                missing.removeAll(lockFileFromFile.getDependencies());
                StringBuilder sb = new StringBuilder();
                sb.append("Failed verifying Lockfile. The following are missing:");
                sb.append(JsonUtils.toJson(missing));
                sb.append("your lockfile contains the following:");
                sb.append(JsonUtils.toJson(lockFileFromFile.getDependencies()));
                sb.append("your project contains the following:");
                sb.append(JsonUtils.toJson(lockFileFromProject.getDependencies()));
                sb.append("Your lockfile is out of date. Please run 'mvn lockfile:generate' to update it.");
                getLog().error(sb.toString());
                throw new MojoExecutionException("Failed verifying lock file");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not read lock file", e);
        }
        getLog().info("Lockfile successfully validated.");
    }
}
