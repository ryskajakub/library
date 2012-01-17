package bootstrap.liftweb

import _root_.java.util.Locale

import _root_.net.liftweb.http._
import net.liftweb.http.rest._
import _root_.net.liftweb.http.provider._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import jr.library.snippet.loggedLibrarian
import net.liftweb.common.Full
import net.liftweb.http.auth.AuthRole
import net.liftweb.http.auth.HttpBasicAuthentication//
import jr.library.service._


class Boot {
	val newBookCopy = Menu("bookCopy", S ? "newBook") / "user" / "newBookCopy" >> Hidden
	val scanned = Menu("scanned", S ? "scCd") / "user" / "scannedCode" >> Hidden
	//val scanning = Menu("scanning", S ? "scanning") / "user" / "scanning" >> LocGroup("user")
	val scanning = Menu(Loc("scanning","user" :: "scanning" :: Nil, S ? "scanning",LocGroup("user")))
	val newStuff = Menu("new", S ? "new") / "user" / "new" >> LocGroup("user")
	val index = Menu("index",S ? "lib") / ("index") >> LocGroup("public")
	val createLibrarian = Menu("createLibrarian",S ? "crLibn") ./ ("admin") / "createLibrarian" >> LocGroup("admin")
	val createLibrary = Menu("createLibrary",S ? "crLib") ./ ("admin") / "createLibrary" >> LocGroup("admin")
	val librarianActions = Menu("librarianActions",S ? "librAct") ./ ("admin") / "librarianActions" >> LocGroup("admin")
	val allBooks = Menu("allBooks",S ? "allBooks") / ("user") / ("allBooks") >> LocGroup("user") 
	val allPerson = Menu("allPerson", S ? "allPerson") / "user" / "allPerson" >> LocGroup("user")
	val publicBrowse = Menu("publicBrowse", S ? "publicBrowse") / "public" / "browse" >> LocGroup("public")

	def boot {
		LiftRules.statelessDispatchTable.append(MyRest)
		LiftRules.addToPackages("jr.library")
		LiftRules.localeCalculator = (x) => new Locale("cs")
		LiftRules.resourceNames = "messages" :: LiftRules.resourceNames
		LiftRules.setSiteMap(SiteMap(index,librarianActions,createLibrary,createLibrarian
		  ,newStuff,scanning,scanned,newBookCopy,allBooks,allPerson,publicBrowse))
		LiftRules.statelessRewrite.prepend({
			case RewriteRequest(ParsePath("place" :: place :: Nil,_,_,_),_,_) =>
				RewriteResponse("index" :: Nil,Map(("place") -> place))
		})
		LiftRules.loggedInTest = (Full(() => loggedLibrarian.is.isDefined))
		LiftRules.early.append(makeUtf8) //
	}

	private def makeUtf8(req: HTTPRequest) {
		req.setCharacterEncoding("UTF-8")
	}
}

