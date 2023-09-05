<h1 align="center">
    <img src="doc/img/logo-plasticscm.svg" alt="Plastic SCM Logo" width="450" />
</h1>

<p align="center">
    Retrieve and manage your Plastic SCM sources from Jenkins.
</p>

---

![Plugin Version](https://img.shields.io/jenkins/plugin/v/plasticscm-plugin.svg?label=version)
[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/plasticscm-plugin/master)](https://ci.jenkins.io/job/Plugins/job/plasticscm-plugin/job/master/)
![Installs](https://img.shields.io/jenkins/plugin/i/plasticscm-plugin.svg?color=blue)

<!-- TOC -->
* [Overview](#overview)
* [How to use the plugin](#how-to-use-the-plugin)
  * [Installing the Plastic SCM client](#installing-the-plastic-scm-client)
  * [Standalone mode](#standalone-mode)
  * [Mergebot mode](#mergebot-mode)
* [Build information](#build-information)
  * [Summary](#summary)
  * [Changes list](#changes-list)
  * [Plastic SCM Build Data](#plastic-scm-build-data)
* [Requirements](#requirements)
* [Upgrades](#upgrades)
  * [Upgrading from 3.x to 4.x](#upgrading-from-3x-to-4x)
  * [Upgrading from 2.x to 3.x](#upgrading-from-2x-to-3x)
* [Development](#development)
  * [Building the Plugin](#building-the-plugin)
  * [Contributing to the Plugin](#contributing-to-the-plugin)
* [Plugin information](#plugin-information)
* [Change log](#change-log)
<!-- TOC -->

# Overview

Plastic SCM is a scalable, engine-agnostic version control and source code management tool for game development studios
of all sizes. It offers optimized workflows for artists and programmers and superior speed working with large files and
binaries.

This plugin integrates Plastic SCM with Jenkins, allowing you to retrieve and manage your Plastic SCM sources from
Jenkins projects and pipelines. It also provides a way to automatically download and install the Plastic SCM client
(`cm`) in the controller or agent machines.

# How to use the plugin

## Installing the Plastic SCM client

The Plastic SCM plugin for Jenkins requires a working `cm` client to be present in the controller or agent machines.
If it's not, the plugin provides a way for the machines to automatically download and install the client.

You can find out more about how to configure the Plastic SCM client in Jenkins in the
[External Tool Configuration guide](doc/external-tool-configuration.md).

## Standalone mode

The Plastic SCM plugin for Jenkins can be used in standalone mode. This behaves as a regular SCM plugin, where the
project configuration defines which repository to use and the credentials to connect to it. The plugin will then
download the sources to the workspace.

You can find out more about how to configure the Plastic SCM plugin in standalone mode in the
[Standalone mode guide](doc/standalone-guide.md).

## Mergebot mode

The Plastic SCM plugin for Jenkins can be used in Mergebot mode. This mode is intended to be used in conjunction with
the [Plastic SCM Mergebot](https://blog.plasticscm.com/2018/09/mergebot-story-of-our-devops-initiative.html)
feature.

A Mergebot will trigger a CI build, which will be requested to Jenkins via a Plug. The targeted Jenkins project will
need to have the Plastic SCM plugin configured in Mergebot mode. The plugin will then download the sources to the
workspace, taking the required spec from a build parameter.

You can find out more about how to configure the Plastic SCM plugin in Mergebot mode in the
[Mergebot mode guide](doc/mergebot-guide.md).


# Build information

## Summary

The build summary includes a reference to the Plastic SCM changeset spec that was built.

![Build summary](doc/img/build-summary.png)

## Changes list

The changes page in each build will have details about each of the changesets that were included in
the build, linked from the summary list.

![Changes summary](doc/img/changes-summary.png)

## Plastic SCM Build Data

You'll find a link in the build page sidebar to display the Plastic SCM data for that build.

![Build data](doc/img/Plastic-SCM-Build-Data.png)

# Requirements

* Jenkins `2.176.4 (2019-09-25)` or newer
* Plastic SCM command line client `10.0.16.6112` or newer

# Upgrades

## Upgrading from 3.x to 4.x

There are no incompatibilities in configuration from version 3.x to version 4.x. The latter adds
support in project/pipeline configurations to specify a Plastic SCM working mode and credentials.

By default, new or existing projects will use the default credentials for `cm` as configured in the
controller or agent machines. This means that upgrading will keep the same behaviour as you had in
the previous version.

## Upgrading from 2.x to 3.x

The upgrade process is mostly seamless. You'll only need to review the configuration parameters of
your jobs **if you configured them to use multiple workspaces**. Since the subdirectory name was
inferred from the workspace name before and that parameter is now gone, the **Subdirectory**
parameter (used now to specify the subdirectory name explicitly) will be empty. Builds might
download all workspaces in the same directory!

# Development

## Building the Plugin

To build the plugin you will need:

* [Maven](https://maven.apache.org/) version `3.5` or newer
* [Java Development Kit (JDK)](https://jdk.java.net/) version `11`

Run the following command to build the plugin:

```shell
mvn package
```

## Contributing to the Plugin

New feature requests and bug fix proposals should be submitted as
[pull requests](https://help.github.com/en/articles/creating-a-pull-request).
Fork the repository. Make the desired changes in your forked copy. Submit a pull request to the
`master` branch.

Use the [Jenkins SCM API coding style guide](https://github.com/jenkinsci/scm-api-plugin/blob/master/CONTRIBUTING.md#code-style-guidelines)
for new code.

Before submitting a pull request please check if your code passes code quality and style checks by
running:

```shell
mvn verify
```

All pull requests will be evaluated by
[Jenkins CI](https://ci.jenkins.io/job/Plugins/job/plasticscm-plugin/).

# Plugin information

This plugin is developed and maintained by Unity Software, owner of the Plastic SCM product.

Visit us at <https://unity.com/solutions/version-control>.

We really appreciate PR and contributions!

# Change log

You can find it [here](CHANGELOG.md)
