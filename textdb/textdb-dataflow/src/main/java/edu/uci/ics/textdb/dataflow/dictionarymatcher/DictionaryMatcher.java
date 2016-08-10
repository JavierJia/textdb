
package edu.uci.ics.textdb.dataflow.dictionarymatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.uci.ics.textdb.api.common.Attribute;
import edu.uci.ics.textdb.api.common.FieldType;
import edu.uci.ics.textdb.api.common.IPredicate;
import edu.uci.ics.textdb.api.common.ITuple;
import edu.uci.ics.textdb.api.common.Schema;
import edu.uci.ics.textdb.api.dataflow.IOperator;
import edu.uci.ics.textdb.common.constants.DataConstants;
import edu.uci.ics.textdb.common.constants.SchemaConstants;
import edu.uci.ics.textdb.common.constants.DataConstants.KeywordMatchingType;
import edu.uci.ics.textdb.common.exception.DataFlowException;
import edu.uci.ics.textdb.common.exception.ErrorMessages;
import edu.uci.ics.textdb.common.field.Span;
import edu.uci.ics.textdb.common.utils.Utils;
import edu.uci.ics.textdb.dataflow.common.DictionaryPredicate;
import edu.uci.ics.textdb.dataflow.common.KeywordPredicate;
import edu.uci.ics.textdb.dataflow.keywordmatch.KeywordMatcher;
import edu.uci.ics.textdb.dataflow.source.IndexBasedSourceOperator;
import edu.uci.ics.textdb.dataflow.source.MultiQueryIndexSourceOperator;
import edu.uci.ics.textdb.storage.DataReaderPredicate;

/**
 * @author Sudeep (inkudo)
 * @author Zuozhi Wang (zuozhi)
 * 
 */
public class DictionaryMatcher implements IOperator {

    private IOperator inputOperator;
    private KeywordMatcher keywordMatcher;

    private Schema inputSchema;
	private Schema outputSchema;
    
    private String currentDictionaryEntry;

    private final DictionaryPredicate predicate;

    /**
     * Constructs a DictionaryMatcher with a dictionary predicate
     * @param predicate
     * 
     */
    public DictionaryMatcher(IPredicate predicate) {
        this.predicate = (DictionaryPredicate) predicate;
    }
    

    /**
     * @about Opens dictionary matcher. Must call open() before calling getNextTuple().
     */
    @Override
    public void open() throws DataFlowException {
        if (this.inputOperator == null) {
            throw new DataFlowException(ErrorMessages.INPUT_OPERATOR_NOT_SPECIFIED);
        }
        try {            
            inputOperator.open();
            
            inputSchema = inputOperator.getOutputSchema();
            if (inputSchema.containsField(SchemaConstants.SPAN_LIST)) {
                outputSchema = inputSchema;
            } else {
                outputSchema = Utils.createSpanSchema(inputSchema);
            }

            
            this.keywordMatcher = generateNextKeywordMatcher();
            if (this.keywordMatcher == null) {
                throw new DataFlowException("Dictionary is empty");
            }
            keywordMatcher.open();

            
            if (inputOperator instanceof MultiQueryIndexSourceOperator) {
                // TODO: handles differently if inputOperator comes directly from index
            }

        } catch (Exception e) {
            throw new DataFlowException(e.getMessage(), e);
        }
    }

    /**
     * @about Gets the next matched tuple. <br>
     * 		  Returns the tuple with results in spanList. <br>
     * 
     * 		  Performs SCAN, KEYWORD_BASIC, or KEYWORD_PHRASE depends on the 
     * 		  dictionary predicate. <br> 
     * 
     *        DictionaryOperatorType.SCAN: <br>
     *        Scan the tuples using ScanSourceOperator. <br>
     *        For each tuple, loop through the dictionary 
     *        and find results. <br> 
     *        We assume the dictionary is smaller than the data at the 
     *        source operator, we treat the data source as the outer
     *        relation to reduce the number of disk IOs. <br>
     *        
     *        DictionaryOperatorType.KEYWORD_BASIC, KEYWORD_PHRASE: <br>
     *        Use KeywordMatcher to find results. <br>
     *        
     *        KEYWORD_BASIC corresponds to KeywordOperatorType.BASIC, which
     *        performs keyword search on the document. The input query is tokenized.
     *        The order of the tokens doesn't matter. <br>
     *        
     *        KEYWORD_PHRASE corresponds to KeywordOperatorType.PHRASE, which
     *        performs phrase search on the document. The input query is tokenized.
     *        The order of the tokens does matter. Stopwords are treated as placeholders 
     *        to indicate an arbitary token. <br>
     *        
     */
    @Override
    public ITuple getNextTuple() throws Exception {
        try {
            ITuple result = null;
            while (result == null) {
                if ((result = keywordMatcher.getNextTuple()) != null) {
                    break;
                }
                
                // If all results from current keywordMatcher are consumed, 
                // advance to next dictionary entry, and
                // generate a new KeywordMatcher
                keywordMatcher.close();
                keywordMatcher = generateNextKeywordMatcher();
                if (this.keywordMatcher == null) {
                    break;
                }
                keywordMatcher.open();
            }
            
            return result;
        } catch (Exception e) {
            throw new DataFlowException(e.getMessage(), e);
        }
        
    }
    
    private KeywordMatcher generateNextKeywordMatcher() throws DataFlowException {
        if ((this.currentDictionaryEntry = predicate.getNextDictionaryEntry()) == null) {
            return null;
        }
                
        KeywordPredicate keywordPredicate = new KeywordPredicate(
                currentDictionaryEntry,
                predicate.getAttributeList(), predicate.getAnalyzer(),
                predicate.getSourceOperatorType());
        
        KeywordMatcher keywordMatcher = new KeywordMatcher(keywordPredicate);
        keywordMatcher.setInputOperator(inputOperator);
        
        return keywordMatcher;
    }
    
    /**
     * @about Closes the operator
     */
    @Override
    public void close() throws DataFlowException {
        try {
        	if (inputOperator != null) {
                inputOperator.close();
        	}
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataFlowException(e.getMessage(), e);
        }
    }

    
    public IOperator getInputOperator() {
		return inputOperator;
	}

	public void setInputOperator(IOperator inputOperator) {
		this.inputOperator = inputOperator;
	}

    @Override
    public Schema getOutputSchema() {
        return outputSchema;
    }
}
