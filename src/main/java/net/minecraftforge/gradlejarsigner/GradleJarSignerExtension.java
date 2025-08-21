/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package net.minecraftforge.gradlejarsigner;

import org.gradle.api.Action;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

public interface GradleJarSignerExtension extends JarSignerInfo {
    String NAME = "jarSigner";

    default TaskProvider<? extends SignTask> sign(Zip task) {
        return this.sign(task, null);
    }

    TaskProvider<? extends SignTask> sign(Zip task, Action<? super SignTask> cfg);

    default TaskProvider<? extends SignTask> sign(TaskProvider<? extends Zip> task) {
        return this.sign(task, null);
    }

    TaskProvider<? extends SignTask> sign(TaskProvider<? extends Zip> task, Action<? super SignTask> cfg);

    void autoDetect();

    void autoDetect(CharSequence prefix);

    void autoDetect(Provider<? extends CharSequence> prefix);

    default void autoDetect(ProviderConvertible<? extends CharSequence> prefix) {
        this.autoDetect(prefix.asProvider());
    }
}
