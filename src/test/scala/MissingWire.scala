/*
 Copyright (c) 2011, 2012, 2013, 2014, 2015 The Regents of the University of
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

import Chisel._
import org.junit.Assert._
import org.junit.Test
import org.junit.Ignore

class MissingWireSuite extends TestSuite {
  @Test def testMissingWire() {
    println("\ntestMissingWire ...")

    class OptionalWire(noWire: Boolean) extends Module {
    
      val io = new Bundle {
        val out = UInt(OUTPUT, 16)
      }

      val aLit = UInt(42, 16)
      // The following should fail without a Wire wrapper.
      if (noWire) {
        val aTemp = UInt(width=16)
        aTemp := aLit
        io.out := aTemp
      } else {
        val aTemp = Wire(UInt(width=16))
        aTemp := aLit
        io.out := aTemp
      }
    }

    class WireTester(c: OptionalWire) extends Tester(c) {
      expect(c.io.out, 42)
    }

    val testArgs = chiselEnvironmentArguments() ++ Array("--targetDir", dir.getPath.toString(),
          "--minimumCompatibility", "3.0.0", "--wError", "--backend", "c", "--genHarness", "--compile", "--test")
    intercept[IllegalStateException] {
      // This should fail since we don't use a Wire wrapper
      chiselMainTest(testArgs, () => Module(new OptionalWire(true))){ c => new WireTester(c) }
    }
    assertTrue(ChiselError.hasErrors)
    
    // This should pass
    chiselMainTest(testArgs, () => Module(new OptionalWire(false))){ c => new WireTester(c) }
    assertFalse(ChiselError.hasErrors)
  }

  @Test def testBadWireWrap() {
    println("\ntestBadWireWrap ...")

    class OptionalWire(noWire: Boolean) extends Module {
    
      val io = new Bundle {
        val out = UInt(OUTPUT, 16)
      }

      val aLit = UInt(42, 16)
      // The following should pass without a Wire wrapper.
      if (noWire) {
        val aTemp = aLit
        io.out := aTemp
      } else {
        val aTemp = Wire(aLit)
        io.out := aTemp
      }
    }

    class WireTester(c: OptionalWire) extends Tester(c) {
      expect(c.io.out, 42)
    }

    val testArgs = chiselEnvironmentArguments() ++ Array("--targetDir", dir.getPath.toString(),
          "--minimumCompatibility", "3.0.0", "--wError", "--backend", "c", "--genHarness", "--compile", "--test")
    
    // This should pass (no Wire() wrapper)
    chiselMainTest(testArgs, () => Module(new OptionalWire(true))){ c => new WireTester(c) }
    assertFalse(ChiselError.hasErrors)

    // This should fail since we use a Wire wrapper around a node with data
    intercept[IllegalStateException] {
      chiselMainTest(testArgs, () => Module(new OptionalWire(false))){ c => new WireTester(c) }
    }
    assertTrue(ChiselError.hasErrors)
  }
}
