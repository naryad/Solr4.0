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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;

/**
 * Simple tests to ensure this factory is working
 */
public class TestTrimFilterFactory extends BaseTokenStreamTestCase {
  public void testTrimming() throws Exception {
    TrimFilterFactory factory = new TrimFilterFactory();
    Map<String,String> args = new HashMap<String,String>();
    args.put("updateOffsets", "false");
    factory.init(args);
    TokenStream ts = factory.create(new MockTokenizer(new StringReader("trim me    "), MockTokenizer.KEYWORD, false));
    assertTokenStreamContents(ts, new String[] { "trim me" });
  }
}
