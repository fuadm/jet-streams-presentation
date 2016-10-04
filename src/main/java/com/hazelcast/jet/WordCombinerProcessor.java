/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet;

import com.hazelcast.jet.io.Pair;
import com.hazelcast.jet.runtime.InputChunk;
import com.hazelcast.jet.runtime.JetPair;
import com.hazelcast.jet.runtime.OutputCollector;

import java.util.HashMap;
import java.util.Map;

/**
 * Processor which will sum incoming counts and emit total counts them when all the input has been consumed.
 */
public class WordCombinerProcessor implements Processor<Pair<String, Integer>, Pair<String, Integer>> {

    private Map<String, Integer> countsCache = new HashMap<>();

    @Override
    public boolean process(InputChunk<Pair<String, Integer>> input,
                           OutputCollector<Pair<String, Integer>> output,
                           String sourceName) throws Exception {

        // increment the count in the cache if word exists, otherwise create new entry in cache
        for (Pair<String, Integer> word : input) {
            Integer value = this.countsCache.get(word.getKey());
            if (value == null) {
                countsCache.put(word.getKey(), word.getValue());
            } else {
                countsCache.put(word.getKey(), value + word.getValue());
            }
        }
        return true;
    }

    @Override
    public boolean complete(OutputCollector<Pair<String, Integer>> output) throws Exception {

        // iterate through the cache and emit all the counts.
        // note that if you have a very large cache, it would be better here to emit a limited
        // number of entries at each call to finalizeProcessor.

        for (Map.Entry<String, Integer> count : countsCache.entrySet()) {
            output.collect(new JetPair<>(count.getKey(), count.getValue()));
        }
        return true;
    }

    @Override
    public void after() {
        // free up memory after execution
        countsCache.clear();
    }
}
