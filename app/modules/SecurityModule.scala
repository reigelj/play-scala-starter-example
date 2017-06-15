package modules

import java.time.Clock

import com.google.inject.AbstractModule
import org.pac4j.http.client.direct.ParameterClient
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.oidc.client.OidcClient
import org.pac4j.oidc.config.OidcConfiguration
import org.pac4j.oidc.profile.OidcProfile
import org.pac4j.play.{CallbackController, LogoutController}
import services.{ApplicationTimer, AtomicCounter, Counter}

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.
  *
  * Play will automatically use any class called `Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class SecurityModule extends AbstractModule {

  override def configure(): Unit = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])
    // OpenID Connect
    val oidcConfiguration = new OidcConfiguration()
    //TODO: https://console.developers.google.com/apis/credentials
    oidcConfiguration.setClientId("343992089165-i1es0qvej18asl33mvlbeq750i3ko32k.apps.googleusercontent.com")
    oidcConfiguration.setSecret("unXK_RSCbCXLTic2JACTiAo9")
    oidcConfiguration.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration")
    oidcConfiguration.addCustomParam("prompt", "consent")
    //TODO: Roles
    val oidcClient = new OidcClient[OidcProfile](oidcConfiguration)
    oidcClient.addAuthorizationGenerator(new RoleAdminAuthGenerator)

    // REST authent with JWT for a token passed in the url as the token parameter
    val jwtAuthenticator = new JwtAuthenticator()
    jwtAuthenticator.addSignatureConfiguration(new SecretSignatureConfiguration("12345678901234567890123456789012"))
    val parameterClient = new ParameterClient("token", jwtAuthenticator)
    parameterClient.setSupportGetRequest(true)
    parameterClient.setSupportPostRequest(false)
    // callback
    val callbackController = new CallbackController()
    callbackController.setDefaultUrl("/?defaulturlafterlogout")
    callbackController.setMultiProfile(true)
    bind(classOf[CallbackController]).toInstance(callbackController)

    // logout
    val logoutController = new LogoutController()
    logoutController.setDefaultUrl("/")
    bind(classOf[LogoutController]).toInstance(logoutController)
  }
}
