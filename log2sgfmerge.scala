import java.io.File
import java.io.FileReader
import java.io.PrintStream
import java.io.BufferedReader
import scala.io.Source
import scala.collection.mutable.ArrayBuffer

val logFile = args(0)
val sgfFile = args(1)
val myColor = args(2)
val oppColor = if (myColor == "B") "W" else "B"

val sgfLines = Source.fromFile(sgfFile).getLines.mkString.split("[;()]").filter(_.size > 0).toList

if (false) {
  for (l <- sgfLines)
	println(l)

  sys.exit(0)
}

case class MoveLog(move: String, po: Int, vn: Int, winRateSim: Double, policy: Double,
				   winRateValue: Option[Double], winRate: Option[Double],
				   bestSeq: String) {
}

case class LogElement(self: ArrayBuffer[MoveLog], ops: ArrayBuffer[MoveLog], comment: String) {
}

def toMoveLog(line: String): MoveLog = {
  val tokens = line.split("\\|", -1).map(_.trim).tail
  if (tokens.size != 8)
	println("ERRROR " + line)
  val winRateValue = if (tokens(5) isEmpty) None else Some(tokens(5).toDouble)
  val winRate = if (tokens(6) isEmpty) None else Some(tokens(6).toDouble)

  MoveLog(tokens(0), tokens(1).toInt, tokens(2).toInt,
		  tokens(3).toDouble, tokens(4).toDouble,
		  winRateValue, winRate,
		  tokens(7))
}

def gtpToSgf(p: String) = {
  if (p == "PASS") {
	"tt"
  } else {
	val c = p.toUpperCase.charAt(0)
	val x = if (c >= 'I') c - 'A' -1 else c - 'A'
	val y = 19 - p.substring(1).toInt
	('a' + x).toChar + "" + ('a' + y).toChar
  }
}

class LogReader(val r: BufferedReader) {
  var buffer: Option[String] = None
  var comment = new ArrayBuffer[String]

  def readLine() = {
	buffer match {
	  case Some(line) => {
		buffer = None
		line
	  }
	  case None => {
		val line = r.readLine
		comment += line
		line
	  }
	}
  }

  def unreadLine(l: String): Unit = {
	buffer = Some(l)
  }

  def readMoveLine(): Option[MoveLog] = {
	val line = readLine()
	if (line == null) {
	  None
	} else if (line.startsWith("|")) {
	  Some(toMoveLog(line))
	} else {
	  unreadLine(line)
	  None
	}
  }

  def readMoveLines(buf: scala.collection.mutable.Buffer[MoveLog]): Unit = {
	readMoveLine() match {
	  case Some(m) => {
		buf += m
		readMoveLines(buf)
		//println(buf)
	  }
	  case None => ()
	}
  }

  def readLog() = {
	var line = readLine
	val log = new ArrayBuffer[LogElement]
	while (line != null) {
	  if (line startsWith "All Playouts ") {
		comment.clear
		comment += line
	  }
	  if (line startsWith "|Move") {
		val moves = new ArrayBuffer[MoveLog]
		readMoveLines(moves)
		line = readLine
		if (!(line startsWith "Opponent Moves"))
		  throw new IllegalArgumentException(line)
		line = readLine
		if (!(line startsWith "|Move"))
		  throw new IllegalArgumentException(line)
		val oppMoves = new ArrayBuffer[MoveLog]
		readMoveLines(oppMoves)
		val p = LogElement(moves, oppMoves, comment.mkString("\n"))
		log += p
	  }
	  line = readLine
	}
	log
  }

  def printLog(log: ArrayBuffer[LogElement]): Unit = {
	print("(")
	printLog(log, 0, 0, None)
	print(")")
  }

  def printLog(log: ArrayBuffer[LogElement], i: Int, j: Int, opp: Option[ArrayBuffer[MoveLog]]): Unit = {
	//println(log)
	if (i < sgfLines.size) {
	  val sgfElement = sgfLines(i)
	  if (sgfElement.startsWith(myColor + "[") && j < log.size) {
		val LogElement(self, opp, comment) = log(j)
		print("(;")
		printLog1(self)
		println(sgfElement)
			printf("C[%s]", comment)
		//printf("C[%d/%d]", i, j)
		printLog(log, i + 1, j + 1, Some(opp))
		println(")")
		println

		if (i + 1 < sgfLines.size)
		  printLog2(self, myColor)
		println
		//j += 1
	  } else {

		//if (false)
		opp match {
		  case Some(o) =>
			print("(;")
			printLog1(o)
		  case None =>
			print(";")
		}
		println(sgfElement)

		printLog(log, i + 1, j, None)

		opp match {
		  case Some(o) =>
			print(")")
			printLog2(o, oppColor)
		  case None =>
		}
		println
	  }
	}
  }

  def printLog1(self: ArrayBuffer[MoveLog]) {
	var first = true
	for (m <- self if m.move != "PASS") {
	  m.winRate match {
		case Some(r) =>
		  if (first)
			print("LB")
		  first = false
		  printf("[%s:%.1f]", gtpToSgf(m.move), r)
		case None => ()
	  }
	}
	println
  }

  def printBestSeq(moves: List[Array[String]]) {
	var blist = ""
	var wlist = ""
	var first = true
	for ((mm, i) <- moves zipWithIndex) {
	  if (mm(1) != "PASS") {
		if (first)
		  print("LB")
		first = false
		printf("[%s:%d]", gtpToSgf(mm(1)), (i + 1))
		if (mm(0) == "B") {
		  blist += "[" + gtpToSgf(mm(1)) + "]"
		} else {
		  wlist += "[" + gtpToSgf(mm(1)) + "]"
		}
	  }
	}
	if (blist.size > 0)
	  print("CR" + blist)
	if (wlist.size > 0)
	  print("MA" + wlist)

/*
		  var blist = ""
		  var wlist = ""
		  print("(;LB")
		  for ((mm, i) <- moves zipWithIndex) {
			if (mm(1) != "PASS") {
			  printf("[%s:%d]", gtpToSgf(mm(1)), (i + 1))
			  if (mm(0) == "B") {
				blist += "[" + gtpToSgf(mm(1)) + "]"
			  } else {
				wlist += "[" + gtpToSgf(mm(1)) + "]"
			  }
			}
		  }
		  if (blist.size > 0)
			print("AB" + blist)
		  if (wlist.size > 0)
			print("AW" + wlist)

		  println(")")
		  */
	//print("(")
	for (mm <- moves) {
	  printf(";%s[%s]", mm(0), gtpToSgf(mm(1)))
	}
	//print(")")
  }

  def printLog2(self: ArrayBuffer[MoveLog], color: String) {
	for (m <- self) {
	  if (!m.bestSeq.trim.isEmpty) {
		val moves = m.bestSeq.split(" ").grouped(2).toList
		printf("(;%s[%s]", color, gtpToSgf(m.move))
		printBestSeq(moves)
		println(")")
	  }
	}
  }
}

val logReader = new LogReader(new BufferedReader(new FileReader(logFile)))
val log = logReader.readLog()
logReader.printLog(log)

// for (line <- logFile.getLines) {
//   if (line.startsWith("|Move")) {
// 	println(line)
//   }
// }
