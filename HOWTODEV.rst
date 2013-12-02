Working on these Jenkins plugins
================================

To get a local environment running, follow these steps:

* Follow the `Jenkins plugin tutorial`_ to install the basics (My preferred IDE: IntelliJ IDEA Community Edition with IdeaVim plugin).
* Ensure you have the Java 7 JRE and JDK from Oracle, so not the OpenJDK.
* `Set up a local Apache Archiva instance.`_ (Alternatively, get access on my local Archiva instance).
* Clone this repository.
* Put the URL to your Archiva instance in the pom.xml files, under `distributionManagement` (we should fix this and set up a LAN Archiva instance).
* Run mvn hpi:run in one of the plugin folders (assuming you added the recommended settings to ~/.m2/settings.xml).
  This will launch a Jenkins instance on http://localhost:8080/.
* If you edit one of the bottom-layer plugins (AdvancedMercurial, FogbugzPlugin), bump all version numbers.
  Tip: Build all plugins with `./build_all.sh`.
* To upload the plugins to live, use the `build_all` script or run `mvn deploy` or `mvn package` to generate a .hpi file in the `target/` folder.
* If you want to edit the `Redis`_ plugin, clone that repository as well. Same instructions apply.
* The `Fogbugz` project (so not FogbugzPlugin) is not a Jenkins package, it will be included by dependant plugins in the .hpi.


Because I went mad with the Seperation of Concerns, we now have a dependency hell. Got suggestions to fix this? Please do tell.


.. External references:
.. _Jenkins plugin tutorial: https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial
.. _Set up a local Apache Archiva instance.: http://archiva.apache.org/docs/1.4-M4/quick-start.html
.. _Redis: http://github.com/paylogic/jenkins-redis-plugin/
