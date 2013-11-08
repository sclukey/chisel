/*
 Copyright (c) 2011, 2012, 2013 The Regents of the University of
 California (Regents). All Rights Reserved.  Redistribution and use in
 source and binary forms, with or without modification, are permitted
 provided that the following conditions are met:

    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the following
      two paragraphs of disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      two paragraphs of disclaimer in the documentation and/or other materials
      provided with the distribution.
    * Neither the name of the Regents nor the names of its contributors
      may be used to endorse or promote products derived from this
      software without specific prior written permission.

 IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 REGENTS HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE. THE SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF
 ANY, PROVIDED HEREUNDER IS PROVIDED "AS IS". REGENTS HAS NO OBLIGATION
 TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR
 MODIFICATIONS.
*/

package Chisel
import scala.collection.mutable.ArrayBuffer

/** This Singleton implements a log4j compatible interface.
  It is used through out the Chisel package to report errors and warnings
  detected at runtime.
  */
object ChiselError {
  var hasErrors: Boolean = false;
  val ChiselErrors = new ArrayBuffer[ChiselError];

  def clear() {
    ChiselErrors.clear()
    hasErrors = false
  }

  /** emit an error message */
  def error(mf: => String, line: StackTraceElement) {
    hasErrors = true
    ChiselErrors += new ChiselError(() => mf, line)
  }

  def error(m: String) {
    val stack = Thread.currentThread().getStackTrace
    error(m, findFirstUserLine(stack) getOrElse stack(0))
  }

  /** Emit an informational message
    (useful to track long running passes) */
  def info(m: String) {
    println(m)
  }

  /** emit a warning message */
  def warning(m: => String) {
    val stack = Thread.currentThread().getStackTrace
    ChiselErrors += new ChiselError(() => m,
      findFirstUserLine(stack) getOrElse stack(0), 1)
  }

  def findFirstUserLine(stack: Array[StackTraceElement]): Option[StackTraceElement] = {
    findFirstUserInd(stack) map { stack(_) }
  }

  def findFirstUserInd(stack: Array[StackTraceElement]): Option[Int] = {
    def isUserCode(ste: StackTraceElement): Boolean = {
      val className = ste.getClassName()
      try {
        val cls = Class.forName(className)
        if( cls.getSuperclass() == classOf[Module] ) {
          true
        } else {
        /* XXX Do it the old way until we figure if it is safe
               to remove from Node.scala
           var line: StackTraceElement = findFirstUserLine(Thread.currentThread().getStackTrace)
         */
          val dotPos = className.lastIndexOf('.')
          if( dotPos > 0 ) {
            (className.subSequence(0, dotPos) != "Chisel") && !className.contains("scala") &&
            !className.contains("java") && !className.contains("$$")
          } else {
            false
          }
        }
      } catch {
        case e: java.lang.ClassNotFoundException => false
      }
    }
    val idx = stack.indexWhere(isUserCode)
    if(idx < 0) {
      println("COULDN'T FIND LINE NUMBER (" + stack(1) + ")")
      None
    } else {
      Some(idx)
    }
  }

  /** Prints error messages generated by Chisel at runtime. */
  def report() {
    if(!ChiselErrors.isEmpty){
      for(err <- ChiselErrors)  err.print;
    }
  }

  /** Throws an exception if there has been any error recorded
    before this point. */
  def checkpoint() {
    if(hasErrors) {
      throw new IllegalStateException(
        "CODE HAS " + ChiselErrors.length + " ERRORS/WARNINGS");
    }
  }
}

class ChiselError(val errmsgFun: () => String, val errline: StackTraceElement,
val errlevel: Int = 0) {

  val level = errlevel
  val line = errline
  val msgFun = errmsgFun

  def print() {
    /* Following conventions for error formatting */
    val levelstr = if( level == 0 ) "error" else "warning"
    if( line != null ) {
      println(line.getFileName + ":" + line.getLineNumber
        + ": " + levelstr + ": " + msgFun() +
        " in class " + line.getClassName)
    } else {
      println(levelstr + ": " + msgFun())
    }
  }
}
