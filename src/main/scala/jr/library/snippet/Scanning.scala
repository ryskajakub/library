/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jr.library.snippet

import net.liftweb.common.Empty
import net.liftweb.http._
import net.liftweb.util._
import Helpers._
import scala.xml._
import jr.library.service._
import jr.library.model._
import scala.collection.JavaConversions._
import jr.library.util._
import java.util.Date


class Scanning {

  object scannedBook extends RequestVar[Option[BookCopy]](None)

  def render(xhtml: NodeSeq) = {
    var scannedCode = ""
    bind("field", xhtml,
      "field" -%> (SHtml.text(scannedCode, scannedCode = _)),
      "submit" -%> SHtml.submit(S ? "scan", () => {
        val bookFromDB: Option[BookCopy] = Service.bookCopy.findByCode(scannedCode)
        if (bookFromDB.isDefined && bookFromDB.get.returned)
          S.redirectTo("/user/new")
        S.redirectTo("/user/scannedCode",
          () => {
            scannedBook(Some(bookFromDB.getOrElse(new BookCopy(scannedCode))))
          })
      })
    )
  }

  lazy val borrows = scannedBook.is.get.borrows

  def scannedCode(xhtml: NodeSeq) = {
    val s = scannedBook.is
    (scannedBook.is) match {
      case None => S.redirectTo("/user/scanning")
      case (Some(x)) =>
        Helpers.bind(
          "entry", xhtml,
          "name" -%> Text(s.get.code),
          "history" -%> {
            (x: NodeSeq) => {
              if (borrows.size > 0)
                x
              else
                NodeSeq.Empty
            }
          }
        )
    }
  }

  def listBorrows(xhtml: NodeSeq) = {
    borrows.flatMap((x: Borrow) =>
      Helpers.bind(
        "entry", xhtml,
        "person" -%> Text(x.person.name),
        "from" -%> Text(formatCz(x.from).toString),
        "to" -%> Text(if (x.realTo == null) "" else formatCz(x.realTo))
      )
    )
  }

  object BookState extends Enumeration {
    val inLibrary = Value("LIB")
    val detached = Value("DET")
    val borrowed = Value("BOR")
    val unknownState = Value("UNK")
    val newBook = Value("NEW")
  }

  def bookState = {
    val book = scannedBook.is.get
    (book.borrows.lastOption, book.library, book.id) match {
      case (None, _, null) => BookState.newBook
      case (_, null, x) if x != null => BookState.detached
      case (Some(x), _, _) if x.realTo == null => BookState.borrowed
      case (_, y, x) if x != null && y != null => BookState.inLibrary
      case _ => BookState.unknownState
    }
  }

  def attachBook(xhtml: NodeSeq) = {
    val allLibraries = Service.library.findAll
    val book = scannedBook.is.get
    if (bookState == BookState.detached) {
      Helpers.bind("entry", xhtml,
        "library" -%> Text(loggedLibrarian.is.get._2.getPlace),
        "submit" -%> SHtml.submit(S ? "attach", () => {
          book.library = loggedLibrarian.is.get._2
          Service.bookCopy.save(book)
          val la = new LibrarianAction
          la.librarian = loggedLibrarian.is.get._1
          la.actionType = LibrarianActionType.bookAttach
          la.description = book.id + " Code:" + book.code
          Service.librarianAction.save(la)
        })
      )
    } else NodeSeq.Empty
  }

  def returnBook(xhtml: NodeSeq) = {
    val book = scannedBook.is.get
    if (bookState == BookState.borrowed) {
      Helpers.bind(
        "entry", xhtml,
        "submit" -%> SHtml.submit(S ? "return",
          (() => {
            val la = new LibrarianAction
            la.description = book.id + " Code:" + book.code
            la.actionType = LibrarianActionType.bookReturned
            la.librarian = loggedLibrarian.is.get._1
            Service.librarianAction.save(la)
            book.borrows.last.realTo = new Date(System.currentTimeMillis)
            Service.bookCopy.save(book)
          })
        )
      )
    } else NodeSeq.Empty
  }

  def here(xhtml: NodeSeq) = {
    if (bookState == BookState.inLibrary) {
      val book = scannedBook.is.get
      val allPeople = Service.person.findAll
      var chosenPerson: Person = new Person
      val la = new LibrarianAction
      la.description = book.id + " Code:" + book.code
      la.librarian = loggedLibrarian.is.get._1
      Helpers.bind(
        "entry", xhtml,
        "returnToPublisher" -%> SHtml.submit(S ? "retPbl", () => {
          book.returned = true
          la.actionType = LibrarianActionType.bookToPublisher
          Service.librarianAction.save(la)
          Service.bookCopy.save(book)
        }),
        "selectPerson" -%> SHtml.selectObj(allPeople.map((x: Person) => Tuple2(x, x.getName)), Empty, (x: Person) => chosenPerson = x),
        "borrow" -%> SHtml.submit(S ? "bor", () => {
          val b = new Borrow()
          b.person = chosenPerson
          b.bookCopy = book
          chosenPerson.borrows += b
          book.borrows += b
          b.from = new Date()
          la.actionType = LibrarianActionType.bookBorrowed
          Service.librarianAction.save(la)
          Service.borrow.save(b)
        }),
        "putAway" -%> SHtml.submit(S ? "putAway", () => {
          la.actionType = LibrarianActionType.bookDetach
          book.library = null
          Service.librarianAction.save(la)
          Service.bookCopy.save(book)
        })
      )
    } else NodeSeq.Empty
  }

  def newBookCopy(xhtml: NodeSeq) = {
    if (bookState == BookState.newBook) {
      val allBooks = Service.book.findAll
      val allLibraries = Service.library.findAll
      var newBookCopy = scannedBook.is.get
      newBookCopy.library = loggedLibrarian.is.get._2
      Helpers.bind(
        "bookCopy", xhtml,
        "code" -%> Text(newBookCopy.code),
        "book" -%> SHtml.selectObj(allBooks.map((x: Book) => Tuple2(x, x.name)),
          Empty, (x: Book) => newBookCopy.book = x),
        /*
          "library" -%> SHtml.selectObj(allLibraries.map((x:Library) => Tuple2(x,x.place)),
            Empty,(x:Library) => newBookCopy.library=x),
          */
        "library" -%> Text(loggedLibrarian.is.get._2.place),
        "submit" -%> SHtml.submit(S ? "submit", () => {
          Service.bookCopy.save(newBookCopy)
          val la = new LibrarianAction
          la.actionType = LibrarianActionType.bookCopyCreated
          la.description = Service.bookCopy.findByCode(newBookCopy.code).get.id
          Service.librarianAction.save(la)
          S.redirectTo("/user/new")
        })
      )
    } else NodeSeq.Empty
  }
}
