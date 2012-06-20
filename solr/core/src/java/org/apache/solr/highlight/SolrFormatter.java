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

package org.apache.solr.highlight;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrInfoMBean;
import org.apache.solr.util.plugin.NamedListInitializedPlugin;

public interface SolrFormatter extends SolrInfoMBean, NamedListInitializedPlugin {

  /** <code>init</code> will be called just once, immediately after creation.
   * <p>The args are user-level initialization parameters that
   * may be specified when declaring a request handler in
   * solrconfig.xml
   */
  public void init(NamedList args);

  /**
   * Return a {@link org.apache.lucene.search.highlight.Formatter} appropriate for this field.
   * 
   * @param fieldName The name of the field
   * @param params The params controlling Highlighting
   * @return An appropriate {@link org.apache.lucene.search.highlight.Formatter}
   */
  public Formatter getFormatter(String fieldName, SolrParams params );
}

