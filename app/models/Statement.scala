package models

object Rating extends Enumeration {
	type Rating = Value
	val PromiseKept, Compromise, PromiseBroken, Stalled, InTheWorks, Unrated = Value
};

object Role extends Enumeration {
	type Role = Value
	val Admin, Editor, Unprivileged = Value
};

import Role._
import Rating._

import java.util.Date

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current
import play.api.templates._

case class Category(id: Long, name: String, order: Long)
case class Author(id: Long, name: String, order: Long, rated: Boolean)
case class Tag(id: Long, name: String)
case class User(id: Long, email: String, name: String, password: String, salt: String, role: Role)
case class Entry(id: Long, stmt_id: Long, content: String, date: Date, user: User)
case class Statement(id: Long, title: String, author: Author, category: Category, quote: Option[String], quote_src: Option[String], entries: List[Entry], tags: List[Tag], rating: Option[Rating], merged_id: Option[Long])

object Category {
	val category = {
		get[Long]("id") ~
		get[String]("name") ~
		get[Long]("ordering") map {
			case id ~ name ~ ordering => Category(id, name, ordering)
		}
	}

	def create(name: String, order: Long): Category = {
		DB.withConnection { implicit c => create(c, name, order) }
	}

	def create(implicit connection: java.sql.Connection, name: String, order: Long): Category = {
		val id: Long = SQL("select nextval('cat_id_seq')").as(scalar[Long].single)

		SQL("insert into category values ({id}, {name}, {order})").on(
			'id -> id,
			'name -> name,
			'order -> order).executeUpdate()

		Category(id, name, order)
	}

	def loadAll(implicit connection: java.sql.Connection): List[Category] = {
		SQL("select * from category order by ordering").as(category*)
	}
}

object Author {
	val author = {
		get[Long]("id") ~
		get[String]("name") ~
		get[Long]("ordering") ~
		get[Boolean]("rated") map {
			case id ~ name ~ ordering ~ rated => Author(id, name, ordering, rated)
		}
	}

	def create(name: String, order: Long, rated: Boolean): Author = {
		DB.withConnection { implicit c => create(c, name, order, rated) }
	}

	def create(implicit connection: java.sql.Connection, name: String, order: Long, rated: Boolean): Author = {
		val id: Long = SQL("select nextval('author_id_seq')").as(scalar[Long].single)

		SQL("insert into author values ({id}, {name}, {order}, {rated})").on(
			'id -> id,
			'name -> name,
			'order -> order,
			'rated -> rated).executeUpdate()

		Author(id, name, order, rated)
	}

	def load(name : String): Option[Author] = {
		DB.withConnection { implicit c =>
			SQL("select * from author where name = {name}").on('name -> name).as(author.singleOpt)
		}
	}

	def loadAll(): List[Author] = {
		DB.withConnection { implicit c =>
			SQL("select * from author order by ordering").as(author*)
		}
	}
}

object Tag {
	val tag = {
		get[Long]("id") ~
		get[String]("name") map {
			case id ~ name => Tag(id, name)
		}
	}

	def create(name: String): Tag = {
		DB.withConnection { implicit c => create(name) }
	}

	def create(implicit connection: java.sql.Connection, name: String): Tag = {
		val id: Long = SQL("select nextval('tag_id_seq')").as(scalar[Long].single)

		SQL("insert into tag values ({id}, {name})").on('id -> id, 'name -> name).executeUpdate()
		Tag(id, name)
	}

	def loadByStatement(stmt_id: Long): List[Tag] = {
		DB.withConnection { implicit c =>
			SQL("""select statement_tags.tag_id, tag.name from statement_tags 
				join tag on statement_tags.tag_id=tag.id 
				where statement_tags.stmt_id = {stmt_id} 
				order by tag.name""").on('stmt_id -> stmt_id).as(tag*)
		}
	}

	def add(implicit connection: java.sql.Connection, stmt: Statement, tag: Tag) {
			SQL("insert into statement_tags values ({tag_id}, {stmt_id})").on(
				'tag_id -> tag.id,
				'stmt_id -> stmt.id
			).executeUpdate
	}

	def loadAll(implicit connection: java.sql.Connection): List[Tag] = {
			SQL("select id, name from tag").as(tag*)
	}
}

object User {
	val user = {
		get[Long]("id") ~
			get[String]("email") ~
			get[String]("name") ~
			get[String]("password") ~
			get[String]("salt") ~
			get[Int]("role") map {
				case id ~ email ~ name ~ password ~ salt ~ role => User(id, email, name, password, salt, if (0 <= role && role < Role.maxId) Role(role) else Unprivileged)
			}
	}

	def load(id: Long): Option[User] = {
		DB.withConnection { implicit c =>
			SQL("select * from users where id = {id}").on('id -> id).as(user.singleOpt)
		}
	}

	def load(email: String): Option[User] = {
		DB.withConnection { implicit c =>
			SQL("select * from users where email = {email}").on('email -> email).as(user.singleOpt)
		}
	}

	def findAll(): List[User] = {
		DB.withConnection { implicit c =>
			SQL("select * from users").as(user*)
		}
	}

	private def passwordhash(salt: String, password: String): String = {
		val md = java.security.MessageDigest.getInstance("SHA-1")
		new sun.misc.BASE64Encoder().encode(md.digest((salt + password).getBytes))
	}

	def create(email: String, name: String, password: String, role: Role): User = {
		DB.withConnection { implicit c =>
			val id: Long = SQL("select nextval('user_id_seq')").as(scalar[Long].single)
			val salt = (for (i <- 1 to 20) yield util.Random.nextPrintableChar).mkString
			val hash = passwordhash(salt, password)
			SQL("insert into users values ({id}, {email}, {name}, {password}, {salt}, {role})").on(
				'id -> id,
				'email -> email,
				'name -> name,
				'password -> hash,
				'salt -> salt,
				'role -> role.id).executeUpdate()

			User(id, email, name, hash, salt, role)
		}
	}

	def authenticate(email: String, password: String): Option[User] = {
		DB.withConnection { implicit connection =>
			User.load(email) filter (u => u.password == passwordhash(u.salt, password))
		}
	}
}

object Entry {
	val entry = {
		get[Long]("id") ~
			get[Long]("stmt_id") ~
			get[String]("content") ~
			get[Date]("date") ~
			get[Long]("user_id") map {
				case id ~ stmt_id ~ content ~ date ~ user_id => Entry(id, stmt_id, content, date, User.load(user_id).get)
			}
	}

	def loadByStatement(stmt_id: Long): List[Entry] = {
		DB.withConnection { implicit c =>
			SQL("select * from entry where stmt_id = {stmt_id} ORDER by date DESC").on('stmt_id -> stmt_id).as(entry*)
		}
	}

	def create(stmt_id: Long, content: String, date: Date, user_id: Long) {
		DB.withTransaction { implicit c =>
			val id = SQL("select nextval('entry_id_seq')").as(scalar[Long].single)

			SQL(
				"""
           insert into entry values (
             {id}, {stmt_id}, {content}, {date}, {user_id}
           )
         """).on(
					'id -> id,
					'stmt_id -> stmt_id,
					'content -> content,
					'date -> date,
					'user_id -> user_id).executeUpdate()
		}
	}
}

object Statement {
	val stmt = {
			get[Long]("statement.id") ~
			get[String]("statement.title") ~
			get[Option[String]]("statement.quote") ~
			get[Option[String]]("statement.quote_src") ~
			get[Option[Int]]("statement.rating") ~
			get[Option[Long]]("statement.merged_id") ~
			get[Long]("author.id") ~
			get[String]("author.name") ~
			get[Long]("author.ordering") ~
			get[Boolean]("author.rated") ~
			get[Long]("category.id") ~
			get[String]("category.name") ~
			get[Long]("category.ordering")  map {
				case id ~ title ~ quote ~ quote_src ~ rating ~ merged_id ~ author_id ~ author_name ~ author_order ~ author_rated ~ 
				 category_id ~ category_name ~ category_order => 
				Statement(id, title, 
					Author(author_id, author_name, author_order, author_rated),
					Category(category_id, category_name, category_order),
					quote, quote_src,
					List[Entry](), 
					List[Tag](),
					rating map { r => if (0 <= r && r < Rating.maxId) Rating(r) else Unrated },
					merged_id
				)
			} // Rating(rating) would throw java.util.NoSuchElementException
	}

	var query = """select statement.id, title, rating, merged_id, quote, quote_src, 
		category.id, category.name, category.ordering,
		author.id, author.name, author.ordering, author.rated
		from statement 
		join category on category.id=cat_id
		join author on author.id=author_id"""

	def allWithoutEntries(): Map[Author, List[Statement]] = {
		DB.withConnection({ implicit c =>
			SQL(query+" order by author.ordering ASC, category.ordering ASC").as(stmt *)
		}).groupBy( _.author )
	}

	def load(id: Long): Option[Statement] = {
		// TODO: Create stmt with entries in the first place
		val s = DB.withConnection({ implicit c =>
			SQL(query+" where statement.id = {id}").on(
				'id -> id).as(stmt.singleOpt)
		})
		s map { stmt => 
			Statement(stmt.id, stmt.title, stmt.author, stmt.category, stmt.quote, stmt.quote_src, 
				Entry.loadByStatement(id), Tag.loadByStatement(id), stmt.rating, stmt.merged_id) 
		}
	}

	def create(title: String, author: Author, cat: Category, quote: Option[String], quote_src: Option[String], rating: Option[Rating], merged_id: Option[Long]): Statement = {
		DB.withTransaction { implicit c => Statement.create(c, title, author, cat, quote, quote_src, rating, merged_id) }
	}

	def create(implicit connection: java.sql.Connection, title: String, author: Author, cat: Category, quote: Option[String], quote_src: Option[String], rating: Option[Rating], merged_id: Option[Long]): Statement = {
		
		require(author.rated == rating.isDefined)
		require(!merged_id.isDefined || !author.rated)

		// Get the project id
		val id: Long = SQL("select nextval('stmt_id_seq')").as(scalar[Long].single)
		// Insert the project
		SQL("insert into statement values ({id}, {title}, {author_id}, {cat_id}, {quote}, {quote_src}, {rating}, {merged_id})").on(
				'id -> id,
				'title -> title,
				'author_id -> author.id,
				'cat_id -> cat.id,
				'quote -> quote,
				'quote_src -> quote_src, 
				'rating -> { rating map { _.id } },
				'merged_id -> merged_id).executeUpdate()

		Statement(id, title, author, cat, quote, quote_src, List[Entry](), List[Tag](), rating, merged_id)
	}
}
