// Copyright 2014 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
//buildscript {
//    repositories {
//        google()
//        mavenCentral()
//    }
//    dependencies {
//        classpath(libs.gradle)
//        classpath(libs.kotlin.gradle.plugin)
//    }
//}
allprojects {
    repositories {
        google()
        mavenCentral()
    }
//    dependencies {
//        classpath(libs.gradle)
//        classpath(libs.kotlin.gradle.plugin)
//    }
}

rootProject.layout.buildDirectory.value(rootProject.layout.buildDirectory.dir("../build").get())

subprojects {
    project.layout.buildDirectory.value(rootProject.layout.buildDirectory.dir(project.name).get())
}

//subprojects {
//    project.evaluationDependsOn(":simple_sms")
//    dependencyLocking {
//        ignoredDependencies.add("io.flutter:*")
//        lockFile = file("${rootProject.projectDir}/project-${project.name}.lockfile")
//        if (!project.hasProperty("local-engine-repo")) {
//            lockAllConfigurations()
//        }
//    }
//}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
