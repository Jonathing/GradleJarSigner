/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradlejarsigner;

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;

import javax.inject.Inject;
import java.io.File;

abstract class JarSignerInfoContainer implements JarSignerInfo, HasPublicType {
    final Property<String> alias;
    final Property<String> storePass;
    final Property<String> keyPass;
    final Property<String> keyStoreData;
    final RegularFileProperty keyStoreFile;

    protected abstract @Inject ObjectFactory getObjects();
    protected abstract @Inject ProjectLayout getProjectLayout();
    protected abstract @Inject ProviderFactory getProviders();

    @Inject
    public JarSignerInfoContainer() {
        this.alias = this.getObjects().property(String.class);
        this.storePass = this.getObjects().property(String.class);
        this.keyPass = this.getObjects().property(String.class);
        this.keyStoreData = this.getObjects().property(String.class);
        this.keyStoreFile = this.getObjects().fileProperty();
    }

    @Override
    public TypeOf<?> getPublicType() {
        return TypeOf.typeOf(JarSignerInfo.class);
    }

    // Package private because I intentionally don't want getters for key info.
    void fill(JarSignerInfoContainer to) {
        to.alias.set(this.alias);
        to.storePass.set(this.storePass);
        to.keyPass.set(this.keyPass);
        to.keyStoreData.set(this.keyStoreData);
        to.keyStoreFile.set(this.keyStoreFile);
    }

    @Override
    public void setAlias(CharSequence value) {
        this.alias.set(this.getProviders().provider(value::toString));
    }

    @Override
    public void setAlias(Provider<? extends CharSequence> value) {
        this.alias.set(value.map(Object::toString));
    }

    @Override
    public void setStorePass(CharSequence value) {
        this.storePass.set(this.getProviders().provider(value::toString));
    }

    @Override
    public void setStorePass(Provider<? extends CharSequence> value) {
        this.storePass.set(value.map(Object::toString));
    }

    @Override
    public void setKeyPass(CharSequence value) {
        this.keyPass.set(this.getProviders().provider(value::toString));
    }

    @Override
    public void setKeyPass(Provider<? extends CharSequence> value) {
        this.keyPass.set(value.map(Object::toString));
    }

    @Override
    public void setKeyStoreData(CharSequence value) {
        this.keyStoreData.set(this.getProviders().provider(value::toString));
    }

    @Override
    public void setKeyStoreData(Provider<? extends CharSequence> value) {
        this.keyStoreData.set(value.map(Object::toString));
    }

    @Override
    public void setKeyStoreFile(Object value) {
        this.keyStoreFile.fileProvider(this.getProviders().provider(() -> this.getProjectLayout().files(value).getSingleFile()));
    }

    @Override
    public void setKeyStoreFile(File value) {
        this.keyStoreFile.set(value);
    }

    @Override
    public void setKeyStoreFile(RegularFile value) {
        this.keyStoreFile.set(value);
    }

    @Override
    public void setKeyStoreFile(Provider<?> value) {
        this.keyStoreFile.fileProvider(value.map(it -> {
            if (it instanceof RegularFile)
                return ((RegularFile) it).getAsFile();
            else if (it instanceof File)
                return (File) it;
            else
                return this.getProjectLayout().files(it).getSingleFile();
        }));
    }
}
