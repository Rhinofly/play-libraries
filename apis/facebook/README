The first version will allow you to access facebook like this:

``` scala

//Create a facebook object definition
class FacebookUser extends AbstractUser with Name with Email

//Use it in an action
def myAction = FacebookAuthenticated {facebookUserPromise:Promise[FacebookUser] =>
  Async {
    facebookUserPromise.map { facebookUser =>
      Ok(facebookUser.name + ": " + facebookUser.email)
    }
  }
}

```
