package edu.uci.ics.textdb.api.storage;

import java.util.List;

import edu.uci.ics.textdb.api.common.Attribute;
import edu.uci.ics.textdb.api.common.ITuple;

/**
 * 
 * @author sandeepreddy602
 *
 */
public interface IDataStore {
    public void storeData(List<Attribute> schema, List<ITuple> tuples) throws Exception;
    public void clearData() throws Exception;
}
