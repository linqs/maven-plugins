package org.linqs.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Create a listing of all the classes in a properties file.
 */
@Mojo(name = "list", defaultPhase = LifecyclePhase.INITIALIZE)
public class ListClasses extends AbstractMojo {
    public static final String DEFAULT_OUTPUT_FILENAME = "classlist.properties";
    public static final String DEFAULT_KEY_PREFIX = "classlist";
    public static final String DEFAULT_KEY_SUFFIX = "classes";

    @Parameter(defaultValue = DEFAULT_OUTPUT_FILENAME)
    private String outputFilename;

    @Parameter(defaultValue = DEFAULT_KEY_PREFIX)
    private String keyPrefix;

    @Parameter(defaultValue = DEFAULT_KEY_SUFFIX)
    private String keySuffix;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        List<String> classNames = new ArrayList<String>();

        @SuppressWarnings("unchecked")
        List<String> roots = (List<String>)(project.getCompileSourceRoots());
        for (String root : roots) {
            Path rootPath = Paths.get(root);

            for (String path : getFiles(root)) {
                String relPath = rootPath.relativize(Paths.get(path)).toString();
                if (!relPath.endsWith(".java")) {
                    continue;
                }

                String name = relPath.replaceFirst("\\.java$", "").replace("/", ".");
                classNames.add(name);
            }
        }

        String outputPath = Paths.get(project.getBuild().getOutputDirectory(), outputFilename).toString();
        try (FileWriter writer = new FileWriter(outputPath)) {
            for (String name : classNames) {
                writer.write(String.format("%s.%s=%s\n", keyPrefix, keySuffix, name));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Unable to write class list: " + outputPath, ex);
        }
    }

    private List<String> getFiles(String root) {
        final List<String> paths = new ArrayList<String>();

        try {
            Files.walkFileTree(Paths.get(root), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    paths.add(file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException("Unable to walk directory: " + root, ex);
        }

        return paths;
    }
}
