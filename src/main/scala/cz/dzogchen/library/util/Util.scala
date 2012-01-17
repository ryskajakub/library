/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.dzogchen.library

import java.text.SimpleDateFormat
import java.util.Date

package object util {
	def formatCz(x:Date) = {
		val df = new SimpleDateFormat("dd.MM.yyyy")
		df.format(x)
	}
/*
	def encode (x:String):String = {
		x.map((c:Char) => c match{
				case 'á' => "%a"
				case 'Á' => "%A"
				case 'é' => "%e"
				case 'É' => "%E"
				case 'ě' => "%x"
				case 'Ě' => "%X"
				case 'í' => "%i"
				case 'Í' => "%I"
				case 'ý' => "%y"
				case 'Ý' => "%Ý"
				case 'ó' => "%o"
				case 'Ó' => "%O"
				case 'ú' => "%u"
				case 'Ú' => "%U"
				case 'ů' => "%f"
				case 'Ů' => "%F"
				case 'č' => "%c"
				case 'Č' => "%C"
				case 'ď' => "%d"
				case 'Ď' => "%D"
				case 'ň' => "%n"
				case 'Ň' => "%N"
				case 'š' => "%s"
				case 'Š' => "%S"
				case 'ť' => "%t"
				case 'Ť' => "%T"
				case 'ř' => "%r"
				case 'Ř' => "%R"
				case 'ž' => "%z"
				case 'Ž' => "%Z"
				case '%' => "%%"
				case c => c.toString
			}
		).mkString
	}
	def decode (x:String):String = {
		var i:Int = 0
		var translate = false
		var strBuff = new StringBuilder(x.length)
		while(i < x.length){
			(x.charAt(i),translate) match {
				case ('%',true) => strBuff += '%'; translate = false
				case ('a',true) => strBuff += 'á'; translate = false
				case ('A',true) => strBuff += 'Á'; translate = false
				case ('e',true) => strBuff += 'é'; translate = false
				case ('E',true) => strBuff += 'É'; translate = false
				case ('o',true) => strBuff += 'ó'; translate = false
				case ('O',true) => strBuff += 'Ó'; translate = false
				case ('u',true) => strBuff += 'ú'; translate = false
				case ('U',true) => strBuff += 'Ú'; translate = false
				case ('f',true) => strBuff += 'ů'; translate = false
				case ('F',true) => strBuff += 'Ů'; translate = false
				case ('x',true) => strBuff += 'ě'; translate = false
				case ('X',true) => strBuff += 'Ě'; translate = false
				case ('y',true) => strBuff += 'ý'; translate = false
				case ('Y',true) => strBuff += 'Ý'; translate = false
				case ('c',true) => strBuff += 'č'; translate = false
				case ('C',true) => strBuff += 'Č'; translate = false
				case ('d',true) => strBuff += 'ď'; translate = false
				case ('D',true) => strBuff += 'Ď'; translate = false
				case ('n',true) => strBuff += 'ň'; translate = false
				case ('N',true) => strBuff += 'Ň'; translate = false
				case ('s',true) => strBuff += 'š'; translate = false
				case ('S',true) => strBuff += 'Š'; translate = false
				case ('t',true) => strBuff += 'ť'; translate = false
				case ('T',true) => strBuff += 'Ť'; translate = false
				case ('r',true) => strBuff += 'ř'; translate = false
				case ('R',true) => strBuff += 'Ř'; translate = false
				case ('z',true) => strBuff += 'ž'; translate = false
				case ('Z',true) => strBuff += 'Ž'; translate = false
				case ('%',false) => translate = true
				case (c,_) => strBuff += c
			}
			i += 1
		}
		strBuff.toString
	}
	*/
}