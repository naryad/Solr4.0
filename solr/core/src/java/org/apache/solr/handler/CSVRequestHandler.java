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

package org.apache.solr.handler;

import org.apache.solr.common.util.NamedList;

/**
 * use {@link UpdateRequestHandler}
 */
@Deprecated
public class CSVRequestHandler extends UpdateRequestHandler {

  @Override
  public void init(NamedList args) {
    super.init(args);
    setAssumeContentType("application/csv");
    log.warn("Using deprecated class: "+this.getClass().getSimpleName()+" -- replace with UpdateRequestHandler");
  }

  //////////////////////// SolrInfoMBeans methods //////////////////////
  @Override
  public String getDescription() {
    return "Add/Update multiple documents with CSV formatted rows";
  }

  @Override
  public String getSource() {
    return "$URL: https://svn.apache.org/repos/asf/lucene/dev/trunk/solr/core/src/java/org/apache/solr/handler/CSVRequestHandler.java $";
  }
}





