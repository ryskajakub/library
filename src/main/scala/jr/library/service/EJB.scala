package jr.library.service

import java.text.DateFormat
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.MimeMessage
import javax.persistence._
import javax.ejb._
import java.util.Date
import java.io.Serializable
import java.lang.{Long => JLong}
import java.util.Locale
import java.util.logging.Logger
import javax.annotation._
import org.scala_libs.jpa.ScalaEntityManager
import scala.reflect._
import jr.library.model.BookCopy
import jr.library.model._
import scala.collection.JavaConversions._

class Sem(manager:EntityManager) extends ScalaEntityManager{
	val em = manager
	val factory = null
}

trait GenericBean[A <: SuperEntity]{
	@PersistenceContext
	var manager : EntityManager = _
	var sem : ScalaEntityManager = _
	@PostConstruct
	def injectManager(){
		sem = new Sem(manager)
	}
	def save(entity:A){
		try{
			sem.mergeAndFlush(entity)
		} catch {
			case ex:Exception =>
		}
	}
	def findAll(implicit m:Manifest[A]) = {
		val res = sem.createNamedQuery(m.erasure.getSimpleName + ".findAll")
		.getResultList
		res.foreach((x:SuperEntity) => x.initializeLazyFields)
		res
	}
	def delete(id:JLong)(implicit m:Manifest[A]) {
		val maybeEntity = sem.getReference(m.erasure,id)
		maybeEntity match {
			case Some(x:AnyRef) => sem.remove(x)
			case _ => ;
		}
		sem.flush
	}
}

trait GenericNameBean[A <: WithName] extends GenericBean[A]{
	def findByName(name:String)(implicit m:Manifest[A]):Option[A] = {
		val entity = sem
		.createNamedQuery(m.erasure.getSimpleName + ".findByName")
		.setParams("name" -> name)
		.findOne
		entity
	}
	def isNameAvailable(name:String)(implicit m:Manifest[A]) = {
		findByName(name)(m) match {
			case None => true
			case _ => false
		}
	}
}

@Stateless
@LocalBean
class LibraryBean extends GenericBean[Library] {
	def findByPlace(place:String):Option[Library] = {
		val entity = sem
		.createNamedQuery("Library.findByPlace")
		.setParams("place" -> place)
		.findOne
		entity
	}
}

@Stateless
@LocalBean
class BookBean extends GenericNameBean[Book]{
	override def findAll(implicit m:Manifest[Book]) = {
		val res = sem
		.createNamedQuery("Book.findAll")
		.getResultList
		res.foreach((x:Book) => {
				x.initializeLazyFields
				x.bookCopies.foreach(_.initializeLazyFields)
			})
		res
	}
	def findAllByLikeName(name:String) = {
		val res = sem
		.createNamedQuery("Book.findAllByLikeName")
		.setParams("name" -> ("%" + name + "%"))
		.getResultList
		res.foreach((x:Book) => {
				x.initializeLazyFields
				x.bookCopies.foreach(_.initializeLazyFields)
			})
		res	
	}
}

@Stateless
@LocalBean
class LibrarianActionBean extends GenericBean[LibrarianAction]

@Stateless
@LocalBean
class BookCopyBean extends GenericBean[BookCopy] {
	def findByCode(code:String):Option[BookCopy] = {
		val book = sem
		.createNamedQuery("BookCopy.findByCode")
		.setParams(("code" -> code))
		.findOne
		book.map((x:BookCopy) => x.initializeLazyFields)
		book
	}
	def findAllLentBooks = {
		val all = sem
		.createNamedQuery("BookCopy.findAllLentBooks")
		.findAll
		all.foreach((x:BookCopy) => x.initializeLazyFields)
		all
	}
	def findPaginated(firstRes:Int,numRes:Int) = {
		sem.createNamedQuery("BookCopy.findAllSorted")
		.setFirstResult(firstRes)
		.setMaxResults(numRes)
		.getResultList
	}
	def findPublicAll = {
		import java.text._
		val collator = Collator.getInstance(new Locale("cs"))
		collator.setStrength(Collator.SECONDARY)
		val all = sem
		.createNamedQuery("BookCopy.findPublicAll")
		.findAll
		all.map{(x:BookCopy) =>
			x.book.authors = scala.collection.mutable.Set(sem
				.createQuery[Author]("""
					SELECT author
					FROM Author author,
					IN (author.books) book
					WHERE book.id = :id
					ORDER BY author.name
				""").setParameter("id", x.book.id)
				.findAll:_*)
			x.borrows = sem.createQuery[Borrow]("""
				SELECT borrow
				FROM Borrow borrow
				WHERE borrow.bookCopy.id = :id
			""").setParameter("id", x.id)
			.findAll
		
		}
		all.filter((x:BookCopy) => !x.returned)
		//.sortWith((x,y) => collator.)
	}
	def count:Long = sem.createNamedQuery("BookCopy.count").findOne.get
}

@Stateless
@LocalBean
class AuthorBean extends GenericNameBean[Author]

case class MyTuple(id:JLong,message:EmailType.Value) extends Serializable

object EmailType extends Enumeration{
	val fortnight, initial, warning = Value
}

@Stateless
@LocalBean
class BorrowBean extends GenericBean[Borrow] {
	@Resource
	var ts:TimerService = _
	@Resource(name = "mail/librarySession")
	var mail:Session = _
	override def save(b:Borrow) {
		val bor = sem.mergeAndFlush(b)
		val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT,new Locale("cs"))
		val moreDaysLater = new Date(b.from.getTime + (Borrow.day * 60))
		System.out.println(moreDaysLater.getTime.toString);
		System.out.println(b.from.getTime.toString);
		System.out.println(dateFormat.format(moreDaysLater))
		System.out.println(dateFormat.format(b.from))
		sendMail("""Ahoj, vypůjčil sis tuto knihu """ + b.bookCopy.book.name + """ a do """ +
dateFormat.format(moreDaysLater)	+ """  ji musíš do
knihovny vrátit. V opačném případě souhlasíš s tím, že ti bude
za každý den z prodlení účtována částka 3,- na den, kterou pak
pošleš na komunitní účet 1021034503/5500 spec. symbol 900
					""",b.person.email);
		val borrow = sem.find(classOf[Borrow],bor.id).get
		ts.createTimer(new Date(System.currentTimeMillis + (Borrow.day * 60)),
							MyTuple(borrow.id,EmailType.warning))
		ts.createTimer(new Date(System.currentTimeMillis + (Borrow.day * 53)),
							MyTuple(borrow.id,EmailType.fortnight))
		Logger.getAnonymousLogger.severe("timers created")
	}
	def sendMail(s:String,email:String){
		val message = new MimeMessage(mail)
		message.setRecipients(Message.RecipientType.TO,email)
		message.setSubject("Dzogčhen - Knihovna","UTF-8")
		message.setSentDate(new Date)
		message.setHeader("Content-type", "text/html; charset=UTF-8")
		message.setText(s, "UTF-8")
		Transport.send(message)
		Logger.getAnonymousLogger.severe("sending email finished")
	}
	@Timeout
	def catchTimer(timer:Timer){
		Logger.getAnonymousLogger.severe("in timeout")
		val myTuple = timer.getInfo.asInstanceOf[MyTuple]
		val borrowType = (sem.find(classOf[Borrow],myTuple.id) -> myTuple.message)
		borrowType match {
			case (None,_) =>
			case (Some(x),_) if x.realTo != null =>
			case (Some(x),EmailType.fortnight) =>
				val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT,new Locale("cs"))
				val moreDaysLater = new Date(x.from.getTime + (Borrow.day * 60))
				sendMail("""
Ahoj, půjčená kniha """ + x.person.name + """ by měla být za tři dny vrácena
do knihovny, jinak ti bude účtováno domluvené půjčovné 3,- na den
za každý den po datu """ + dateFormat.format(moreDaysLater) + """.""",x.person.email)
			case (Some(x),EmailType.warning) => sendMail("""
Ahoj, půjčená kniha """ + x.bookCopy.book.name + """ už měla být vrácena v knihovně
a od dnešního dne započítáváme za každý další den 3,- půjčovného.
Prosíme, vrať knihu co nejdřívě do knihovny a zaplať půjčovné.
Pokud se tak stane do 7 dní od tohoto mailu, je to OK.
V opačném případě předáme k dořešení na yellow. Děkujeme za pochopení.
				""",x.person.email)
			case _ =>
		}
	}
	def findAllPending =
		sem.createNamedQuery("Borrow.findAllPending")
	.getResultList
}

@Stateless
@LocalBean
class PersonBean extends GenericNameBean[Person] 

@Stateless
@LocalBean
class LibrarianBean extends GenericBean[Librarian] {
	def login(username:String,password:String,library:Library):Option[Tuple2[Librarian,Library]] = {
		val librarian:Option[Librarian] =
			sem
		.createNamedQuery("Librarian.login")
		.setParams( ("username" -> username))
		.findOne
		librarian match {
			case Some(x:Librarian) if (x.permittedLibraries.contains(library)
												&& password == x.password) => Some(Tuple2(x,library))
			case _ => None
		}
	}
}
