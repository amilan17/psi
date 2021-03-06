package org.cedar.psi.registry.stream

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.kafka.streams.kstream.Reducer
import org.apache.kafka.streams.kstream.ValueJoiner
import org.apache.kafka.streams.kstream.ValueMapperWithKey

@Slf4j
@CompileStatic
class StreamFunctions {

  static Reducer identityReducer = new Reducer() {
    @Override
    Object apply(Object aggregate, Object nextValue) {
      nextValue
    }
  }

  static Reducer<String> mergeJsonStrings = new Reducer<String>() {
    @Override
    String apply(String aggregate, String nextValue) {
      log.debug("Merging new value $nextValue into existing aggregate ${aggregate}")
      def slurper = new JsonSlurper()
      def slurpedAggregate = aggregate ? slurper.parseText(aggregate as String) as Map : [:]
      def slurpedNewValue = slurper.parseText(nextValue as String) as Map
      def result = slurpedAggregate + slurpedNewValue
      return JsonOutput.toJson(result)
    }
  }

  /**
   * Returns a ValueJoiner which returns json with the left value under the given left key
   * and the right value under the given right key. For example:
   *
   * def joiner = buildKeyedJsonJoiner('left', 'right')
   * joiner.apply('{"hello": "world"}', '{"answer": 42}')
   * >> '{"left": {"hello": "world"}, "right": {"answer": 42}}'
   *
   * @param leftKey  The key to put the left value under
   * @param rightKey The key to put the right value under
   * @return         The combined result
   */
  static ValueJoiner<String, String, String> buildKeyedJsonJoiner(String leftKey, String rightKey) {
    return new ValueJoiner<String, String, String>() {
      @Override
      String apply(String leftValue, String rightValue) {
        log.debug("Joining left value ${leftValue} with right value ${rightValue}")
        def slurper = new JsonSlurper()
        def leftSlurped = leftValue ? slurper.parseText(leftValue) as Map : null
        def rightSlurped = rightValue ? slurper.parseText(rightValue) as Map : null
        def result = [(leftKey): leftSlurped, (rightKey): rightSlurped]
        return JsonOutput.toJson(result)
      }
    }
  }

  /**
   * Returns a ValueJoiner which returns json with the left value under the given left key
   * and merged into the right value.
   *
   * def joiner = buildKeyedJsonJoiner('left')
   * joiner.apply('{"hello": "world"}', '{"answer": 42}')
   * >> '{"left": {"hello": "world"}, "answer": 42}'
   *
   * @param leftKey  The key to put the left value under
   * @param rightKey The key to put the right value under
   * @return         The combined result
   */
  static ValueJoiner<String, String, String> buildKeyedJsonJoiner(String leftKey) {
    return new ValueJoiner<String, String, String>() {
      @Override
      String apply(String leftValue, String rightValue) {
        log.debug("Joining left value ${leftValue} with right value ${rightValue}")
        def slurper = new JsonSlurper()
        Map leftSlurped = leftValue ? slurper.parseText(leftValue) as Map : null
        Map rightSlurped = rightValue ? slurper.parseText(rightValue) as Map : [:]
        Map result = [(leftKey): leftSlurped] + rightSlurped
        return JsonOutput.toJson(result)
      }
    }
  }

  static ValueMapperWithKey<String, String, String> parsedInfoNormalizer = new ValueMapperWithKey<String, String, String>() {
    @Override
    String apply(String readOnlyKey, String value) {
      def slurper = new JsonSlurper()
      def valueMap = value ? slurper.parseText(value) as Map : [:]
      if (!valueMap.containsKey('publishing')) {
        valueMap.put('publishing', [private: false])
      }
      return JsonOutput.toJson(valueMap)
    }
  }

}
