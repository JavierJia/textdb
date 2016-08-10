package edu.uci.ics.textdb.dataflow.common;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

import edu.uci.ics.textdb.api.common.Attribute;
import edu.uci.ics.textdb.api.storage.IDataStore;
import edu.uci.ics.textdb.common.exception.DataFlowException;

/**
 * A predicate used by MultiQueryIndexSourceOperator to indicate multiple queries of one dataStore.
 * @author Zuozhi Wang (zuozhiw@gmail.com)
 *
 */
public class MultiQueryPredicate {

    private List<Query> queryList;
    private List<String> queryStringList;
    private IDataStore dataStore;
    private List<Attribute> attributeList;
    private Analyzer luceneAnalyzer;
    
    public MultiQueryPredicate(List<Query> queryList, List<String> queryStringList, IDataStore dataStore, 
            List<Attribute> attributeList, Analyzer luceneAnalyzer) throws DataFlowException {
        if (this.queryList.size() != this.queryStringList.size()) {
            throw new DataFlowException("queryList and queryStringList have different sizes");
        }
        if (this.queryList.isEmpty() || this.queryStringList.isEmpty()) {
            throw new DataFlowException("queryList or queryStringList is empty");
        }
        
        this.queryList = queryList;
        this.queryStringList = queryStringList;
        this.dataStore = dataStore;
        this.attributeList = attributeList;
        this.luceneAnalyzer = luceneAnalyzer;
    }
    
    public List<Query> getQueryList() {
        return queryList;
    }

    public List<String> getQueryStringList() {
        return queryStringList;
    }

    public IDataStore getDataStore() {
        return dataStore;
    }

    public List<Attribute> getAttributeList() {
        return attributeList;
    }

    public Analyzer getLuceneAnalyzer() {
        return luceneAnalyzer;
    }

}
