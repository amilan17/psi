package org.cedar.psi.registry.stream

import groovy.json.JsonSlurper
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord

/*
* create a generic data type schema and register it to the schema-registry
* also populate data from the raw data*/
class CreateSchema {

    static GenericRecord reduceValue(String value){

        //get schema definition (see under resources aggregatedSchema.avsc )
//        final InputStream rawGranuleSchema = RawGranuleStreamConfig.class.getClassLoader()
//                .getResourceAsStream("src/main/resources/aggregatedSchema.avsc")
        def slurper = new JsonSlurper()
        def slurperValue = slurper.parseText(value)

        String GRANULE_SCHEMA = "{" +
                "   \"namespace\": \"org.cedar.psi.api\"," +
                "   \"type\": \"record\", " +
                "   \"name\": \"Granule\"," +
                "   \"fields\": [" +
                "       {\"name\": \"trackingId\", \"type\": \"string\"}," +
                "       {\"name\": \"dataStream\", \"type\": \"string\"}," +
                "       {\"name\": \"checksum\", \"type\": \"string\",\"default\":\"null\"}," +
                "       {\"name\": \"relativePath\", \"type\": \"string\", \"default\":\"null\"}," +
                "       {\"name\": \"path\", \"type\": \"string\",\"default\":\"null\"}," +
                "       {\"name\": \"fileSize\", \"type\": [\"null\",\"int\"], \"default\":\"null\"}," +
                "       {\"name\": \"lastUpdated\", \"type\": [\"null\",\"string\"], \"default\":\"null\" }" +
                " ]" +
                "}"
        Schema.Parser parser = new Schema.Parser()
//        Schema schema = parser.parse(GRANULE_SCHEMA)
        Schema schema = parser.parse(GRANULE_SCHEMA)
        GenericRecord avroRecord = new GenericData.Record(schema)
        if(slurperValue.trackingId==null){
            avroRecord.put("trackingId", UUID.randomUUID()) //if tracking id not provided generate a new uuid
        }else{
            avroRecord.put("trackingId", slurperValue.trackingId)

        }

        avroRecord.put("dataStream", slurperValue.dataStream)
        avroRecord.put("checksum", slurperValue.checksum)
        avroRecord.put("relativePath", slurperValue.relativePath)
        avroRecord.put("path", slurperValue.path)
        avroRecord.put("fileSize", slurperValue.fileSize)
        avroRecord.put("lastUpdated", slurperValue.lastUpdated)
        return avroRecord
    }
}

