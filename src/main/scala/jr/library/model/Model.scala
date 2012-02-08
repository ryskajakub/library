package jr.library.model

import java.io.Serializable
import java.util.logging.Logger
import java.util.{Set => JSet,HashSet => JHashSet,List => JList, ArrayList => JArrayList, Date}
import java.lang.{Long => JLong}
import javax.persistence._
import javax.validation.constraints._
import scala.reflect.BeanProperty
import jr.library.util._
import scala.collection.JavaConversions._

@Entity
class Something(
	@BeanProperty var book:String,
	@BeanProperty var int:Int
) extends WithName with Serializable {
	override def initializeLazyFields{ }
}

@Entity
@NamedQueries(
	Array(
		new NamedQuery(name = "Author.findAll", query = "select a from Author a order by a.name "),
		new NamedQuery(name = "Author.findByName", query = "select a from Author a where a.name = :name")
	)
)
class Author extends WithName with Serializable{
	@ManyToMany(mappedBy = "authors",cascade=Array(CascadeType.MERGE))
	@BeanProperty
	var books:JSet[Book] = new JHashSet[Book]();
	override def initializeLazyFields{
		books.size
	}
}
@Entity
@NamedQueries( 
	Array(
		new NamedQuery(name = "Book.findAll",
		query = "select b from Book b order by b.name"),
		new NamedQuery(name = "Book.findAllByLikeName",
		query = "SELECT b FROM Book b WHERE b.name LIKE :name ORDER BY b.name"),
		new NamedQuery(name = "Book.findByName", query = "select b from Book b where b.name = :name")
	)
)
class Book extends WithName with Serializable{
	@Size(min=1)
	@ManyToMany(cascade=Array(CascadeType.PERSIST,CascadeType.MERGE))
	@BeanProperty
	var authors: JSet[Author] = new JHashSet[Author]
	@OneToMany(mappedBy="book")
	@BeanProperty
	var bookCopies: JSet[BookCopy] = new JHashSet[BookCopy]
	@BeanProperty
	@ManyToMany
	var tags:JSet[Tag] = new JHashSet[Tag]
	override def initializeLazyFields{
		bookCopies.size
		authors.size
		tags.size
	}
}

@Entity
class Tag extends WithName with Serializable{
	@BeanProperty
	@ManyToMany(mappedBy="tags")
	var books: JSet[Book] = new JHashSet[Book]
	override def initializeLazyFields{
		books.size
	}
}

@Entity
@NamedQueries(
	Array(
		new NamedQuery(name = "BookCopy.findPublicAll", query = """
			SELECT x
			FROM BookCopy x
			ORDER BY x.book.name
		"""),
		new NamedQuery(name = "BookCopy.findAll", query = "select x from BookCopy x"),
		new NamedQuery(name = "BookCopy.findAllSorted", query = "select x from BookCopy x order by x.book.name"),
		new NamedQuery(name = "BookCopy.findByCode",
		query = "select x from BookCopy x where x.code = :code"),
		new NamedQuery(name = "BookCopy.count", query = "select count(x) from BookCopy x")
	)
)
class BookCopy extends SuperEntity with Serializable{
	def this(_code:String){
		this()
		code = _code
	}
	@ManyToOne(cascade=Array(CascadeType.MERGE,CascadeType.PERSIST))
	@BeanProperty
	@NotNull
	var book:Book = _
	@OneToMany(mappedBy="bookCopy", cascade=Array(CascadeType.REMOVE,CascadeType.PERSIST,CascadeType.MERGE))
	@BeanProperty
	var borrows: JList[Borrow] = new JArrayList[Borrow]
	@NotNull
	@Size(min=1)
	@BeanProperty
	@Column(length=70,nullable=false,unique=true)
	var code:String = ""
	@BeanProperty
	@ManyToOne
	var library:Library = _
	@BeanProperty
	var returned_ :Short = 0
	def returned_= (b:Boolean){returned_ = (if (b) 1 else 0)}
	def returned = if (returned_ == 0) false else true
	override def initializeLazyFields{
		borrows.size
	}

}

@Entity
@NamedQueries(
	Array(
		new NamedQuery(name = "Borrow.findAllPending", query = "SELECT x FROM Borrow x " +
		"WHERE x.realTo IS NULL"),
		new NamedQuery(name = "Borrow.findAll", query = "SELECT x FROM Borrow x")
))
class Borrow extends SuperEntity with Serializable{
	@ManyToOne(cascade = Array(CascadeType.PERSIST, CascadeType.MERGE))
	@BeanProperty
	@NotNull
	var person:Person = _
	@ManyToOne(cascade = Array(CascadeType.PERSIST, CascadeType.MERGE))
	@BeanProperty
	@NotNull
	var bookCopy:BookCopy = _
	@Temporal(value = javax.persistence.TemporalType.DATE)
	@BeanProperty
	var realTo:Date = _
	@Temporal(javax.persistence.TemporalType.DATE)
	@BeanProperty
	@NotNull
	var from_ :Date = _
	def from = from_
	def from_= (f:Date){from_ = f}
	def shouldTo = new Date(from.getTime + (Borrow.day.toLong * Borrow.daysBorrow))
	override def initializeLazyFields{}
}
object Borrow{
	def day = (1000L * 60 * 60 * 24)
	def finePerDay = 3
  def daysBorrow = 60L;
  def daysFortnight = 53L;
}

@NamedQueries(
	Array(
		new NamedQuery(name = "LibrarianAction.findAll", query = "select l from LibrarianAction l")
	)
)
@Entity
class LibrarianAction extends SuperEntity with Serializable{
	@BeanProperty
	@ManyToOne
	var librarian:Librarian =_
	@BeanProperty
	var description:String = ""
	def description_= (x:java.lang.Long) { description = x.toString}
	@BeanProperty
	@Temporal(value = javax.persistence.TemporalType.DATE)
	var date:Date = new Date
	@BeanProperty
	var actionType:String = ""
	def actionType_= (lat:LibrarianActionType.Value) { actionType = lat.toString }
	override def initializeLazyFields{}
}
object LibrarianActionType extends Enumeration{
	val bookAttach = Value("Book Attach")
	val bookDetach = Value("Book Detach")
	val bookCreated = Value("Book Created")
	val bookChanged = Value("Book Changed")
	val bookCopyCreated = Value("Book Copy Created")
	val authorCreated = Value("Author Created")
	val authorChanged = Value("Author Changed")
	val personCreated = Value("Person Created")
	val personChanged = Value("Person Changed")
	val bookBorrowed = Value("Book Borrowed")
	val bookReturned = Value("Book Returned")
	val bookToPublisher = Value("Book Returned to Publisher")
}
@NamedQueries(
	Array(
		new NamedQuery(name = "Librarian.findAll", query = "select l from Librarian l"),
		new NamedQuery(name = "Librarian.login", query = "select l from Librarian l where l.name = :username"),
		new NamedQuery(name = "Librarian.findByName", query = "select l from Librarian l where l.name = :name")
	)
)
@Entity
class Librarian extends WithName with Serializable {
	@ManyToMany(cascade = Array(CascadeType.MERGE,CascadeType.ALL))
	@BeanProperty
	var permittedLibraries : JSet[Library] = new JHashSet[Library]
	@Size(min=1)
	@BeanProperty
	@Column(length=50,nullable=false)
	var password: String="";
	override def initializeLazyFields{
		permittedLibraries.size
	}
	@BeanProperty
	var canDelete_ = 0.toShort
	def canDelete = if (canDelete_ == 0) false else true
	def canDelete_=(c:Boolean) { canDelete_ = (if (c) 1 else 0).toShort }
}

@Entity
@NamedQueries(
	Array(
		new NamedQuery(name = "Library.findAll", query = "select x from Library x"),
		new NamedQuery(name = "Library.findByPlace", query = "select x from Library x where x.place = :place")
	)
)
class Library extends SuperEntity with Serializable {
	@ManyToMany(mappedBy="permittedLibraries", cascade = Array(CascadeType.MERGE))
	@BeanProperty
	var librarians:JSet[Librarian] = new JHashSet[Librarian]
	@Column(length=70,unique=true,nullable=false)
	@Size(min=1)
	@BeanProperty
	var place:String= "";
	@OneToMany(mappedBy="library")
	@BeanProperty
	var bookCopies:JSet[BookCopy] = new JHashSet[BookCopy]
	override def initializeLazyFields{
		librarians.size
		bookCopies.size
	}
}

@Entity
@NamedQueries(
	Array(
		new NamedQuery(name = "Person.findAll", query = "select p from Person p order by p.name"),
		new NamedQuery(name = "Person.findByName", query = "select p from Person p where p.name = :name")
	)
)
class Person extends WithName with Serializable{
	@OneToMany(mappedBy="person",cascade=Array(CascadeType.ALL))
	@BeanProperty
	var borrows:JList[Borrow] = new JArrayList[Borrow]
	@BeanProperty
	@Size(min=1)
	@Column(length=70,unique=true,nullable=false)
	var email:String = "";
	@BeanProperty
	@Size(min=1)
	@Column(length=20)
	var telephone:String= "";
	override def initializeLazyFields{
		borrows.size
	}
	def hasBorrowedBooks = {
		!(borrows.forall((x:Borrow) => x.realTo == null))
	}
}

@MappedSuperclass
abstract class WithName extends SuperEntity{
	@Column(length=50,unique=true,nullable=false)
	@Size(min=1)
	@BeanProperty
	var name:String = "";
}
