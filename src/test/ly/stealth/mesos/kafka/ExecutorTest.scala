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
import java.util
import java.util.Properties
import scala.collection.JavaConversions._
import java.io.StringWriter
import org.apache.mesos.Protos.TaskState

class ExecutorTest extends MesosTestCase {
  @Test
  def optionMap {
    val task = this.task(data = props(Util.parseMap("a=1,b=2")))
    val read: Map[String, String] = Executor.optionMap(task)
    assertEquals(2, read.size)
    assertEquals("1", read.getOrElse("a", null))
    assertEquals("2", read.getOrElse("b", null))
  }

  @Test(timeout = 5000)
  def startBroker_success {
    Executor.startBroker(executorDriver, task())
    executorDriver.waitForStatusUpdates(1)
    assertEquals(1, executorDriver.statusUpdates.size())

    var status = executorDriver.statusUpdates.get(0)
    assertEquals(TaskState.TASK_RUNNING, status.getState)
    assertTrue(Executor.server.isStarted)

    Executor.server.stop()
    executorDriver.waitForStatusUpdates(2)

    assertEquals(2, executorDriver.statusUpdates.size())
    status = executorDriver.statusUpdates.get(1)
    assertEquals(TaskState.TASK_FINISHED, status.getState)
    assertFalse(Executor.server.isStarted)
  }

  @Test(timeout = 5000)
  def startBroker_failure {
    Executor.server.asInstanceOf[TestBrokerServer].failOnStart = true
    Executor.startBroker(executorDriver, task())

    executorDriver.waitForStatusUpdates(1)
    assertEquals(1, executorDriver.statusUpdates.size())

    val status = executorDriver.statusUpdates.get(0)
    assertEquals(TaskState.TASK_FAILED, status.getState)
    assertFalse(Executor.server.isStarted)
  }

  @Test
  def stopBroker {
    Executor.server.start(Map())
    assertTrue(Executor.server.isStarted)

    Executor.stopBroker
    assertFalse(Executor.server.isStarted)

    Executor.stopBroker // no error
  }

  @Test(timeout = 5000)
  def launchTask {
    Executor.launchTask(executorDriver, task())
    executorDriver.waitForStatusUpdates(1)
    assertTrue(Executor.server.isStarted)
  }

  @Test
  def killTasks {
    Executor.server.start(Map())
    Executor.killTask(executorDriver, taskId())
    assertFalse(Executor.server.isStarted)
  }

  @Test
  def shutdown {
    Executor.server.start(Map())
    Executor.shutdown(executorDriver)
    assertFalse(Executor.server.isStarted)
  }

  private def props(map: util.Map[String, String]): String = {
    val props = new Properties()
    for ((k, v) <- map) props.setProperty(k, v)

    val buffer = new StringWriter()
    props.store(buffer, null)
    "" + buffer
  }
}
