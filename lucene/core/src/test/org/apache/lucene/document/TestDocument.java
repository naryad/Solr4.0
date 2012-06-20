package org.apache.lucene.document;

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

import java.io.StringReader;

import org.apache.lucene.analysis.EmptyTokenizer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;


/**
 * Tests {@link Document} class.
 */
public class TestDocument extends LuceneTestCase {
  
  String binaryVal = "this text will be stored as a byte array in the index";
  String binaryVal2 = "this text will be also stored as a byte array in the index";
  
  public void testBinaryField() throws Exception {
    Document doc = new Document();
    
    FieldType ft = new FieldType();
    ft.setStored(true);
    IndexableField stringFld = new Field("string", binaryVal, ft);
    IndexableField binaryFld = new StoredField("binary", binaryVal.getBytes());
    IndexableField binaryFld2 = new StoredField("binary", binaryVal2.getBytes());
    
    doc.add(stringFld);
    doc.add(binaryFld);
    
    assertEquals(2, doc.getFields().size());
    
    assertTrue(binaryFld.binaryValue() != null);
    assertTrue(binaryFld.fieldType().stored());
    assertFalse(binaryFld.fieldType().indexed());
    assertFalse(binaryFld.fieldType().tokenized());
    
    String binaryTest = doc.getBinaryValue("binary").utf8ToString();
    assertTrue(binaryTest.equals(binaryVal));
    
    String stringTest = doc.get("string");
    assertTrue(binaryTest.equals(stringTest));
    
    doc.add(binaryFld2);
    
    assertEquals(3, doc.getFields().size());
    
    BytesRef[] binaryTests = doc.getBinaryValues("binary");
    
    assertEquals(2, binaryTests.length);
    
    binaryTest = binaryTests[0].utf8ToString();
    String binaryTest2 = binaryTests[1].utf8ToString();
    
    assertFalse(binaryTest.equals(binaryTest2));
    
    assertTrue(binaryTest.equals(binaryVal));
    assertTrue(binaryTest2.equals(binaryVal2));
    
    doc.removeField("string");
    assertEquals(2, doc.getFields().size());
    
    doc.removeFields("binary");
    assertEquals(0, doc.getFields().size());
  }
  
  /**
   * Tests {@link Document#removeField(String)} method for a brand new Document
   * that has not been indexed yet.
   * 
   * @throws Exception on error
   */
  public void testRemoveForNewDocument() throws Exception {
    Document doc = makeDocumentWithFields();
    assertEquals(8, doc.getFields().size());
    doc.removeFields("keyword");
    assertEquals(6, doc.getFields().size());
    doc.removeFields("doesnotexists"); // removing non-existing fields is
                                       // siltenlty ignored
    doc.removeFields("keyword"); // removing a field more than once
    assertEquals(6, doc.getFields().size());
    doc.removeField("text");
    assertEquals(5, doc.getFields().size());
    doc.removeField("text");
    assertEquals(4, doc.getFields().size());
    doc.removeField("text");
    assertEquals(4, doc.getFields().size());
    doc.removeField("doesnotexists"); // removing non-existing fields is
                                      // siltenlty ignored
    assertEquals(4, doc.getFields().size());
    doc.removeFields("unindexed");
    assertEquals(2, doc.getFields().size());
    doc.removeFields("unstored");
    assertEquals(0, doc.getFields().size());
    doc.removeFields("doesnotexists"); // removing non-existing fields is
                                       // siltenlty ignored
    assertEquals(0, doc.getFields().size());
  }
  
  public void testConstructorExceptions() {
    FieldType ft = new FieldType();
    ft.setStored(true);
    new Field("name", "value", ft); // okay
    new StringField("name", "value", Field.Store.NO); // okay
    try {
      new Field("name", "value", new FieldType());
      fail();
    } catch (IllegalArgumentException e) {
      // expected exception
    }
    new Field("name", "value", ft); // okay
    try {
      FieldType ft2 = new FieldType();
      ft2.setStored(true);
      ft2.setStoreTermVectors(true);
      new Field("name", "value", ft2);
      fail();
    } catch (IllegalArgumentException e) {
      // expected exception
    }
  }
  
  /**
   * Tests {@link Document#getValues(String)} method for a brand new Document
   * that has not been indexed yet.
   * 
   * @throws Exception on error
   */
  public void testGetValuesForNewDocument() throws Exception {
    doAssert(makeDocumentWithFields(), false);
  }
  
  /**
   * Tests {@link Document#getValues(String)} method for a Document retrieved
   * from an index.
   * 
   * @throws Exception on error
   */
  public void testGetValuesForIndexedDocument() throws Exception {
    Directory dir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir);
    writer.addDocument(makeDocumentWithFields());
    IndexReader reader = writer.getReader();
    
    IndexSearcher searcher = newSearcher(reader);
    
    // search for something that does exists
    Query query = new TermQuery(new Term("keyword", "test1"));
    
    // ensure that queries return expected results without DateFilter first
    ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals(1, hits.length);
    
    doAssert(searcher.doc(hits[0].doc), true);
    writer.close();
    reader.close();
    dir.close();
  }

  public void testGetValues() {
    Document doc = makeDocumentWithFields();
    assertEquals(new String[] {"test1", "test2"},
                 doc.getValues("keyword"));
    assertEquals(new String[] {"test1", "test2"},
                 doc.getValues("text"));
    assertEquals(new String[] {"test1", "test2"},
                 doc.getValues("unindexed"));
    assertEquals(new String[0],
                 doc.getValues("nope"));
  }
  
  private Document makeDocumentWithFields() {
    Document doc = new Document();
    FieldType stored = new FieldType();
    stored.setStored(true);
    doc.add(new StringField("keyword", "test1", Field.Store.YES));
    doc.add(new StringField("keyword", "test2", Field.Store.YES));
    doc.add(new TextField("text", "test1", Field.Store.YES));
    doc.add(new TextField("text", "test2", Field.Store.YES));
    doc.add(new Field("unindexed", "test1", stored));
    doc.add(new Field("unindexed", "test2", stored));
    doc
        .add(new TextField("unstored", "test1", Field.Store.NO));
    doc
        .add(new TextField("unstored", "test2", Field.Store.NO));
    return doc;
  }
  
  private void doAssert(Document doc, boolean fromIndex) {
    IndexableField[] keywordFieldValues = doc.getFields("keyword");
    IndexableField[] textFieldValues = doc.getFields("text");
    IndexableField[] unindexedFieldValues = doc.getFields("unindexed");
    IndexableField[] unstoredFieldValues = doc.getFields("unstored");
    
    assertTrue(keywordFieldValues.length == 2);
    assertTrue(textFieldValues.length == 2);
    assertTrue(unindexedFieldValues.length == 2);
    // this test cannot work for documents retrieved from the index
    // since unstored fields will obviously not be returned
    if (!fromIndex) {
      assertTrue(unstoredFieldValues.length == 2);
    }
    
    assertTrue(keywordFieldValues[0].stringValue().equals("test1"));
    assertTrue(keywordFieldValues[1].stringValue().equals("test2"));
    assertTrue(textFieldValues[0].stringValue().equals("test1"));
    assertTrue(textFieldValues[1].stringValue().equals("test2"));
    assertTrue(unindexedFieldValues[0].stringValue().equals("test1"));
    assertTrue(unindexedFieldValues[1].stringValue().equals("test2"));
    // this test cannot work for documents retrieved from the index
    // since unstored fields will obviously not be returned
    if (!fromIndex) {
      assertTrue(unstoredFieldValues[0].stringValue().equals("test1"));
      assertTrue(unstoredFieldValues[1].stringValue().equals("test2"));
    }
  }
  
  public void testFieldSetValue() throws Exception {
    
    Field field = new StringField("id", "id1", Field.Store.YES);
    Document doc = new Document();
    doc.add(field);
    doc.add(new StringField("keyword", "test", Field.Store.YES));
    
    Directory dir = newDirectory();
    RandomIndexWriter writer = new RandomIndexWriter(random(), dir);
    writer.addDocument(doc);
    field.setStringValue("id2");
    writer.addDocument(doc);
    field.setStringValue("id3");
    writer.addDocument(doc);
    
    IndexReader reader = writer.getReader();
    IndexSearcher searcher = newSearcher(reader);
    
    Query query = new TermQuery(new Term("keyword", "test"));
    
    // ensure that queries return expected results without DateFilter first
    ScoreDoc[] hits = searcher.search(query, null, 1000).scoreDocs;
    assertEquals(3, hits.length);
    int result = 0;
    for (int i = 0; i < 3; i++) {
      Document doc2 = searcher.doc(hits[i].doc);
      Field f = (Field) doc2.getField("id");
      if (f.stringValue().equals("id1")) result |= 1;
      else if (f.stringValue().equals("id2")) result |= 2;
      else if (f.stringValue().equals("id3")) result |= 4;
      else fail("unexpected id field");
    }
    writer.close();
    reader.close();
    dir.close();
    assertEquals("did not see all IDs", 7, result);
  }
  
  // LUCENE-3616
  public void testInvalidFields() {
    try {
      new Field("foo", new EmptyTokenizer(new StringReader("")), StringField.TYPE_STORED);
      fail("did not hit expected exc");
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

  public void testBoost() throws Exception {
    Directory dir = newDirectory();
    IndexWriterConfig iwc = new IndexWriterConfig(TEST_VERSION_CURRENT, new MockAnalyzer(random()));
    iwc.setMergePolicy(newLogMergePolicy());
    IndexWriter iw = new IndexWriter(dir, iwc);
    Document doc = new Document();
    doc.add(new StringField("field1", "sometext", Field.Store.YES));
    doc.add(new TextField("field2", "sometext", Field.Store.NO));
    doc.add(new StringField("foo", "bar", Field.Store.NO));
    iw.addDocument(doc); // add an 'ok' document
    try {
      doc = new Document();
      // try to boost with norms omitted
      StringField field = new StringField("foo", "baz", Field.Store.NO);
      field.setBoost(5.0f);
      doc.add(field);
      iw.addDocument(doc);
      fail("didn't get any exception, boost silently discarded");
    } catch (UnsupportedOperationException expected) {
      // expected
    }
    DirectoryReader ir = DirectoryReader.open(iw, false);
    assertEquals(1, ir.numDocs());
    assertEquals("sometext", ir.document(0).get("field1"));
    ir.close();
    iw.close();
    dir.close();
  }
}
