package org.codice.ddf.catalog.content.monitor;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IngestProcessor implements Processor {
  private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessor.class);

  private FileSystemPersistenceProvider persistenceProvider;

  public IngestProcessor(String persistentID) {
    persistenceProvider = new FileSystemPersistenceProvider(persistentID);
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    Message in = exchange.getIn();
    Object b = in.getBody();
    String key = "";
    String op = "";
    Object opObj = in.getHeader("operation");

    if(opObj instanceof String){
      op = (String) opObj;
    } else{
      LOGGER.debug("Expected a String but got a {}", opObj.getClass().toString());
    }

    if (b instanceof GenericFile) {
      GenericFile f = (GenericFile) b;
      key = f.getAbsoluteFilePath();
      key = DigestUtils.sha1Hex(key);
    } else{
      LOGGER.debug("Expected a GenericFile but got a {}", b.getClass().toString());
    }

    if(op.equals("DELETE")){
      persistenceProvider.delete(key);
    }
    else {
      // TODO: Access service peter will create to get metacardID
      String fakeID = "fakeMetacardId";
      persistenceProvider.store(key, fakeID);
    }
  }

  private String getShaFor(String value) {
    return DigestUtils.sha1Hex(value);
  }
}
