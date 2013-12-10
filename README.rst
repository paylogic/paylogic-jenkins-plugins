paylogic-jenkins-plugins
========================

Some Jenkins plugins we developed at Paylogic.
These are currently still in development, so no guarantees there.

Note: the folders here are all submodules, as we've moved every plugin to it's own repo.


Plugins do the following:

- RedisPlugin: a Jenkins plugin that provides Redis connections for use by other plugins.
- Fogbugz: a Java interface to the Fogbugz 7 XML API.
- FogbugzPlugin: a Jenkins plugin that reports build status to Fogbugz using 'Fogbugz'
- AdvancedMercurialManager: a Java class that executes update, commit and merge commands for Mercurial. Parses output of that as well.
  Also provides a task that pushes branches put in Redis by Gatekeeper/Upmerge plugins.
- GatekeeperPlugin: a Jenkins plugin that merges a feature branch to a release branch (meant to run before tests).
- UpmergePlugin: a Jenkins plugin that merges current release branch to next release branch until there are no further releases available.



How to get a build using all the plugins running
------------------------------------------------

* Install all .hpi files by uploading them to the Jenkins plugin manager.
    * Ensure you have installed the seperately provided Redis plugin.
    * Also install the Mercurial and Multiple-SCMs plugins from the Jenkins marketplace.
* Go to Jenkins' global settings page and set:
    * Your redis server
    * Your fogbugz information (you need to get a api token manually)
    * The case to build on a fogbugz trigger (listed under fogbugz settings)
* Create or edit a build and set the following:
    * Make your build parametrized, and include 'CASE_ID' (and optionally REPO_SUBDIR for MultiSCM) string parameters.
    * (Optional) If you use Multi-SCM, make your repository to merge on have $REPO_SUBDIR as path to checkout in. 
      Do not use '$REPO_SUBDIR' literally as the MultiSCM subfolder parameter. This will not work. Just make sure the two are the same.
    * (Optional) set a build name like this: 'Case ${ENV, var="CASE_ID"} - Branch ${ENV, var="NODE_ID"} || Build #${BUILD_NUMBER}'
    * Ensure you have the following build steps in this order:
        * Add the 'Perform Gatekeepering' step and check the checkbox
        * Add your build and tests steps
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
.. _Jenkins Redis plugin: https://github.com/paylogic/jenkins-redis-plugin/
