paylogic-jenkins-plugins
========================

Some Jenkins plugins we developed at Paylogic.
These are currently still in development, so no guarantees there.


Plugins do the following:

- Fogbugz: a Java interface to the Fogbugz 7 XML API.
- FogbugzPlugin: a Jenkins plugin that reports build status to Fogbugz using 'Fogbugz'
- AdvancedMercurialManager: a Java class that executes update, commit and merge commands for Mercurial. Parses output of that as well.
  Also provides a task that pushes branches put in Redis by Gatekeeper/Upmerge plugins.
- GatekeeperPlugin: a Jenkins plugin that merges a feature branch to a release branch (meant to run before tests).
- UpmergePlugin: a Jenkins plugin that merges current release branch to next release branch until there are no further releases available.


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
