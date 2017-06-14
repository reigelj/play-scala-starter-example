package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent.Future
import com.unboundid.ldap.sdk._
import com.unboundid.ldap.sdk.{ Filter => LdapFilter }
import scala.collection.JavaConversions._
import play.api.libs.json._

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
        println(request.body, request.body.asJson)
        loginResult(request)
  }
  // TODO: Bad practice? Remove returns? https://tpolecat.github.io/2014/05/09/return.html
  def loginResult(request: Request[AnyContent]): Result = {
      var json = request.body.asJson
      var (username, password) = ("", "")
      try{
        json.map {data=>
          print("parsing json: ")
          username = (data \ "username").as[String]//asFormUrlEncoded.get("username").mkString("")
          password = (data \ "password").as[String]//asFormUrlEncoded.get("password").mkString("")
        }
        println("username", username)
      }
      catch{
        case e:Exception=> 
          return Status(400)("Error parsing credential json")
      }
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
          return Status(400)("Error getting search results")
      }
      var userDN: String = null
      //TODO: Convert to json?
      var user: SearchResultEntry = null
      //Notifications for search requests counts != 1.
      if (searchResult.getEntryCount() > 1)
      {
          return Status(400)("We got more than one Entry for:" + searchRequest.getFilter())
      }
      if (searchResult.getEntryCount() == 0)
      {
          return Status(400)("We got No Entries for:" + searchRequest.getFilter()) 
      }
      // Search for the user's DN
      searchResult.getSearchEntries().toList.foreach{entry =>
          userDN = entry.getDN()
          user = entry
          println("Found an Entry: " + entry)
      }
      val userBindRequest: SimpleBindRequest = new SimpleBindRequest(userDN, password)
      // Check for empty usernames/passwords
      if (userBindRequest.getBindDN() == null)
      {
          return Status(400)("We got a null for the userBindRequest UserDN and therefore the bind is anonymous !")
      }
      if (userBindRequest.getPassword() == null)
      {
          return Status(400)("We got a null for the userBindRequest Password and therefore the bind is anonymous !")
      }
      // Attempt to log the user in
      try
      {
          println(userDN)
          userConnection.bind(userDN, password)
           Ok(Json.obj(
            "sAMAccountName" -> user.getAttributeValue("sAMAccountName"),
            "firstName" -> user.getAttributeValue("givenName"), 
            "lastName" -> user.getAttributeValue("sn"),
            "company" -> user.getAttributeValue("company"), 
            "department" -> user.getAttributeValue("department"), 
            "title" -> user.getAttributeValue("title"),
            "description" -> user.getAttributeValue("description"), 
            "officeCity" -> user.getAttributeValue("physicalDeliveryOfficeName"),
            "state" -> user.getAttributeValue("st"),
            "email" -> user.getAttributeValue("mail")
            ))
          //TODO: Create and pass back JWT & entry; Example from acuts:
          //var token = authorization.signToken(user);
          //res.json({ user: user, token: token });
      }
      catch
      {
        case e:LDAPException=>
          return Status(400)("user bind exception")
      }
  }
}