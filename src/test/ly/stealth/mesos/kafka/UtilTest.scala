/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ly.stealth.mesos.kafka

import org.junit.Test
import org.junit.Assert._
import ly.stealth.mesos.kafka.Util.{Wildcard, Period}
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import java.util

class UtilTest {
  @Test
  def parseMap {
    var map = Util.parseMap("a=1,b=2", ",", "=")
    assertEquals(2, map.size())
    assertEquals("1", map.get("a"))
    assertEquals("2", map.get("b"))

    // missing pair
    map = Util.parseMap("a=1,,b=2", ",", "=")
    assertEquals(2, map.size())

    // missing value
    try {
      Util.parseMap("a=1,b,c=3", ",", "=")
      fail()
    } catch { case e: IllegalArgumentException => }

    // null
    assertTrue(Util.parseMap(null, ",", "=").isEmpty)
  }

  @Test
  def parseJson {
    val node: Map[String, Object] = Util.parseJson("{\"a\":\"1\", \"b\":\"2\"}")
    assertEquals(2, node.size)
    assertEquals("1", node("a").asInstanceOf[String])
    assertEquals("2", node("b").asInstanceOf[String])
  }

  @Test
  def copyAndClose {
    val data = new Array[Byte](16 * 1024)
    for (i <- 0 until data.length) data(i) = i.toByte

    var inClosed = false
    var outClosed = false

    val in = new ByteArrayInputStream(data) {
      override def close(): Unit = super.close(); inClosed = true
    }
    val out = new ByteArrayOutputStream() {
      override def close(): Unit = super.close(); outClosed = true
    }

    Util.copyAndClose(in, out)
    assertTrue(util.Arrays.equals(data, out.toByteArray))
    assertTrue(inClosed)
    assertTrue(outClosed)
  }

  // Period
  @Test
  def Period_init() {
    new Period("1m")

    // empty
    try {
      new Period("")
      fail()
    } catch { case e: IllegalArgumentException => }

    // zero without units
    new Period("0")

    // no units
    try {
      new Period("1")
      fail()
    } catch { case e: IllegalArgumentException => }

    // no value
    try {
      new Period("ms")
      fail()
    } catch { case e: IllegalArgumentException => }

    // wrong unit
    try {
      new Period("1k")
      fail()
    } catch { case e: IllegalArgumentException => }

    // non-integer value
    try {
      new Period("0.5m")
      fail()
    } catch { case e: IllegalArgumentException => }

    // invalid value
    try {
      new Period("Xh")
      fail()
    } catch { case e: IllegalArgumentException => }
  }

  @Test
  def Period_ms {
    assertEquals(0, new Period("0").ms)
    assertEquals(1, new Period("1ms").ms)
    assertEquals(10, new Period("10ms").ms)

    val s: Int = 1000
    assertEquals(s, new Period("1s").ms)
    assertEquals(10 * s, new Period("10s").ms)

    val m: Int = 60 * s
    assertEquals(m, new Period("1m").ms)
    assertEquals(10 * m, new Period("10m").ms)

    val h: Int = 60 * m
    assertEquals(h, new Period("1h").ms)
    assertEquals(10 * h, new Period("10h").ms)

    val d: Int = 24 * h
    assertEquals(d, new Period("1d").ms)
    assertEquals(10 * d, new Period("10d").ms)
  }

  @Test
  def Period_value {
    assertEquals(0, new Period("0").value)
    assertEquals(10, new Period("10ms").value)
    assertEquals(50, new Period("50h").value)
    assertEquals(20, new Period("20d").value)
  }

  @Test
  def Period_unit {
    assertEquals("ms", new Period("0").unit)
    assertEquals("ms", new Period("10ms").unit)
    assertEquals("h", new Period("50h").unit)
    assertEquals("d", new Period("20d").unit)
  }

  @Test
  def Period_toString {
    assertEquals("10ms", "" + new Period("10ms"))
    assertEquals("5h", "" + new Period("5h"))
  }

  // Wildcard
  @Test
  def Wildcard_matches() {
    var wildcard: Wildcard = new Wildcard("1")
    assertTrue(wildcard.matches("1"))
    assertFalse(wildcard.matches("a1"))
    assertFalse(wildcard.matches("1a"))

    // ? char
    wildcard = new Wildcard("?")
    assertTrue(wildcard.matches("a"))
    assertTrue(wildcard.matches("b"))
    assertFalse(wildcard.matches(""))
    assertFalse(wildcard.matches("ab"))

    wildcard = new Wildcard("1?2")
    assertTrue(wildcard.matches("1a2"))
    assertTrue(wildcard.matches("1b2"))
    assertFalse(wildcard.matches("1ab2"))

    // * char
    wildcard = new Wildcard("*")
    assertTrue(wildcard.matches(""))
    assertTrue(wildcard.matches("a"))
    assertTrue(wildcard.matches("ab"))

    wildcard = new Wildcard("1*2")
    assertTrue(wildcard.matches("1a2"))
    assertTrue(wildcard.matches("1b2"))
    assertTrue(wildcard.matches("1ab2"))

    // complex case
    wildcard = new Wildcard("1?2*3")
    assertTrue(wildcard.matches("1a23"))
    assertTrue(wildcard.matches("1a2cd3"))
    assertFalse(wildcard.matches("1a2cd"))
    assertFalse(wildcard.matches("a2cd3"))
    assertFalse(wildcard.matches("1ab2cd3"))
  }
}
