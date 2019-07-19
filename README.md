# Plastic SCM plugin for Jenkins

![Jenkins Plugin Version](https://img.shields.io/jenkins/plugin/v/plasticscm-plugin.svg?label=version) [![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/plasticscm-plugin/master)](https://ci.jenkins.io/job/Plugins/job/plasticscm-plugin/job/master/)

This plugin integrates [Jenkins](https://jenkins.io/) with [Plastic SCM](https://www.plasticscm.com/).

## Requirements

- Jenkins `2.60` or newer
- Plastic SCM command line client `8.0.16.x`

## Development

### Building the Plugin

To build the plugin you will need [Maven](https://maven.apache.org/) and [Java 8 Development Kit](https://jdk.java.net/) (JDK).

Run the following command to compile the source code
```
mvn build
```

### Contributing to the Plugin

New feature proposals and bug fix proposals should be submitted as pull requests. Fork the repository, Make the
desired changes in your forked copy. Submit a pull request.

Before submitting a pull request please check if your code passes code quality and style checks by running
```
mvn verify
```

All pull requests will be evaluated by the [Jenkins CI](https://ci.jenkins.io/job/Plugins/job/plasticscm-plugin/).
