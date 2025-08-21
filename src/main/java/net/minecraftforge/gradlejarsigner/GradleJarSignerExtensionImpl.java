/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradlejarsigner;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

abstract class GradleJarSignerExtensionImpl implements GradleJarSignerExtensionInternal {
    private final Project project;
    private final JarSignerInfoContainer container;

    protected abstract @Inject ObjectFactory getObjects();

    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public GradleJarSignerExtensionImpl(Project project) {
        this.project = project;
        this.container = this.getObjects().newInstance(JarSignerInfoContainer.class, project);
    }

    @Override
    public TaskProvider<? extends SignTask> sign(Zip task, @Nullable Action<? super SignTask> cfg) {
        return this.sign(this.project.getTasks().named(task.getName(), Zip.class), cfg);
    }

    @Override
    public TaskProvider<? extends SignTask> sign(TaskProvider<? extends Zip> task, @Nullable Action<? super SignTask> cfg) {
        return SignTaskInternal.register(this.project, this.container, task, cfg);
    }

    public void autoDetect() {
        autoDetect(this.getProviders().provider(this.project::getName));
    }

    public void autoDetect(CharSequence prefix) {
        autoDetect(this.getProviders().provider(() -> prefix));
    }

    public void autoDetect(Provider<? extends CharSequence> prefix) {
        autoDetectImpl(prefix.map(Object::toString));
    }

    private void autoDetectImpl(Provider<String> prefix) {
        prefix = prefix.map(p -> p + '.');
        this.set(this.container.alias, prefix, "SIGN_KEY_ALIAS");
        this.set(this.container.keyPass, prefix, "SIGN_KEY_PASSWORD");
        this.set(this.container.storePass, prefix, "SIGN_KEYSTORE_PASSWORD");
        this.set(this.container.keyStoreData, prefix, "SIGN_KEYSTORE_DATA");
    }

    @Override
    public JarSignerInfoContainer getContainer() {
        return this.container;
    }

    private void set(Property<String> prop, Provider<String> prefix, String key) {
        //@formatter:off
        prop.set(
                    this.getProviders().gradlePropertiesPrefixedBy(prefix).map(props -> props.get(key))
            .orElse(this.getProviders().systemPropertiesPrefixedBy(prefix).map(props -> props.get(key)))
            .orElse(this.getProviders().environmentVariablesPrefixedBy(prefix).map(props -> props.get(key)))
            .orElse(this.getProviders().gradleProperty(key))
            .orElse(this.getProviders().systemProperty(key))
            .orElse(this.getProviders().environmentVariable(key))
        );
        //@formatter:on
    }
}
