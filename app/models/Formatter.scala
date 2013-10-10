package models

import models.Rating._
import java.util.Date
import eu.henkelmann.actuarius.{Transformer, Decorator}

import play.api.templates.Html
import play.api.Play
import play.api.Play.current

object Formatter {

	def glyph(rating: Rating) : String = {
		rating match {
			case PromiseKept => "glyphicon-thumbs-up"
			case Compromise => "glyphicon-adjust"
			case PromiseBroken => "glyphicon-thumbs-down"
			case Stalled => "glyphicon-time"
			case InTheWorks => "glyphicon-cog"
			case Unrated => "glyphicon-question-sign"
		}
	}

	def color(rating: Rating) : String = {
		rating match {
			case PromiseKept => "#5cb85c"
			case Compromise => "#f0ad4e"
			case PromiseBroken => "#d9534f"
			case Stalled => "#d9984f"
			case InTheWorks => "#5bc0de"
			case Unrated => "#aaaaaa"
		}
	}

	def name(rating: Rating) : String = {
		rating match {
			case PromiseKept => "Gehalten"
			case Compromise => "Kompromiss"
			case PromiseBroken => "Gebrochen"
			case Stalled => "Blockiert"
			case InTheWorks => "In Arbeit"
			case Unrated => "Unbewertet"
		}
	}

	def url : String = Play.configuration.getString("url").get
	def twitter : String = Play.configuration.getString("twitter").get
	def mail : String = Play.configuration.getString("mail").get
	def disqus_shortname : String = Play.configuration.getString("disqus.shortname").get

	def encode(url: String) = java.net.URLEncoder.encode(url, "UTF-8")

	def format(date: Date)(implicit lang: play.api.i18n.Lang) : String = {
		new java.text.SimpleDateFormat("dd.MM.yy", lang.toLocale).format(date)
	}

	private object FilterHeadlineFromMarkdown extends Decorator {		
	    override def allowVerbatimXml():Boolean = false		    
	    override def decorateImg(alt:String, src:String, title:Option[String]):String = ""		    
	    override def decorateRuler():String = ""		    
	    override def decorateHeaderOpen(headerNo:Int):String = "<div style='display: none'>"
	    override def decorateHeaderClose(headerNo:Int):String = "</div>"
	    override def decorateCodeBlockOpen():String = "<div 'display: none'>"
	    override def decorateCodeBlockClose():String = "<div 'display: none'>"
	}
	private object markdownToHTMLWithoutHeadlines extends Transformer {
		 override def deco() : Decorator = FilterHeadlineFromMarkdown
	}

	private object FilterXMLFromMarkdown extends Decorator {		
	    override def allowVerbatimXml():Boolean = false		    
	}
	private object markdownToHTML extends Transformer {
		 override def deco() : Decorator = FilterXMLFromMarkdown
	}

	def transformBodyToHTML(markdown: String) : Html = {
		Html(markdownToHTMLWithoutHeadlines(markdown))
	}

	def transformFirstLineToHTML(markdown: String) : Html = {
		def FindFirstLine( it: Iterator[String] ) : String = {
			if(it.hasNext) {
				val line = it.next
				line.find(_.isLetterOrDigit) match {
					case Some(c) => line
					case None => FindFirstLine(it)
				}
			} else {
				""
			}
		}
		Html(markdownToHTML(FindFirstLine(markdown.lines)))
	}

	def transformToHTML(markdown: String) : Html = {
		Html(markdownToHTML(markdown))
	}
}