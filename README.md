# JavaFX example application with Auth0 login

## What is this?

This is a complete JavaFX application skeleton that includes a login/logout process integrated with Auth0. It's based on the real code for one of my desktop apps, I reduced it to showcase only the Auth0 login but could be more simple.

The important part for Auth0 login is in class `Auth0Login.java` but many other utilities and necessary REST client code are included.

It saves the user session in Java preferences and includes gradle tasks to generate a distributable application with JDK included.

Requires: Java 11 for running. Java 14 for jpackage distributable.

Recommended for actual applications using this: Add sentry.io to global error handlers.

## How to run

Follow the Android/Native app tutorial to configure your Auth0 account => https://auth0.com/docs/quickstart/native

Then make sure to fill your Auth0 account details in `Auth0Login.java` file or the login form won't load correctly.

Run:

```bash
./gradlew run
```

Create distributable folder with JDK included:

```bash
./gradlew dist
```

Create installer JDK included:

```bash
./gradlew distInstaller
```

But if you want to distribute properly done Windows installers you should use InnoSetup or similar. Check: https://github.com/DomGries/InnoDependencyInstaller
