package com.lunatech.energy.demo.serialization.jackson

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.lunatech.energy.demo.Machine.{MachineCreated, MachineEvent, MachineStatus, MachineStatusChanged}
import enumeratum.{Enum, EnumEntry}
import org.slf4j.LoggerFactory

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import scala.reflect.ClassTag

final class JacksonModule extends SimpleModule {

  private val log = LoggerFactory.getLogger(getClass)

  log.info("registering jackson serialization stuff")

  addSerializer(classOf[MachineStatus], new EnumEntrySerializer[MachineStatus]() {})
  addDeserializer(classOf[MachineStatus], new EnumEntryDeserializer[MachineStatus] {})

  addSerializer(classOf[OffsetDateTime], new OffsetDateTimeSerializer)
  addDeserializer(classOf[OffsetDateTime], new OffsetDateTimeDeserializer)

  setMixInAnnotation(classOf[MachineEvent], classOf[MachineEventMixin])
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[MachineCreated], name = "MachineCreated"),
    new JsonSubTypes.Type(value = classOf[MachineStatusChanged], name = "MachineStatusChanged")
  )
)
trait MachineEventMixin

abstract class EnumEntrySerializer[E <: EnumEntry](implicit classTag: ClassTag[E])
  extends StdSerializer[E](classTag.runtimeClass.asInstanceOf[Class[E]]) {

  override def serialize(enumEntry: E, gen: JsonGenerator, provider: SerializerProvider): Unit =
    gen.writeString(enumEntry.entryName)
}

abstract class EnumEntryDeserializer[E <: EnumEntry](implicit `enum`: Enum[E], classTag: ClassTag[E])
  extends StdDeserializer[E](classTag.runtimeClass.asInstanceOf[Class[E]]) {

  override def deserialize(parser: JsonParser, context: DeserializationContext): E =
    enum.withName(parser.getText)
}

final class OffsetDateTimeSerializer extends JsonSerializer[OffsetDateTime] {
  private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  override def serialize(value: OffsetDateTime, gen: JsonGenerator, serializers: SerializerProvider): Unit =
    gen.writeString(formatter.format(value))
}

final class OffsetDateTimeDeserializer extends JsonDeserializer[OffsetDateTime] {
  private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): OffsetDateTime =
    OffsetDateTime.parse(p.getText, formatter);
}
