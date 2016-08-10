package edu.uci.ics.textdb.dataflow.source;

import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.common.Schema;
import edu.uci.ics.textdb.api.dataflow.IOperator;
import edu.uci.ics.textdb.common.exception.DataFlowException;
import edu.uci.ics.textdb.dataflow.common.MultiQueryPredicate;
import edu.uci.ics.textdb.storage.DataReaderPredicate;
import edu.uci.ics.textdb.storage.reader.DataReader;

public class MultiQueryIndexSourceOperator implements IOperator {
    
    private MultiQueryPredicate predicate;
    private Schema outputSchema;
    private DataReader dataReader;
    
    private int queryCursor = -1;
    
    private int cursor = CLOSED;
    
    public MultiQueryIndexSourceOperator(MultiQueryPredicate multiQueryPredicate) {
        this.predicate = multiQueryPredicate;
    }

    @Override
    public void open() throws DataFlowException {
        if (cursor != CLOSED) {
            return;
        }      
        if (predicate.getQueryList().isEmpty()) {
            throw new DataFlowException("Query list is empty.");
        }
        try {
            queryCursor++;
            dataReader = generateDataReader();
            dataReader.open();
            
            outputSchema = dataReader.getOutputSchema();
            
            cursor = OPENED;
        } catch (Exception e) {
            throw new DataFlowException(e.getMessage(), e);
        }
        
    }
    
    @Override
    public ITuple getNextTuple() throws Exception {
        try {
            ITuple result = null;
            
            while (result == null) {
                result = dataReader.getNextTuple();
                if (result != null) {
                    break;
                }
                
                queryCursor++;
                if (queryCursor > predicate.getQueryList().size() - 1) {
                    return null;
                }
                
                dataReader.close();
                dataReader = generateDataReader();
                dataReader.open();
            }
            
            return result;
            
        } catch (Exception e) {
            throw new DataFlowException(e.getMessage(), e);
        }
    }
    
    @Override
    public void close() throws Exception {
        if (cursor != OPENED) {
            return;
        }
        try {
            if (dataReader != null) {
                dataReader.close();
            }
            cursor = CLOSED;
            queryCursor = -1;
        } catch (Exception e) {
            throw new DataFlowException(e.getMessage(), e);
        }
        
    }
    
    private DataReader generateDataReader() {
        DataReaderPredicate dataReaderPredicate = new DataReaderPredicate(
                predicate.getQueryList().get(queryCursor),
                predicate.getQueryStringList().get(queryCursor),
                predicate.getDataStore(),
                predicate.getAttributeList(),
                predicate.getLuceneAnalyzer());
        dataReaderPredicate.setIsSpanInformationAdded(true);
        DataReader dataReader = new DataReader(dataReaderPredicate);
        return dataReader;
    }  

    @Override
    public Schema getOutputSchema() {
        return outputSchema;
    }

}
