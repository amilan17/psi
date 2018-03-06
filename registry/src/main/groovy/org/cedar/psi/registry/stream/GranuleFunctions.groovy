package org.cedar.psi.registry.stream

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.avro.generic.GenericRecord

@Slf4j
class GranuleFunctions {

  static mergeGranules = { GenericRecord aggregate, GenericRecord newValue ->
    log.debug("Merging new value $newValue into existing aggregate ${aggregate}")
    newValue.each { k, v ->
      aggregate.put(k as String, v)   // GenericRecord only have put and get functions
    }
    return aggregate
  }

}
