---
# Allowed version bumps: patch, minor, major
jahia-configuration: minor
---

Deprecated the mail settings of the Jahia configuration: `mailServer`, `mailFrom`, `mailAdministrator` and `mailParanoia`. The configuration ignores them and logs a warning, so configure the mail service instead.
