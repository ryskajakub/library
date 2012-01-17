package cz.dzogchen.library.service

import javax.naming.InitialContext
import cz.dzogchen.library.model._
import net.liftweb.http.GetRequest
import net.liftweb.http.Req
import net.liftweb.http.rest.RestHelper
import net.liftweb.util.Helpers
import net.liftweb.util.Helpers._
import scala.xml.NodeSeq
import scala.xml.Text
import scala.collection.JavaConversions._

class ServiceSource {
	//val appName = "library-1.0-SNAPSHOT"
	//val appName = "cz.dzogchen_library_war_1.0-SNAPSHOT"
	val appName = "lib" // ok
	val librarian = (new InitialContext)
	.lookup("java:global/" + appName + "/LibrarianBean")
	.asInstanceOf[LibrarianBean]
	val library = (new InitialContext)
	.lookup("java:global/" + appName + "/LibraryBean")
	.asInstanceOf[LibraryBean]
	val librarianAction = (new InitialContext)
	.lookup("java:global/" + appName + "/LibrarianActionBean")
	.asInstanceOf[LibrarianActionBean]
	val book = (new InitialContext)
	.lookup("java:global/" + appName + "/BookBean")
	.asInstanceOf[BookBean]
	val bookCopy = (new InitialContext)
	.lookup("java:global/" + appName + "/BookCopyBean")
	.asInstanceOf[BookCopyBean]
	val borrow = (new InitialContext)
	.lookup("java:global/" + appName + "/BorrowBean")
	.asInstanceOf[BorrowBean]
	val author = (new InitialContext)
	.lookup("java:global/" + appName + "/AuthorBean")
	.asInstanceOf[AuthorBean]
	val person = (new InitialContext)
	.lookup("java:global/" + appName + "/PersonBean")
	.asInstanceOf[PersonBean]
}

object Service extends ServiceSource

object MyRest extends RestHelper {
  serve {
    case Req("api" :: "public" :: "browse" :: Nil, "xml", GetRequest) => {
		 val allBooks = Service.bookCopy.findPublicAll
		 <books>
			 {
				 allBooks.flatMap((x:BookCopy) =>
					 Helpers.bind("entry",
					 <book>
						 <name><entry:name/></name>
						 <authors>
						 	 <entry:authors>
								 <author><entry2:author/></author>
							 </entry:authors>
						 </authors>
						 <library><entry:library/></library>
						 <available><entry:available/></available>
					 </book>,
					 "name" -%> Text(x.book.name),
					 "library" -%> Text(x.library.place),
					 "available" -%> Text((x.borrows.forall(_.realTo != null)).toString),
					 "authors" -%> {(xhtml:NodeSeq) =>
						 x.book.authors.toList.flatMap{(author:Author) =>
							 Helpers.bind("entry2",
								 xhtml,
								 "author" -%> Text(author.name)
							 )
						 }
					 }
				 ))
			 }
		 </books>
	 }
  }
}