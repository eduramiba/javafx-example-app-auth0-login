# JavaFX example application with Auth0 login

## What is this?

This is a complete JavaFX application skeleton that includes a login/logout process integrated with Auth0.

The imoprant part for Auth0 login is in class `Auth0Login.java` but many other utilities and necessary REST client code is included.

## How to run

Follow the Android/Native app tutorial to configure your Auth0 account => https://auth0.com/docs/quickstart/native

Then make sure to fill your Auth0 account details in `Auth0Login.java` file.

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