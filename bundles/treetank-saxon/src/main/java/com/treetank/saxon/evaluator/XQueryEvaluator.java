package com.treetank.saxon.evaluator;

import java.util.concurrent.Callable;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XQueryCompiler;
import net.sf.saxon.s9api.XQueryExecutable;
import net.sf.saxon.s9api.XdmValue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.treetank.api.IDatabase;
import com.treetank.saxon.wrapper.DocumentWrapper;
import com.treetank.saxon.wrapper.NodeWrapper;

/**
 * <h1>XQuery evaluator</h1>
 * 
 * <p>
 * Evaluates an XQuery expression against a Treetank storage and returns an
 * XdmValue instance, which corresponds to zero or more XdmItems.
 * </p>
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
public class XQueryEvaluator implements Callable<XdmValue> {

	/** Logger. */
	private static final Log LOGGER = LogFactory
			.getLog(com.treetank.saxon.evaluator.XQueryEvaluator.class);

	/** XQuery expression. */
	private transient final String mExpression;

	/** Treetank session. */
	private transient final IDatabase mDatabase;

	/**
	 * Constructor.
	 * 
	 * @param expression
	 *            XQuery expression.
	 * @param database
	 *            Treetank database.
	 * @param file
	 *            Target Treetank storage.
	 */
	public XQueryEvaluator(final String expression, final IDatabase database) {
		mExpression = expression;
		mDatabase = database;
	}

	@Override
	public XdmValue call() throws Exception {
		XdmValue value = null;

		try {
			final Processor proc = new Processor(false);
			final Configuration config = proc.getUnderlyingConfiguration();
			final NodeWrapper doc = (NodeWrapper) new DocumentWrapper(
					mDatabase, config).wrap();
			final XQueryCompiler comp = proc.newXQueryCompiler();
			final XQueryExecutable exp = comp.compile(mExpression);
			final net.sf.saxon.s9api.XQueryEvaluator exe = exp.load();
			exe.setSource(doc);
			value = exe.evaluate();
		} catch (final SaxonApiException e) {
			LOGGER.error("Saxon Exception: " + e.getMessage(), e);
			throw e;
		}

		return value;
	}
}