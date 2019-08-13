# Plastic SCM plugin for Jenkins

![Plugin Version](https://img.shields.io/jenkins/plugin/v/plasticscm-plugin.svg?label=version) [![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/plasticscm-plugin/master)](https://ci.jenkins.io/job/Plugins/job/plasticscm-plugin/job/master/)

This plugin integrates [Jenkins](https://jenkins.io/) with [Plastic SCM](https://www.plasticscm.com/).

## Requirements

* Jenkins `2.60.3` or newer
* Plastic SCM command line client `8.0.16.3400` or newer

## Configuration

See [plugin website](https://plugins.jenkins.io/plasticscm-plugin) for instructions how to setup and use the plugin.

## Development

### Building the Plugin

To build the plugin you will need
* [Maven](https://maven.apache.org/) version `3.5` or newer
* [Java Development Kit (JDK)](https://jdk.java.net/) version `8`

Run the following command to build the plugin
```shell
mvn package
```

### Contributing to the Plugin

New feature proposals and bug fix proposals should be submitted as
[pull requests](https://help.github.com/en/articles/creating-a-pull-request).
Fork the repository. Make the desired changes in your forked copy. Submit a pull request to the `master` branch.

Use the [Jenkins SCM API coding style guide](https://github.com/jenkinsci/scm-api-plugin/blob/master/CONTRIBUTING.md#code-style-guidelines) for new code.

Before submitting a pull request please check if your code passes code quality and style checks by running
```shell
mvn verify
```

All pull requests will be evaluated by [Jenkins CI](https://ci.jenkins.io/job/Plugins/job/plasticscm-plugin/).
