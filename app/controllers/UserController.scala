package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent.Future
import com.unboundid.ldap.sdk._
import com.unboundid.ldap.sdk.{Filter => LdapFilter}
import scala.collection.JavaConversions._
import play.api.libs.json._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class UserController @Inject()(configuration: play.api.Configuration) extends Controller {

  //TODO: Pack4j demo, or place in OpenID/JWT/Single sign on ourselves
  // passing credentials OpenId- https://stackoverflow.com/questions/39076085/should-i-pass-credentials-via-http-header-or-post-body-as-json-to-rest-api
  // pack4j demo- https://github.com/pac4j/play-pac4j-scala-demo
  private val url = configuration.underlying.getString("ldap.dc")
  private val port = configuration.underlying.getString("ldap.port")
  private var adminDN: String = configuration.underlying.getString("ldap.admin_dn")
  private var adminPass: String = configuration.underlying.getString("ldap.admin_pass")
  private val adminConnection: LDAPConnection = new LDAPConnection()
  private val userConnection: LDAPConnection = new LDAPConnection()
  try {
    adminConnection.connect(url, 389)
    userConnection.connect(url, 389)
  }
  catch {
    case e: Exception =>
      println("initial connection failed")
  }

  def login = Action {
    implicit request =>
      loginResult(request)
  }

  def loginResult(request: Request[AnyContent]): Result = {
    try {
      adminConnection.bind(adminDN, adminPass)
      println("admin success")
    }
    catch {
      case e: Exception =>
        println("admin bind failed", adminDN, adminPass)
    }
    var (username, password) = ("", "")
    try {
      var json = request.body.asJson
      json.foreach { data =>
        print("parsing json: ")
        username = (data \ "username").as[String] //asFormUrlEncoded.get("username").mkString("")
        password = (data \ "password").as[String] //asFormUrlEncoded.get("password").mkString("")
      }
      println("username", username)
    }
    catch {
      case e: Exception =>
        return Status(400)("Error parsing credential json")
    }
    // Construct Filter to find user
    var findUserfilter: LdapFilter = null
    findUserfilter = LdapFilter.createEqualityFilter(configuration.underlying.getString("ldap.search_name"), username)
    // Create a Search Request given the user's info.
    var searchRequest: SearchRequest = new SearchRequest(configuration.underlying.getString("ldap.search_group"), SearchScope.SUB, findUserfilter)
    searchRequest.setSizeLimit(1) // We will error if we get more than one hit
    var searchResult: SearchResult = null
    //Attempt to search for the user's DN
    try {
      searchResult = adminConnection.search(searchRequest)
    }
    catch {
      case e: LDAPSearchException =>
        return Status(400)("Error getting search results")
    }
    var userDN: String = null
    //TODO: Convert to json?
    var user: SearchResultEntry = null
    //Notifications for search requests counts != 1.
    if (searchResult.getEntryCount > 1) {
      println("We got more than one Entry for:" + searchRequest.getFilter)
    }
    // Search for the user's DN
    // Attempt to log the user in
    try {
      searchResult.getSearchEntries.toList.foreach { entry =>
        userDN = entry.getDN
        user = entry
      }
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
    catch {
      case e: Exception =>
        Status(401)("Username or password is incorrect")
    }
  }
}