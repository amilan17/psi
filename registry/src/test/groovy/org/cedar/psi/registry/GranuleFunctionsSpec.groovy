package org.cedar.psi.registry

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.cedar.psi.registry.stream.CreateSchema
import org.cedar.psi.registry.stream.GranuleFunctions
import spock.lang.Specification
import tech.allegro.schema.json2avro.converter.AvroConversionException
import tech.allegro.schema.json2avro.converter.JsonAvroConverter


class GranuleFunctionsSpec extends Specification {

  def 'merge function merges json strings'() {
    def currentAggregate = '{"trackingId":"ABC","message":"this is a test","answer": 42}'
    def newValue = '{"trackingId":"ABC", "message":"this is only a test","greeting": "hello, world!"}'
    def mergedAggregate = '{"trackingId":"ABC","message":"this is only a test","answer":42,"greeting":"hello, world!"}'

    expect:
    GranuleFunctions.mergeGranules(genGonvert(currentAggregate), genGonvert(newValue)) == genGonvert(mergedAggregate)
//    GranuleFunctions.mergeGranules(genGonvert(currentAggregate), genGonvert(newValue)) == genGonvert(mergedAggregate)

  }

  def 'populate schema'() {
    def aggregatedValue = '{"trackingId":"ABC","dataStream":"this is a test","checksum": "42","relativePath":"hello, world!"}'
    def expectedValue = '{"trackingId":"ABC","dataStream":"this is only a test","checksum":"42","relativePath":"hello, world!"}'

    expect:
    def genValue =  CreateSchema.transform(aggregatedValue)
    genValue.get("trackingId") == genGonvert(expectedValue).get("trackingId")

  }

  def 'populate schema with random UUID'() {
    def aggregatedValue = '{"dataStream":"this is a test","checksum": "42","relativePath":"hello, world!"}'
    def expectedValue = '{"trackingId":"ABC","dataStream":"this is only a test","checksum":"42","relativePath":"hello, world!"}'

    expect:
    def genValue =  CreateSchema.transform(aggregatedValue)
    genValue.get("trackingId") != null

  }

  def genGonvert(String json){

    String schema ="{" +
            "   \"namespace\": \"org.cedar.psi.api\"," +
            "   \"type\": \"record\", " +
            "   \"name\": \"Granule\"," +
            "   \"fields\": [" +
            "       {\"name\": \"trackingId\", \"type\": \"string\",\"default\":\"null\"}," +
            "       {\"name\": \"dataStream\", \"type\": \"string\",\"default\":\"null\"}," +
            "       {\"name\": \"checksum\", \"type\": \"string\",\"default\":\"null\"}," +
            "       {\"name\": \"relativePath\", \"type\": \"string\", \"default\":\"null\"}," +
            "       {\"name\": \"path\", \"type\": \"string\", \"default\":\"null\"}," +
            "       {\"name\": \"fileSize\", \"type\": \"string\", \"default\":\"null\"}," +
            "       {\"name\": \"lastUpdated\", \"type\": \"string\", \"default\":\"null\"}" +
            " ]" +
            "}"

    JsonAvroConverter converter = new JsonAvroConverter()
// conversion to GenericData.Record
    GenericData.Record record = converter.convertToGenericDataRecord(json.getBytes(), new Schema.Parser().parse(schema))

    return record

  }

}
