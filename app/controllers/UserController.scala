package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent.Future
import com.unboundid.ldap.sdk._
import com.unboundid.ldap.sdk.{ Filter => LdapFilter }
import scala.collection.JavaConversions._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class UserController @Inject() extends Controller {

  //TODO: Pack4j demo, or place in OpenID/JWT/Single sign on ourselves
  // passing credentials OpenId- https://stackoverflow.com/questions/39076085/should-i-pass-credentials-via-http-header-or-post-body-as-json-to-rest-api
  // pack4j demo- https://github.com/pac4j/play-pac4j-scala-demo
  //TODO: Place url, port, pass in the environment vars. application.conf ${?URL} https://www.playframework.com/documentation/2.5.x/ProductionConfiguration
  private val url = "wolf.ad.usamco.com"
  private val port = 389
  private val adminConnection: LDAPConnection = new LDAPConnection()
  private val userConnection: LDAPConnection = new LDAPConnection()
  try{
    adminConnection.connect(url, 389)
    userConnection.connect(url, 389)
  }
  catch{
    case e:Exception=>
      println("initial connection failed")
  }
  try{
      adminConnection.bind("cn=usanet,cn=users,dc=ad,dc=usamco,dc=com", "pr1m@ry")
      println("admin success")
  }
  catch{
    case e:Exception=>
      println("admin bind failed")
  }
  def login = Action {
    implicit request =>
      val username = request.body.asFormUrlEncoded.get("username").mkString("")
      val password = request.body.asFormUrlEncoded.get("password").mkString("")
      // Construct Filter to find user
      var findUserfilter: LdapFilter = null
      findUserfilter = LdapFilter.createEqualityFilter("sAMAccountName", username)
      // Create a Search Request given the user's info.
      var searchRequest: SearchRequest= new SearchRequest("cn=users,dc=ad,dc=usamco,dc=com", SearchScope.SUB, findUserfilter)
      searchRequest.setSizeLimit(1) // We will error if we get more than one hit
      var searchResult: SearchResult = null
      //Attempt to search for the user's DN
      try
      {
          searchResult = adminConnection.search(searchRequest)
          println(searchResult)
      }
      catch
      {
        case e:LDAPSearchException=>
          // TODO Auto-generated catch block
          Status(400)("Error getting search results")
      }
      var userDN: String = null
      //Notifications for search requests counts != 1.
      if (searchResult.getEntryCount() > 1)
      {
          println("We got more than one Entry for:" + searchRequest.getFilter())
      }
      if (searchResult.getEntryCount() == 0)
      {
          println("We got No Entries for:" + searchRequest.getFilter())
      }
      // Search for the user's DN
      searchResult.getSearchEntries().toList.foreach{entry =>
          userDN = entry.getDN()
          println("Found an Entry: " + userDN)
      }
      val userBindRequest: SimpleBindRequest = new SimpleBindRequest(userDN, password)
      // Check for empty usernames/passwords
      if (userBindRequest.getBindDN() == null)
      {
          println("We got a null for the userBindRequest UserDN and therefore the bind is anonymous !")
      }
      if (userBindRequest.getPassword() == null)
      {
          println("We got a null for the userBindRequest Password and therefore the bind is anonymous !")
      }
      // Attempt to log the user in
      try
      {
          userConnection.bind(userDN, password)
          Ok("Success")
      }
      catch
      {
        case e:LDAPException=>
          Status(400)("user bind exception")
      }
  }
}