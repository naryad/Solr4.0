/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.analysis;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;

import java.io.Reader;
import java.util.Map;

/**
 * Factory for {@link StandardTokenizer}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_stndrd" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.StandardTokenizerFactory" maxTokenLength="255"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */

public class StandardTokenizerFactory extends TokenizerFactory {
  
  private int maxTokenLength;
  
  @Override
  public void init(Map<String,String> args) {
    super.init(args);
    assureMatchVersion();
    maxTokenLength = getInt("maxTokenLength", 
                            StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
  }

  public StandardTokenizer create(Reader input) {
    StandardTokenizer tokenizer
      = new StandardTokenizer(luceneMatchVersion, input); 
    tokenizer.setMaxTokenLength(maxTokenLength);
    return tokenizer;
  }
}
