Amazon Web Services utils
=========================

This project contains utilities that are used in multiple Amazon services.

The Aws object
--------------

This object is mainly used to wrap Play's `WS` component and add signing to it. Usage is like this:

```
Aws
  .withSigner(...)
  .url(...)
 	
  ...
 	
Aws
  .withSigner3(...)
  .url(...)
```

The Signers
-----------

The current version supplies Aws version 3 and Aws version 4 signers.

Installation
------------

Add the following dependency:

``` scala
"nl.rhinofly" %% "api-aws-utils" % "1.0"
```
