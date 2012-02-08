package jr.library.snippet

import net.liftweb.http._
import net.liftweb.util._
import Helpers._
import scala.xml._
import jr.library.service._
import jr.library.model._
import scala.collection.JavaConversions._
import jr.library.util._

class PublicBrowse {
  def render(xhtml: NodeSeq) = {
    val allBooks = Service.bookCopy.findPublicAll
    Helpers.bind("repeat", xhtml,
      "body" -%> {
        (xhtml2: NodeSeq) => {
          allBooks.flatMap {
            (x: BookCopy) => {
              val state =
                if (x.borrows.forall(_.realTo != null)) "Ano"
                else "Ne - je půjčená"
              Helpers.bind("book", xhtml2,
                "book" -%> Text(x.book.name),
                "authors" -%> {
                  (xhtml3: NodeSeq) =>
                    x.book.authors.toList.flatMap {
                      (x: Author) =>
                        Helpers.bind("author", xhtml3,
                          "author" -%> Text(x.name)
                        )
                    }
                },
                "library" -%> Text(x.library.place),
                "available" -%> Text(state),
                "code" -%> Text(x.code)
              )
            }
          }
        }
      }
    )
  }
}

class Browse {
  def repeatAllPerson(xhtml: NodeSeq): NodeSeq = {
    Service.person.findAll.flatMap((x: Person) => {
      Helpers.bind(
        "column", xhtml,
        "name" -%> Text(x.name),
        "tel" -%> Text(x.telephone),
        "email" -%> Text(x.email)
      )
    })
  }

  def repeatAllBooks(xhtml: NodeSeq) = {
    val allBooks = Service.book.findAll
    allBooks.flatMap((x: Book) => {
      x.bookCopies.filterNot(_.returned).flatMap((x: BookCopy) => {
        val Tuple2(state, person) = (x.borrows.lastOption) match {
          case Some(y) if (y.realTo == null) => Tuple2(S ? "lent", y.person.name)
          case _ => Tuple2((S ? "here"), "")
        }
        Helpers.bind(
          "entry", xhtml,
          "name" -> Text(x.book.name),
          "code" -%> Text(x.code),
          "state" -%> Text(state),
          "person" -%> Text(person),
          "library" -%> Text(if (x.library == null) S ? "noLib" else x.library.place)
        )
      })
    })
  }

  def repeatLentBooks(xhtml: NodeSeq): NodeSeq = {
    val allLentBooks = Service.borrow.findAllPending
    if (allLentBooks == null) {
      NodeSeq.Empty
    } else {
      NodeSeq.fromSeq(allLentBooks.flatMap((x: Borrow) => {
        val diff = System.currentTimeMillis - x.shouldTo.getTime
        val fine = if (diff > 0) Some(diff) else None
        val days = fine.map((x: Long) => (x / Borrow.day * Borrow.finePerDay).toString)
        Helpers.bind(
          "column", xhtml,
          "who" -%> new Text(x.person.name),
          "what" -%> new Text(x.bookCopy.book.name),
          "code" -%> new Text(x.bookCopy.code),
          "startDate" -%> new Text((formatCz(x.from)).toString),
          "shouldDate" -%> new Text(formatCz(x.shouldTo).toString),
          "fine" -%> Text(days.getOrElse(S ? "noFine"))
        )
      }
      ))
    }
  }
}
