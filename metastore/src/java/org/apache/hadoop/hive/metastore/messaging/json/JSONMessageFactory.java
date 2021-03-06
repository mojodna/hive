/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hadoop.hive.metastore.messaging.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.Function;
import org.apache.hadoop.hive.metastore.api.Index;
import org.apache.hadoop.hive.metastore.api.NotificationEvent;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.messaging.AddPartitionMessage;
import org.apache.hadoop.hive.metastore.messaging.AlterIndexMessage;
import org.apache.hadoop.hive.metastore.messaging.AlterPartitionMessage;
import org.apache.hadoop.hive.metastore.messaging.AlterTableMessage;
import org.apache.hadoop.hive.metastore.messaging.CreateDatabaseMessage;
import org.apache.hadoop.hive.metastore.messaging.CreateFunctionMessage;
import org.apache.hadoop.hive.metastore.messaging.CreateIndexMessage;
import org.apache.hadoop.hive.metastore.messaging.CreateTableMessage;
import org.apache.hadoop.hive.metastore.messaging.DropDatabaseMessage;
import org.apache.hadoop.hive.metastore.messaging.DropFunctionMessage;
import org.apache.hadoop.hive.metastore.messaging.DropIndexMessage;
import org.apache.hadoop.hive.metastore.messaging.DropPartitionMessage;
import org.apache.hadoop.hive.metastore.messaging.DropTableMessage;
import org.apache.hadoop.hive.metastore.messaging.InsertMessage;
import org.apache.hadoop.hive.metastore.messaging.MessageDeserializer;
import org.apache.hadoop.hive.metastore.messaging.MessageFactory;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TJSONProtocol;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * The JSON implementation of the MessageFactory. Constructs JSON implementations of each
 * message-type.
 */
public class JSONMessageFactory extends MessageFactory {

  private static final Logger LOG = LoggerFactory.getLogger(JSONMessageFactory.class.getName());

  private static JSONMessageDeserializer deserializer = new JSONMessageDeserializer();

  @Override
  public MessageDeserializer getDeserializer() {
    return deserializer;
  }

  @Override
  public String getVersion() {
    return "0.1";
  }

  @Override
  public String getMessageFormat() {
    return "json";
  }

  @Override
  public CreateDatabaseMessage buildCreateDatabaseMessage(Database db) {
    return new JSONCreateDatabaseMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, db.getName(), now());
  }

  @Override
  public DropDatabaseMessage buildDropDatabaseMessage(Database db) {
    return new JSONDropDatabaseMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, db.getName(), now());
  }

  @Override
  public CreateTableMessage buildCreateTableMessage(Table table) {
    return new JSONCreateTableMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, table, now());
  }

  @Override
  public AlterTableMessage buildAlterTableMessage(Table before, Table after) {
    return new JSONAlterTableMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, after, now());
  }

  @Override
  public DropTableMessage buildDropTableMessage(Table table) {
    return new JSONDropTableMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, table.getDbName(),
        table.getTableName(), now());
  }

  @Override
  public AddPartitionMessage buildAddPartitionMessage(Table table,
      Iterator<Partition> partitionsIterator) {
    return new JSONAddPartitionMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, table,
        partitionsIterator, now());
  }

  @Override
  public AlterPartitionMessage buildAlterPartitionMessage(Table table, Partition before,
      Partition after) {
    return new JSONAlterPartitionMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, table, before, after,
        now());
  }

  @Override
  public DropPartitionMessage buildDropPartitionMessage(Table table,
      Iterator<Partition> partitionsIterator) {
    return new JSONDropPartitionMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, table.getDbName(),
        table.getTableName(), getPartitionKeyValues(table, partitionsIterator), now());
  }

  @Override
  public CreateFunctionMessage buildCreateFunctionMessage(Function fn) {
    return new JSONCreateFunctionMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, fn, now());
  }

  @Override
  public DropFunctionMessage buildDropFunctionMessage(Function fn) {
    return new JSONDropFunctionMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, fn, now());
  }

  @Override
  public CreateIndexMessage buildCreateIndexMessage(Index idx) {
    return new JSONCreateIndexMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, idx, now());
  }

  @Override
  public DropIndexMessage buildDropIndexMessage(Index idx) {
    return new JSONDropIndexMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, idx, now());
  }

  @Override
  public AlterIndexMessage buildAlterIndexMessage(Index before, Index after) {
    return new JSONAlterIndexMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, before, after, now());
  }

  @Override
  public InsertMessage buildInsertMessage(String db, String table, Map<String, String> partKeyVals,
      List<String> files) {
    return new JSONInsertMessage(MS_SERVER_URL, MS_SERVICE_PRINCIPAL, db, table, partKeyVals,
        files, now());
  }

  private long now() {
    return System.currentTimeMillis() / 1000;
  }

  static Map<String, String> getPartitionKeyValues(Table table, Partition partition) {
    Map<String, String> partitionKeys = new LinkedHashMap<String, String>();
    for (int i = 0; i < table.getPartitionKeysSize(); ++i)
      partitionKeys.put(table.getPartitionKeys().get(i).getName(), partition.getValues().get(i));
    return partitionKeys;
  }

  static List<Map<String, String>> getPartitionKeyValues(final Table table,
      Iterator<Partition> iterator) {
    return Lists.newArrayList(Iterators.transform(iterator,
        new com.google.common.base.Function<Partition, Map<String, String>>() {
          @Override
          public Map<String, String> apply(@Nullable Partition partition) {
            return getPartitionKeyValues(table, partition);
          }
        }));
  }

  static String createTableObjJson(Table tableObj) throws TException {
    TSerializer serializer = new TSerializer(new TJSONProtocol.Factory());
    return serializer.toString(tableObj, "UTF-8");
  }

  static String createPartitionObjJson(Partition partitionObj) throws TException {
    TSerializer serializer = new TSerializer(new TJSONProtocol.Factory());
    return serializer.toString(partitionObj, "UTF-8");
  }

  static String createFunctionObjJson(Function functionObj) throws TException {
    TSerializer serializer = new TSerializer(new TJSONProtocol.Factory());
    return serializer.toString(functionObj, "UTF-8");
  }

  static String createIndexObjJson(Index indexObj) throws TException {
    TSerializer serializer = new TSerializer(new TJSONProtocol.Factory());
    return serializer.toString(indexObj, "UTF-8");
  }

  public static ObjectNode getJsonTree(NotificationEvent event) throws Exception {
    JsonParser jsonParser = (new JsonFactory()).createJsonParser(event.getMessage());
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(jsonParser, ObjectNode.class);
  }

  public static Table getTableObj(ObjectNode jsonTree) throws Exception {
    TDeserializer deSerializer = new TDeserializer(new TJSONProtocol.Factory());
    Table tableObj = new Table();
    String tableJson = jsonTree.get("tableObjJson").asText();
    deSerializer.deserialize(tableObj, tableJson, "UTF-8");
    return tableObj;
  }

  public static List<Partition> getPartitionObjList(ObjectNode jsonTree) throws Exception {
    TDeserializer deSerializer = new TDeserializer(new TJSONProtocol.Factory());
    List<Partition> partitionObjList = new ArrayList<Partition>();
    Partition partitionObj = new Partition();
    Iterator<JsonNode> jsonArrayIterator = jsonTree.get("partitionListJson").iterator();
    while (jsonArrayIterator.hasNext()) {
      deSerializer.deserialize(partitionObj, jsonArrayIterator.next().asText(), "UTF-8");
      partitionObjList.add(partitionObj);
    }
    return partitionObjList;
  }

  public static Function getFunctionObj(ObjectNode jsonTree) throws Exception {
    TDeserializer deSerializer = new TDeserializer(new TJSONProtocol.Factory());
    Function funcObj = new Function();
    String tableJson = jsonTree.get("functionObjJson").asText();
    deSerializer.deserialize(funcObj, tableJson, "UTF-8");
    return funcObj;
  }

  public static Index getIndexObj(ObjectNode jsonTree) throws Exception {
    return getIndexObj(jsonTree, "indexObjJson");
  }

  public static Index getIndexObj(ObjectNode jsonTree, String indexObjKey) throws Exception {
    TDeserializer deSerializer = new TDeserializer(new TJSONProtocol.Factory());
    Index indexObj = new Index();
    String tableJson = jsonTree.get(indexObjKey).asText();
    deSerializer.deserialize(indexObj, tableJson, "UTF-8");
    return indexObj;
  }
}
