/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradlejarsigner;

import net.minecraftforge.gradleutils.shared.EnhancedPlugin;
import org.gradle.api.Project;

import javax.inject.Inject;

abstract class GradleJarSignerPlugin extends EnhancedPlugin<Project> {
    static final String NAME = "jarSigner";
    static final String DISPLAY_NAME = "Gradle Jar Signer";

    @Inject
    public GradleJarSignerPlugin() {
        super(NAME, DISPLAY_NAME);
    }

    @Override
    public void setup(Project target) {
        target.getExtensions().create(GradleJarSignerExtension.NAME, GradleJarSignerExtensionImpl.class, target);
    }
}
