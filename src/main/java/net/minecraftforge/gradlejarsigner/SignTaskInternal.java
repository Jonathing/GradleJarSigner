/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradlejarsigner;

import groovy.lang.Closure;
import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import net.minecraftforge.gradleutils.shared.EnhancedTask;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

interface SignTaskInternal extends SignTask, EnhancedTask, JarSignerInfoInternal, HasPublicType {
    static TaskProvider<? extends SignTask> register(Project project, JarSignerInfoContainer container, TaskProvider<? extends Zip> parent, @Nullable Action<? super SignTask> cfg) {
        return SignTaskImpl.register(project, container, parent, cfg);
    }

    @Override
    default @Internal TypeOf<?> getPublicType() {
        return TypeOf.typeOf(SignTask.class);
    }

    @Override
    default Class<? extends EnhancedPlugin<? super Project>> pluginType() {
        return GradleJarSignerPlugin.class;
    }

    PatternFilterable patternFilterable();

    @Override
    default @Internal Set<String> getIncludes() {
        return patternFilterable().getIncludes();
    }

    @Override
    default @Internal Set<String> getExcludes() {
        return patternFilterable().getExcludes();
    }

    @Override
    default PatternFilterable setIncludes(Iterable<String> includes) {
        return patternFilterable().setIncludes(includes);
    }

    @Override
    default PatternFilterable setExcludes(Iterable<String> excludes) {
        return patternFilterable().setExcludes(excludes);
    }

    @Override
    default PatternFilterable include(String... includes) {
        return patternFilterable().include(includes);
    }

    @Override
    default PatternFilterable include(Iterable<String> includes) {
        return patternFilterable().include(includes);
    }

    @Override
    default PatternFilterable include(Spec<FileTreeElement> includeSpec) {
        return patternFilterable().include(includeSpec);
    }

    @Override
    default PatternFilterable include(Closure includeSpec) {
        return patternFilterable().include(includeSpec);
    }

    @Override
    default PatternFilterable exclude(String... excludes) {
        return patternFilterable().exclude(excludes);
    }

    @Override
    default PatternFilterable exclude(Iterable<String> excludes) {
        return patternFilterable().exclude(excludes);
    }

    @Override
    default PatternFilterable exclude(Spec<FileTreeElement> excludeSpec) {
        return patternFilterable().exclude(excludeSpec);
    }

    @Override
    default PatternFilterable exclude(Closure excludeSpec) {
        return patternFilterable().exclude(excludeSpec);
    }
}
