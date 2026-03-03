package com.github.rorymurphy.fluentquery.maven.mojo;

import com.github.rorymurphy.fluentquery.maven.generate.MetaClassGenerator;
import com.github.rorymurphy.fluentquery.maven.model.EntityMeta;
import com.github.rorymurphy.fluentquery.maven.scan.CompiledEntityScanner;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
public final class GenerateMetaClassesMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/fluentquery", required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            getLog().debug("Scanning compiled classes in " + classesDirectory);
            CompiledEntityScanner scanner = new CompiledEntityScanner();
            List<EntityMeta> entities = scanner.scan(classesDirectory, project.getCompileClasspathElements());

            if (entities.isEmpty()) {
                getLog().debug("No entities found for fluentquery metaclass generation.");
                return;
            }

            MetaClassGenerator generator = new MetaClassGenerator();
            for (EntityMeta entityMeta : entities) {
                String source = generator.generate(entityMeta);
                String generatedPackage = generator.generatedPackage(entityMeta.packageName());
                File packageDir = new File(outputDirectory, generatedPackage.replace('.', File.separatorChar));
                File outputFile = new File(packageDir, "Q" + entityMeta.className() + ".java");
                writeIfChanged(outputFile, source, "metaclass");
            }

            Map<String, List<EntityMeta>> byPackage = entities.stream()
                .collect(Collectors.groupingBy(EntityMeta::packageName));

            for (Map.Entry<String, List<EntityMeta>> entry : byPackage.entrySet()) {
                String generatedPackage = generator.generatedPackage(entry.getKey());
                File packageDir = new File(outputDirectory, generatedPackage.replace('.', File.separatorChar));
                File outputFile = new File(packageDir, "QEntities.java");
                String source = generator.generateQEntities(entry.getKey(), entry.getValue());
                writeIfChanged(outputFile, source, "QEntities facade");
            }

            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
            getLog().debug("Registered generated source root " + outputDirectory.getAbsolutePath());
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to generate fluentquery metaclasses", ex);
        }
    }

    private void writeIfChanged(File outputFile, String source, String kind) throws Exception {
        File packageDir = outputFile.getParentFile();
        if (!packageDir.exists() && !packageDir.mkdirs()) {
            throw new MojoExecutionException("Failed to create output directory " + packageDir);
        }

        String existing = outputFile.exists() ? Files.readString(outputFile.toPath(), StandardCharsets.UTF_8) : null;
        if (source.equals(existing)) {
            getLog().debug("Skipping unchanged " + kind + " " + outputFile.getAbsolutePath());
            return;
        }

        Files.writeString(outputFile.toPath(), source, StandardCharsets.UTF_8);
        getLog().debug("Generated " + kind + " " + outputFile.getAbsolutePath());
    }
}
