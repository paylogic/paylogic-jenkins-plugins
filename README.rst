paylogic-jenkins-plugins
========================

This package contains a set of plugins to help you to integrate Fogbugz with Jenkins.
These are currently still in development, maintained by Paylogic, and it is not guaranteed that they are bug free.

These plugins perform the following tasks:

- Fogbugz: a Java interface to the Fogbugz 7 XML API.
- FogbugzPlugin: a Jenkins plugin that reports build status to Fogbugz using 'Fogbugz'
- GatekeeperPlugin: a Jenkins plugin that merges a feature branch to a release branch (meant to run before tests).
  Also merges current release branch to next release branch until there are no further releases available.
  This also pushes the upmerged content, including Gatekeeper merge if present.


Note: The folders in this package are different plugins belonging to their own repository.


Development environment
-----------------------

To start to set up the development environment, please copy the content of the settings.xml file to your own settings.xml file.
If it does not exist, create a file named 'settings.xml' under ~/.m2/. This is necessary because it contains
the URL to Jenkins repository. Then, you can use the `make` tool to get an up-to-date revision:

.. code-block:: shell

    make pull

If you want to run tests:

.. code-block:: shell

    make test

And, if you want to build the plugins:

.. code-block:: shell

    make build


Deployment to jenkins instance
------------------------------

Once you have updated a plugin, you can update it to Jenkins. In order to do it, use the following command:

.. code-block:: shell

    make upload URL=http://jenkins.example.com

Please note that you need to specify the URL of the Jenkins instance.


Deployment to jenkins-ci.org
----------------------------

Follow the guide at: https://wiki.jenkins-ci.org/display/JENKINS/Hosting+Plugins
In short: configure maven for your user and password, make sure the version has SNAPSHOT suffix and:

.. code-block:: shell

    mvn release:clean release:prepare release:perform



Deployment to Maven Central (for java-fogbugz)
----------------------------------------------

See: https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide
Our namespace (org.paylogic) already has sync enabled from sonatype to central.


How to get a build using all the plugins running
------------------------------------------------

* Install all .hpi files by uploading them to the Jenkins plugin manager (or use uploadall.sh script).
    * Also install the Mercurial and Multiple-SCMs plugins from the Jenkins marketplace.
* Go to Jenkins' global settings page and set:
    * Your fogbugz information (you need to get a api token manually)
    * The case to build on a fogbugz trigger (listed under fogbugz settings)
* Create or edit a build and set the following:
    * Make your build parametrized, and include 'CASE_ID' (and optionally REPO_SUBDIR for MultiSCM) string parameters.
    * (Optional) If you don't need to defer branch info from fogbugs, have those parameters on your job:
        * TARGET_BRANCH
            branch we will merge into, aka mainline or release branch
        * FEATURE_BRANCH
            branch which will be merged into mainline, aka feature branch
        * REPO_PATH
            repository path to be added to 'base' repo url (empty by default)
    * (Optional) If you use Multi-SCM, make your repository to merge on have $REPO_SUBDIR as path to checkout in.
      Do not use '$REPO_SUBDIR' literally as the MultiSCM subfolder parameter. This will not work. Just make sure the two are the same.
    * (Optional) set a build name like this: 'Case ${ENV, var="CASE_ID"} - Branch ${ENV, var="NODE_ID"} || Build #${BUILD_NUMBER}'
    * Ensure you have the following build steps in this order:
        * Add the 'Perform Gatekeeper merge' step and check the checkbox
        * Add your build and tests steps
        * Add the 'Perform Gatekeeper commit' step and check the checkbox
        * Add the 'Perform Upmerging of release branches' step
        * Add the 'Perform a Mercurial Push command' step
    * Under post-build actions add:
        * Add the 'Add fogbugz link to case on build page' action
        * Add the 'Report status to related fogbugz case' action


Contact
-------

If you have questions, bug reports, suggestions, etc. please create an issue on
the `GitHub project page`_. The latest version of ``Paylogic Jenkins Plugins`` will always be
available on GitHub.


License
-------

This software is licensed under the `MIT license`_

© 2013 `Maikel Wever`_ and Paylogic_ International.


.. External references:
.. _MIT license: http://en.wikipedia.org/wiki/MIT_License
.. _Paylogic: http://www.paylogic.com/
.. _GitHub project page: https://github.com/paylogic/paylogic-jenkins-plugins
.. _Maikel Wever: https://github.com/maikelwever/
