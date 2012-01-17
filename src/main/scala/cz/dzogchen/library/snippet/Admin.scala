/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.dzogchen.library.snippet

import java.util.logging.Logger
import net.liftweb.http._
import S._
import net.liftweb.util._
import Helpers._
import scala.xml._
import cz.dzogchen.library._
import cz.dzogchen.library.service._
import cz.dzogchen.library.model._
import scala.collection.JavaConversions._
import scala.collection.mutable.Set

class LibrarySnippet {
	object editedLibrary extends RequestVar[Library](new Library)
	def create (content:NodeSeq) = {
		val lib = editedLibrary.is
		var password = ""
		bind("entry",content,
			  "createPassword" -%> SHtml.password("",password = _),
			  "place" -> SHtml.text(lib.place, lib.place=_),
			  "submit" -> SHtml.submit("Kaboom", () => {
					if(password == "[10}G?Rv")
						Service.library.save(lib)
				})
		)
	}
	def modify (content:NodeSeq) = {
		val all = Service.library.findAll
		val anchors = all.map((x:Library) => SHtml.link("/admin/createLibrary",
			() => editedLibrary(x), new Text(x.getPlace))).map(
		{
			x:NodeSeq =>
			<li>{x}</li>
		}).foldLeft(NodeSeq.Empty)(_++_)
		bind("entry",content, "anchors" -> anchors)
	}
}
class LibrarianSnippet {
	object editedLibrarian extends RequestVar[Librarian](new Librarian)
	def create (xhtml:NodeSeq) = {
		val librarian = editedLibrarian.is
		val allLibraries = Service.library.findAll
		def checkboxes(x:Library):NodeSeq = {
			SHtml.checkbox(librarian.getPermittedLibraries.contains(x),
				(checked:Boolean) => {
					if(checked){
						if(!librarian.getPermittedLibraries.contains(x))
							librarian.getPermittedLibraries += x
						x.getLibrarians += librarian
					} else {
						librarian.getPermittedLibraries -= x
						x.getLibrarians -= librarian
					}
				}) ++ <label>{x.getPlace}</label>
		}
		var password = ""
		bind("entry",xhtml,
			  "name" -> SHtml.text(librarian.name, librarian.name=_),
			  "createPassword" -%> SHtml.password("",password = _),
			  "canDelete" -> SHtml.checkbox(librarian.canDelete,librarian.canDelete = _),
			  "password" -> SHtml.text("", (x:String) =>
				if(x != null || x != "")
				librarian.password = Helpers.hash256(x)),
			  "submit" -> SHtml.submit(S ?"submit", () =>{
					if(password == "[10}G?Rv")
						Service.librarian.save(librarian)
				}),
			  "permittedLibraries" -> ( allLibraries
												.map((x:Library) => checkboxes(x))
												.foldLeft(NodeSeq.Empty)(_++_)))
	}
	def modify (content:NodeSeq) = {
		val all = Service.librarian.findAll
		all.flatMap((x:Librarian) => {
			bind("entry",content,
				  "anchors" -%> SHtml.link("/admin/createLibrarian",
						() => editedLibrarian(x) ,new Text(x.name)
				)
			)
		})
	}
}

class LibrarianActionSnippet{
	def render(xhtml:NodeSeq) = {
		val all = Service.librarianAction.findAll
		all.flatMap((x:LibrarianAction) => {
			Helpers.bind("entry",xhtml,
				  "description" -> new Text(x.description),
				  "date" -> new Text(x.date.toString),
				  "librarian" -> new Text(if(x.librarian == null) "" else x.librarian.name),
				  "action" -%> Text(x.actionType)
			)
		})
	}
}