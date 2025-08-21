/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradlejarsigner;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

abstract class SignTaskImpl extends DefaultTask implements SignTaskInternal {
    private final JarSignerInfoContainer container;
    private final PatternSet patternSet;

    protected abstract @InputFile RegularFileProperty getInputJar();
    protected abstract @InputFile RegularFileProperty getOutputJar();
    protected abstract @InputFile RegularFileProperty getOutputOriginal();

    protected abstract @Inject ObjectFactory getObjects();
    protected abstract @Inject ArchiveOperations getArchiveOperations();

    static TaskProvider<? extends SignTask> register(Project project, JarSignerInfoContainer container, TaskProvider<? extends Zip> parent, @Nullable Action<? super SignTask> cfg) {
        var ret = project.getTasks().register("jarsign" + StringGroovyMethods.capitalize(parent.getName()), SignTaskImpl.class, task -> {
            task.getInputJar().set(parent.map(AbstractArchiveTask::getArchiveFile).map(Provider::get));

            container.fill(task.container);

            task.getOutputs().upToDateWhen(it -> parent.map(zip -> zip.getOutputs().getUpToDateSpec().isSatisfiedBy(zip)).getOrElse(false));

            if (cfg != null)
                cfg.execute(task);
        });
        parent.configure(task -> task.finalizedBy(ret));
        return ret;
    }

    @Inject
    public SignTaskImpl() {
        this.container = this.getObjects().newInstance(JarSignerInfoContainer.class);
        this.patternSet = this.getObjects().newInstance(PatternSet.class);

        this.getOutputJar().convention(this.getInputJar());
        this.getOutputOriginal().fileProvider(this.getInputJar().map(RegularFile::getAsFile).map(it -> new File(it.getParentFile(), it.getName() + ".original")));

        this.onlyIf(
            "If missing key information, input jar will be unsigned",
            task -> ((SignTaskImpl) task).hasEnoughInfo()
        );
    }

    @TaskAction
    void signSafe() {
        try {
            this.sign();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasEnoughInfo() {
        return this.container.alias.isPresent() &&
               this.container.storePass.isPresent() &&
               this.container.keyPass.isPresent() &&
               (this.container.keyStoreData.isPresent() || this.container.keyStoreFile.isPresent());
    }

    private void sign() throws IOException {
        final Map<String, UnsignedData> ignoredStuff = new HashMap<>();

        File tmp = this.getTemporaryDir();
        File output = this.getInputJar().get().getAsFile();
        File original = this.getOutputOriginal().get().getAsFile();
        Files.move(output.toPath(), original.toPath(), StandardCopyOption.REPLACE_EXISTING);

        File input = original;
        if (!patternSet.isEmpty()) {
            input = new File(tmp, input.getName() + ".unsigned");
            processInputJar(original, input, ignoredStuff);
            if (!ignoredStuff.isEmpty())
                output = new File(tmp, input.getName() + ".signed");
        }

        File keyStore;
        if (this.container.keyStoreFile.isPresent()) {
            if (this.container.keyStoreData.isPresent())
                throw new IllegalStateException("Both KeyStoreFile and KeyStoreData can not be set at the same time");
            keyStore = this.container.keyStoreFile.getAsFile().get();
        } else if (this.container.keyStoreData.isPresent()) {
            byte[] data = Base64.getDecoder().decode(this.container.keyStoreData.get().getBytes(StandardCharsets.UTF_8));
            keyStore = new File(tmp, "keystore");
            Files.write(keyStore.toPath(), data);
        } else {
            throw new IllegalArgumentException("SignJar needs either a Base64 encoded KeyStore file, or a path to a KeyStore file");
        }

        Map<String, String> map = new HashMap<>();
        map.put("alias", this.container.alias.get());
        map.put("storePass", this.container.storePass.get());
        map.put("jar", input.getAbsolutePath());
        map.put("signedJar", output.getAbsolutePath());
        map.put("keyStore", keyStore.getAbsolutePath());
        if (this.container.keyPass.isPresent())
            map.put("keypass", this.container.keyPass.get());

        try {
            this.getAnt().invokeMethod("signjar", map);
        } finally {
            // Report a proper warning if keystore cannot be deleted
            if (!this.container.keyStoreFile.isPresent())
                keyStore.delete();
        }

        if (!ignoredStuff.isEmpty())
            writeOutputJar(output, this.getInputJar().get().getAsFile(), ignoredStuff);
    }

    private record UnsignedData(byte[] data, long lastModified) { }

    private void processInputJar(File input, File output, final Map<String, UnsignedData> unsigned) throws IOException {
        final Spec<FileTreeElement> spec = patternSet.getAsSpec();

        output.getParentFile().mkdirs();
        try (JarOutputStream outs = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(output)))){
            this.getArchiveOperations().zipTree(input).visit(new FileVisitor() {
                @Override
                public void visitDir(FileVisitDetails details) {
                    try {
                        String path = details.getPath();
                        ZipEntry entry = new ZipEntry(path.endsWith("/") ? path : path + "/");
                        outs.putNextEntry(entry);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void visitFile(FileVisitDetails details) {
                    try {
                        if (spec.isSatisfiedBy(details)) {
                            ZipEntry entry = new ZipEntry(details.getPath());
                            entry.setTime(details.getLastModified());
                            outs.putNextEntry(entry);
                            details.copyTo(outs);
                            outs.closeEntry();
                        } else {
                            InputStream stream = details.open();
                            ByteArrayOutputStream tmp = new ByteArrayOutputStream(stream.available());
                            byte[] buf = new byte[0x100];
                            int len;
                            while ((len = stream.read(buf)) != -1)
                                tmp.write(buf, 0, len);

                            byte[] data = tmp.toByteArray();
                            unsigned.put(details.getPath(), new UnsignedData(data, details.getLastModified()));
                            stream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void writeOutputJar(File signedJar, File outputJar, Map<String, UnsignedData> unsigned) throws IOException {
        outputJar.getParentFile().mkdirs();

        JarOutputStream outs = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputJar)));

        byte[] buf = new byte[0x100];
        ZipFile base = new ZipFile(signedJar);
        for (ZipEntry e : Collections.list(base.entries())) {
            if (e.isDirectory()) {
                outs.putNextEntry(e);
            } else {
                ZipEntry n = new ZipEntry(e.getName());
                n.setTime(e.getTime());
                outs.putNextEntry(n);
                InputStream in = base.getInputStream(e);
                int len;
                while ((len = in.read(buf)) != -1)
                    outs.write(buf, 0, len);
                outs.closeEntry();
            }
        }
        base.close();

        for (Entry<String, UnsignedData> e : unsigned.entrySet()) {
            ZipEntry n = new ZipEntry(e.getKey());
            n.setTime(e.getValue().lastModified());
            outs.putNextEntry(n);
            outs.write(e.getValue().data());
            outs.closeEntry();
        }

        outs.close();
    }

    @Override
    public JarSignerInfoContainer getContainer() {
        return this.container;
    }

    @Override
    public PatternFilterable patternFilterable() {
        return this.patternSet;
    }
}
