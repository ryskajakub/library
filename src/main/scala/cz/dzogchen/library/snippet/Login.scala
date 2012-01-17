/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.dzogchen.library.snippet

import java.util.logging.Logger
import net.liftweb.http._
import net.liftweb.http.provider.HTTPRequest
import net.liftweb.util._
import S._
import net.liftweb.common._
import scala.xml._
import Helpers._
import cz.dzogchen.library._
import cz.dzogchen.library.service._
import cz.dzogchen.library.model._
import scala.collection.JavaConversions._

object loggedLibrarian extends SessionVar[Option[Tuple2[Librarian,Library]]](None)
class Login {
	object message extends RequestVar[String]("")
	def render (content:NodeSeq) = {
		var password = ""
		var username = ""
		lazy val allLibraries = Service.library.findAll
		val param = S.param("place").flatMap(Service.library.findByPlace(_))
		var library:Either[Library,Library] = param match{
			case Full(x) => Right(x)
			case _ => Left(allLibraries.headOption.getOrElse(new Library))
		}
		def textOrObject(x:Either[Library,Library]):NodeSeq = {
			val a:NodeSeq = x match{
				case Right(x) =>
					Text(x.place)
				case Left(_) =>
					SHtml.selectObj(
						allLibraries.map((x:Library) => x -> x.place),
						Empty,
						((x:Library) => library = Left(x))
					)
			}
			a
		}
		Helpers.bind("entry",content,
						 "username" -%> SHtml.text("", username = _),
						 "password" -%> SHtml.password("", password = _),
						 "library" -> textOrObject(library),
						 "submit" -%> SHtml.submit(S ? "log",
															() => {
					val l = Service.librarian.login(username,Helpers.hash256(password),library.merge)
					loggedLibrarian(l)
					l match{
						case Some(l) => S.redirectTo("/user/new")
						case _ => S.redirectTo("/")
					}
				})
		)
	}
	def loggedIn(xhtml:NodeSeq):NodeSeq = {
		val l = loggedLibrarian.is
		val (username,logout,library) = l match {
			case Some(Tuple2(x,y)) => Tuple3(
					Text(x.name),
					SHtml.link("/",() => loggedLibrarian(None),Text(S ?"logout")),
					Text(y.place))
			case _ =>
				Tuple3(Text((xhtml \\ "entry:username").text),NodeSeq.Empty,Text(""))
		}
		Helpers.bind("entry",xhtml,
						 "username" -%> username,
						 "logout" -%> {(x:NodeSeq) => {SHtml.link("/", () => loggedLibrarian(None),x)}},
						 "library" -%> library
		)
	}
	private def makeUtf8(req: HTTPRequest) {
		req.setCharacterEncoding("UTF-8")
	}
}
class BackToSlash{
	def render(xhtml:NodeSeq) = {
		Helpers.bind(
			"link",
			xhtml,
			"slash" -%> {(x:NodeSeq) => SHtml.link("/", () => (), x)}
		)
	}
}
