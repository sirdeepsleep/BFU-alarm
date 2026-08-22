# installationFireWall

[RU](README.RU.md) / **EN**

A tool for creating stubs and blockers for selected package names

Stub: An empty application with the same package name

Blocker: An empty application with a different package name, creating a permission of the androidX library with the target package name to create a conflict when attempting to install the target package if it uses that library

What this can be used for:

If your phone manufacturer wants to pre-install an unnecessary program for you, you should install the stub before they do it

The blocker serves for a different purpose:
Not to occupy the target package name directly so that it is harder to detect, but to block the installation of the target package due to a permission conflict

This can be used in scenarios when you are pressured to install some program, but you want to refuse covertly, showing that the program does not install
