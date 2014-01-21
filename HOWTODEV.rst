Working on these Jenkins plugins
================================

To get a local environment running, follow these steps:

* Follow the `Jenkins plugin tutorial`_ to install the basics (My preferred IDE: IntelliJ IDEA Community Edition with IdeaVim plugin).
* Ensure you have the Java 7 JRE and JDK from Oracle, so not the OpenJDK.
* Clone this repository.
* run `./updateall.sh` script
* link settings.xml to ~/.m2/settings.xml (or merge the contents with yours)
* Run mvn hpi:run in one of the plugin folders. This will launch a Jenkins instance on http://localhost:8080/.
* If you edit one of the bottom-layer plugins (AdvancedMercurial, FogbugzPlugin), bump all version numbers.
  Tip: Build all plugins with `./build_all.sh`.
* To upload the plugins to live, use the `./upload_all.sh` script.
* The `Fogbugz` project (so not FogbugzPlugin) is not a Jenkins package, it will be included by dependant plugins in the .hpi.
* Read the README on howto to enable the plugins for a build.


.. External references:
.. _Jenkins plugin tutorial: https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial
