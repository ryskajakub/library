package jr.library.snippet

import java.text.Collator
import java.util.Locale
import java.util.logging.Logger
import net.liftweb.http._
import S._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Run
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.js.jquery.JqJsCmds.DisplayMessage
import net.liftweb.util._
import Helpers._
import scala.xml._
import jr.library._
import jr.library.service._
import jr.library.model._
import scala.collection.JavaConversions._
import scala.collection.JavaConversions
import scala.collection.mutable.{Set => MSet}

class NewBook {
	object editedBook extends RequestVar[Book](new Book)
	def book(xhtml:NodeSeq) = {
		val noop = (x:String) => ()
		val comp = Collator.getInstance(new Locale("cs"))
		def sort(x:Author,y:Author) = {
			if (x.books.size == y.books.size)
				comp.compare(x.name ,y.name) < 0
			else
				x.books.size > y.books.size
		}
		val edited = editedBook.is
		val allAuthors = Service.author.findAll.sortWith(sort)
		val isNewBook = (edited.id == null)
		bind("book",xhtml,
			  "name" -%> SHtml.text(edited.name, edited.name=_),
			  "authors" -%> SHtml.multiSelectObj(
			 	allAuthors.map((x:Author) => Tuple2(x,x.name + " " + x.books.size.toString))
				,edited.authors.toSeq,
				(authors:List[Author]) => {
					edited.setAuthors(MSet(authors:_*))
				}),
			  "submit" -%> SHtml.submit(if(isNewBook) (S ?"newBook") else S ? "chBook",{
					() => {
						Service.book.save(edited)
						val la = new LibrarianAction
						la.description = Service.book.findByName(edited.name).get.id
						la.librarian = loggedLibrarian.is.get._1
						la.actionType = if(isNewBook) LibrarianActionType.bookCreated else LibrarianActionType.bookChanged
						Service.librarianAction.save(la)
					}
				})
		)
	}
	def allBooks (content:NodeSeq):NodeSeq = {
		val all = Service.book.findAll
		val anchors = all.flatMap((x:Book) => {
				bind("book",content,"name" -> SHtml.link("/user/new",
																	  () => editedBook(x), new Text(x.name)))
			})
		anchors
	}
}
class NewAuthor{
	object editedAuthor extends RequestVar[Author](new Author)
	def author(xhtml:NodeSeq) = {
		val edited = editedAuthor.is
		val newAuthor = edited.id == null
		var authorBox = ""
		def submit = {
			val label = if (newAuthor) S ? "newAuth" else S ? "chAuth"
			def fun() : JsCmd = {
				if(edited.name.length < 1)
					return SetHtml(authorBox,Text(S ? "nameLength"))
				if(!Service.author.isNameAvailable(edited.name))
					return SetHtml(authorBox,Text(S ? "nameUnavailable"))
				Service.author.save(edited)
				val la = new LibrarianAction
				la.description = Service.author.findByName(edited.name).get.id
				la.librarian = loggedLibrarian.is.get._1
				la.actionType = if(newAuthor) LibrarianActionType.authorCreated else LibrarianActionType.authorChanged
				Service.librarianAction.save(la)
				Run("location.reload(true)")
			}
			SHtml.ajaxButton(label, () => fun)
		}
		bind(
			"author",xhtml,
			"name" -%> {
				(id:NodeSeq) => {
				authorBox = id.text
				SHtml.ajaxText(edited.name,
					(x:String) => {
						edited.name = x
						//SetHtml("info",Text(""))
						Run("""(function(){;})()""")
					}
				)
			}},
			"submit" -%> submit
		)
	}
	def allAuthors (content:NodeSeq) = {
		val all = Service.author.findAll
		val anchors = all.flatMap((x:Author) => 
			bind("author",content, "name" -%> SHtml.link("/user/new",
																		() => editedAuthor(x), new Text(x.name)))
		)
		anchors
	}
}
class NewPerson{
	object editedPerson extends RequestVar[Person](new Person)
	def person(xhtml:NodeSeq) = {
		val edited = editedPerson.is
		val isNewPerson = edited.id == null
		bind("person",xhtml,
			  "name" -%> SHtml.text(edited.name, edited.name=_),
			  "email" -%> SHtml.text(edited.email, edited.email=_),
			  "telephone" -%> SHtml.text(edited.telephone, edited.telephone=_),
			  "submit" -%> SHtml.submit(if (isNewPerson)
				  (S ? "newPer") else S ? "chPer"
												 ,() => {
					Service.person.save(edited)
					val la = new LibrarianAction
					la.description = Service.person.findByName(edited.name).get.id
					la.librarian = loggedLibrarian.is.get._1
					la.actionType = if (isNewPerson) LibrarianActionType.personCreated else LibrarianActionType.personChanged
					Service.librarianAction.save(la)
				})
		)
	}

	def allPerson (content:NodeSeq) = {
		val all = Service.person.findAll
		val anchors = all.flatMap((x:Person) =>
			bind("person",content,
				  "name" -%> SHtml.link("/user/new", () => editedPerson(x), new Text(x.name)))
		)
		anchors
	}
}