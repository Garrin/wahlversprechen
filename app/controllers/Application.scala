package controllers

import scala.concurrent.duration._

import play.api._
import play.api.cache._
import play.api.data._
import play.api.templates._
import play.api.mvc._
import play.api.data.Forms._
import play.api.Play.current

import models._
import models.Rating._
import views._

object Application extends Controller with Secured with Cached {
	override val cachePrefix = "app"

	// Authentification
	val loginForm = Form(
		tuple(
			"email" -> text,
			"password" -> text) verifying ("Ungültige E-Mail oder falsches Passwort", result => result match {
				case (email, password) => User.authenticate(email, password).isDefined
			}))

	def login = HTTPS { implicit request =>
		Ok(html.login(loginForm))
	}

	def authenticate = Action { implicit request =>
		loginForm.bindFromRequest.fold(
			formWithErrors => BadRequest(html.login(formWithErrors)),
			user => Redirect(routes.Application.index).withSession("email" -> user._1))
	}

	def logout = Action {
		Redirect(routes.Application.login).withNewSession.flashing(
			"success" -> "Du wurdest erfolgreich ausgeloggt")
	}

	// Index page
	def index = CachedAction("index") { implicit request =>
		val optuser = user(request)

		val oauthor = Author.loadRated
		val statistics = oauthor match {
			case Some(author) => Statement.countRatings(author)
			case None => (1, Map.empty[Rating, Int])
		}
		Logger.debug("Statements: " + statistics._1 + " Statistics: " + statistics._2.toString)
		Ok(views.html.index(statistics._1, statistics._2, Statement.byEntryDate(oauthor, Some(5)), Statement.byImportantTag(oauthor, Some(5)), user(request)))
	}
	
	def recent = CachedAction("recent") { implicit request => 		
		val mapstmtByAuthor = Statement.byEntryDate(None, None).groupBy(_.author)
		Ok(views.html.viewStatementList("Alle Wahlversprechen nach letzter Aktualisierung", mapstmtByAuthor, false, user(request)))
	}

	def top = CachedAction("top") { implicit request =>  
		Ok(views.html.viewStatementList("Die wichtigsten Wahlversprechen nach Ressorts", Statement.byImportantTag(None, None).groupBy(_.author), true, user(request)))
	}

	def all = CachedAction("all") { implicit request => 
		Ok(views.html.viewStatementList("Alle Wahlversprechen nach Ressorts", Statement.all(), true, user(request)))
	}

	def tag(tag: String) = CachedAction("tag." + tag) { implicit request =>
		Ok(views.html.viewStatementList("Wahlversprechen mit Tag '"+tag+"'", Statement.byTag(tag, None, None).groupBy(_.author), false, user(request)))		
	}

	def category(category: String) = CachedAction("category."+category) { implicit request => 
		Ok(views.html.viewStatementList("Wahlversprechen aus dem Ressort '"+category+"'", Statement.byCategory(category, None, None).groupBy(_.author), false, user(request)))		
	}

	def rating(ratingId: Int) = CachedAction("rating."+ratingId) { implicit request => 
		if(0<= ratingId && ratingId < Rating.maxId ) {
			val rating = Rating(ratingId)
			Ok(views.html.viewStatementList("Wahlversprechen mit Bewertung '"+Formatter.name(rating)+"'", Statement.byRating(rating, None, None).groupBy(_.author), true, user(request)))		
		} else {
			NotFound
		}
	}

	def search(query: String) = CachedAction("search."+query) { implicit request => 
		val mapstmtByAuthor = Statement.find(query)
		Ok(views.html.viewStatementList("Suchergebnisse für '" + query + "'", mapstmtByAuthor, false, user(request) ))
	}

	import play.api.libs.json._
	def itemsByAuthorAsJSON(authorName: String) = CachedAction("items.json." + authorName) { implicit request => 
		Author.load(authorName) match {
			case None => NotFound
			case Some(author) => {
				Ok( Json.toJson( Statement.filter(author = Some(author)) ) )
			}
		}
	}

	def tagsAsJSON() = CachedAction("tags.json") { implicit request =>
		Ok( Json.toJson( Tag.loadAll() ) )
	}

	def categoriesAsJSON() = CachedAction("categories.json") { implicit request =>
		Ok( Json.toJson( Category.loadAll() ) )
	}

	def updatesAsFeed = CachedAction("updatesAsFeed") { implicit request =>
		Ok(views.xml.entryList("wahlversprechen2013.de: Alle Aktualisierungen", routes.Application.recent.absoluteURL(false), Entry.loadRecent(10)))			
	}
	
	def loader_io = Action {
		Ok("loaderio-a8c18c9612671703651ea5e79d55623e")
	}
}

trait ControllerBase {
	def username(request: RequestHeader) = request.session.get("email")
}

/**
 * Provide security features
 */
trait Secured extends ControllerBase {
	def user(request: RequestHeader) = username(request) flatMap { User.load(_) }

	def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)

	def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
		Action(request => f(user)(request))
	}

	def IsAdmin(f: => User => Request[AnyContent] => Result) = IsAuthenticated { username =>
		request =>
			val u = User.load(username)
			if (u.isDefined && u.get.role == Role.Admin) {
				f(u.get)(request)
			} else {
				Results.Forbidden
			}
	}

	def IsEditor(f: => User => Request[AnyContent] => Result) = IsAuthenticated { username =>
		request =>
			val u = User.load(username)
			if (u.isDefined && (u.get.role == Role.Admin || u.get.role == Role.Editor)) {
				f(u.get)(request)
			} else {
				Results.Forbidden
			}
	}

	/** Called before every request to ensure that HTTPS is used. */
	def HTTPS(f: => Request[AnyContent] => Result) = Action { request =>
		import play.api.Play.current
		if (Play.isDev
			|| request.headers.get("x-forwarded-proto").isDefined
			&& request.headers.get("x-forwarded-proto").get.contains("https")) {
			f(request)
		} else {
			Results.Redirect("https://"+request.host + request.uri);
		}
	}
}

object CacheFormat extends Enumeration {
	type CacheFormat = Value
	val HTML, JSON, RSS = Value
};
import CacheFormat._

trait Cached extends ControllerBase {
	def cachePrefix: String

	private def cacheId(id: Any, format: CacheFormat) = cachePrefix + "." + format + "." + id

	def invalidateCaches(id: Any) {
		for( f <- 0 until CacheFormat.maxId ) Cache.remove( cacheId(id, CacheFormat(f)) )
	}

	def CachedEternal(id: Any, format: CacheFormat)(f: Request[AnyContent] => Result): Action[AnyContent] = {	
		CachedAction(cacheId(id, format), 0.seconds)(f)
	}

	def CachedAction(key: String, duration: Duration = 30.minutes)(f: Request[AnyContent] => Result): Action[AnyContent] = {
	  Action { request =>
	    username(request) match {
	    	case Some(user) => {
	    		Logger.debug("Uncached request. Key = " + key)
	    		f(request)
	    	}
	    	case None => {
	    		Logger.debug("Cached request. Key = " + key + " duration = " + duration.toSeconds);
	    		Cache.getOrElse( key, duration.toSeconds.toInt ) { 
	    			Logger.debug("Added request to cache. Key = " + key);
	    			f(request) 
	    		}
	    	}
	    }
	  }
	}
}
