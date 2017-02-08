package io.iohk.ethereum.db.dataSource

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

import io.iohk.iodb.{ByteArrayWrapper, LSMStore}

class IodbDataSource(lSMStore: LSMStore) extends DataSource {

  import IodbDataSource._

  override def get(namespace: Namespace, key: Key): Option[Value] =
    lSMStore.get(ByteArrayWrapper((namespace ++ key).toArray)).map(v => v.data.toIndexedSeq)

  override def update(namespace: Namespace, toRemove: Seq[Key], toUpsert: Seq[(Key, Value)]): DataSource = {
    lSMStore.update(
      ByteArrayWrapper(storageVersionGen()),
      toRemove.map(key => ByteArrayWrapper((namespace ++ key).toArray)),
      asStorables(namespace, toUpsert))
    new IodbDataSource(lSMStore)
  }

  private def asStorables(namespace: Namespace,
                          keyValues: Seq[(Key, Value)]): Seq[(ByteArrayWrapper, ByteArrayWrapper)] =
    keyValues.map(kv => ByteArrayWrapper((namespace ++ kv._1).toArray) -> ByteArrayWrapper(kv._2.toArray))
}


object IodbDataSource {
  private val updateCounter = new AtomicLong(System.currentTimeMillis())

  private def storageVersionGen(): Array[Byte] = {
    ByteBuffer.allocate(java.lang.Long.SIZE / java.lang.Byte.SIZE).putLong(updateCounter.incrementAndGet()).array()
  }
}