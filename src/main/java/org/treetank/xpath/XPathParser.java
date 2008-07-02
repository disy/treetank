
package org.treetank.xpath;

import java.util.NoSuchElementException;

import org.treetank.api.IAxis;
import org.treetank.api.IFilter;
import org.treetank.api.IItem;
import org.treetank.api.IReadTransaction;
import org.treetank.axislayer.AncestorAxis;
import org.treetank.axislayer.AttributeAxis;
import org.treetank.axislayer.AttributeFilter;
import org.treetank.axislayer.ChildAxis;
import org.treetank.axislayer.CommentFilter;
import org.treetank.axislayer.DescendantAxis;
import org.treetank.axislayer.DocumentRootNodeFilter;
import org.treetank.axislayer.ElementFilter;
import org.treetank.axislayer.FilterAxis;
import org.treetank.axislayer.FollowingAxis;
import org.treetank.axislayer.FollowingSiblingAxis;
import org.treetank.axislayer.NameFilter;
import org.treetank.axislayer.NodeFilter;
import org.treetank.axislayer.PIFilter;
import org.treetank.axislayer.ParentAxis;
import org.treetank.axislayer.PrecedingAxis;
import org.treetank.axislayer.PrecedingSiblingAxis;
import org.treetank.axislayer.SelfAxis;
import org.treetank.axislayer.TextFilter;
import org.treetank.utils.TypedValue;
import org.treetank.xpath.filter.DocumentNodeAxis;
import org.treetank.xpath.filter.ItemFilter;
import org.treetank.xpath.filter.NestedFilter;
import org.treetank.xpath.filter.SchemaAttributeFilter;
import org.treetank.xpath.filter.SchemaElementFilter;
import org.treetank.xpath.filter.TypeFilter;
import org.treetank.xpath.functions.XPathError;
import org.treetank.xpath.functions.XPathError.ErrorType;

/**
 * <h1>XPath Parser</h1>
 * <p>
 * Parses the given XPath query and starts the execution of the XPath request.
 * The given query is send to the scanner that categorizes the symbols by
 * creating tokens. The parser receives this tokens, checks the grammar
 * according to the EBNF given on <a
 * href="http://www.w3.org/TR/xquery-xpath-parsing/">
 * http://www.w3.org/TR/xquery-xpath-parsing/</a>.Then it constitutes the query
 * execution chain.
 * </p>
 * 
 * @author Tina Scherer
 */
public final class XPathParser implements XPathConstants {

  /** IReadTransaction to access the nodes. Is needed for filters and axes. */
  private final IReadTransaction mRTX;

  /** Scanner that scans the symbols of the query and returns them as tokens. */
  private final XPathScanner mScanner;

  /** Represents the current read token. */
  private XPathToken mToken;

  /**
   * Builds the chain of nested IAxis that evaluate the query in a pipeline
   * manner.
   */
  private final PipelineBuilder mPipeBuilder;

  /**
   * Constructor. Initializes the internal state.
   * 
   * @param rtx
   *          The transaction.
   * @param query
   *          The query to process.
   */
  public XPathParser(final IReadTransaction rtx, final String query) {

    mRTX = rtx;
    mScanner = new XPathScanner(query);
    mPipeBuilder = new PipelineBuilder();

  }

  /**
   * Starts parsing the query.
   */
  public final void parseQuery() {

    // get first token, ignore all white spaces
    do {
      mToken = mScanner.nextToken();
    } while (mToken.getType() == Token.SPACE);

    // parse the query according to the rules specified in the XPath 2.0 REC
    parseExpression();

    // after the parsing of the expression no token must be left
    if (mToken.getType() != Token.END) {
      throw new IllegalStateException(
          "The query  has not been processed completely.");
    }
  }

  /**
   * Parses an XPath expression according to the following EBNF production rule:
   * <p>
   * [2] Expr ::= ExprSingle ("," ExprSingle)* .
   * </p>
   */
  private final void parseExpression() {

    mPipeBuilder.addExpr();

    int no = 0;
    do {
      parseExprSingle();
      no++;

    } while (is(Token.COMMA, true));

    mPipeBuilder.finishExpr(getTransaction(), no);
  }

  /**
   * Parses the the rule ExprSingle according to the following production rule:
   * <p>
   * [3] ExprSingle ::= ForExpr | QuantifiedExpr | IfExpr | OrExpr .
   * </p>
   */
  private final void parseExprSingle() {

    mPipeBuilder.addExpressionSingle();

    final String tContent = mToken.getContent();
    if ("for".equals(tContent)) {
      parseForExpr();
    } else if ("some".equals(tContent) || "every".equals(tContent)) {
      parseQuantifiedExpr();
    } else if ("if".equals(tContent)) {
      parseIfExpr();
    } else {
      parseOrExpr();
    }
  }

  /**
   * Parses the the rule ForExpr according to the following production rule: [4]
   * <p>
   * ForExpr ::= SimpleForClause "return" ExprSingle .
   * </p>
   */
  private final void parseForExpr() {

    // get number of all for conditions
    final int rangeVarNo = parseSimpleForClause();
    consume("return", true);

    // parse return clause
    parseExprSingle();

    mPipeBuilder.addForExpression(rangeVarNo);

  }

  /**
   * Parses the the rule SimpleForClause according to the following production
   * rule:
   * <p>
   * [5] SimpleForClause ::= <"for" "$"> VarName "in" ExprSingle ("," "$"
   * VarName "in" ExprSingle)* .
   * </p>
   * 
   * @return returns the number of for-conditions
   */
  private int parseSimpleForClause() {

    consume("for", true);

    int forCondNo = 0;

    // parse all conditions and count them
    do {

      consume(Token.DOLLAR, true);

      // parse for variables
      final String varName = parseVarName();
      consume("in", true);

      parseExprSingle();
      mPipeBuilder.addVariableExpr(getTransaction(), varName);
      forCondNo++;

    } while (is(Token.COMMA, true));

    return forCondNo;

  }

  /**
   * Parses the the rule QuantifiedExpr according to the following production
   * rule:
   * <p>
   * [6] QuantifiedExpr ::= (<"some" "$"> | <"every" "$">) VarName "in"
   * ExprSingle ("," "$" VarName "in" ExprSingle)* "satisfies" ExprSingle .
   * </p>
   */
  private final void parseQuantifiedExpr() {

    // identify quantifier type
    final boolean isSome = is("some", true);
    if (!isSome) {
      consume("every", true);
    }

    // count number of variables
    int varNo = 0;

    do {

      // parse variable name
      consume(Token.DOLLAR, true);
      final String varName = parseVarName();
      consume("in", true);
      varNo++;

      parseExprSingle();
      mPipeBuilder.addVariableExpr(getTransaction(), varName);

    } while (is(Token.COMMA, true));

    // parse satisfies expression
    consume("satisfies", true);

    parseExprSingle();

    mPipeBuilder.addQuantifierExpr(getTransaction(), isSome, varNo);

  }

  /**
   * Parses the the rule IfExpr according to the following production rule:
   * <p>
   * [7] IfExpr ::= <"if" "("> Expr ")" "then" ExprSingle "else" ExprSingle.
   * </p>
   */
  private final void parseIfExpr() {

    // parse if expression
    consume("if", true);
    consume(Token.OPEN_BR, true);

    // parse test expression axis
    parseExpression();
    consume(Token.CLOSE_BR, true);

    // parse then expression
    consume("then", true);
    parseExprSingle();

    // parse else expression
    consume("else", true);
    parseExprSingle();

    mPipeBuilder.addIfExpression(getTransaction());

  }

  /**
   * Parses the the rule OrExpr according to the following production rule:
   * <p>
   * [8] OrExpr ::= AndExpr ( "or" AndExpr )* .
   * </p>
   */
  private final void parseOrExpr() {

    parseAndExpr();

    while (is("or", true)) {

      mPipeBuilder.addExpressionSingle();

      parseAndExpr();
      mPipeBuilder.addOrExpression(getTransaction());

    }
  }

  /**
   * Parses the the rule AndExpr according to the following production rule:
   * <p>
   * [9] AndExpr ::= ComparisonExpr ( "and" ComparisonExpr )* .
   * </p>
   */
  private final void parseAndExpr() {

    parseComparisionExpr();

    while (is("and", true)) {

      mPipeBuilder.addExpressionSingle();

      parseComparisionExpr();
      mPipeBuilder.addAndExpression(getTransaction());

    }
  }

  /**
   * Parses the the rule ComparisionExpr according to the following production
   * rule:
   * <p>
   * [10] ComparisonExpr ::= RangeExpr ( (ValueComp | GeneralComp | NodeComp)
   * RangeExpr )? .
   * </p>
   */
  private final void parseComparisionExpr() {

    parseRangeExpr();

    String comp = mToken.getContent();

    if (isComp()) {

      // parse second operator axis
      mPipeBuilder.addExpressionSingle();

      parseRangeExpr();

      mPipeBuilder.addCompExpression(getTransaction(), comp);
    }

  }

  /**
   * Indicates whether the current token is a comparison operator, or not. *
   * Parses the the rule NodeComp [24], ValueComp [23] and GeneralComp [22]
   * according to the following production rule:
   * <p>
   * [22] GeneralComp ::= * "=" | "!=" | "<" | "<=" | * ">" | ">=" .
   * </p>
   * <p>
   * [23] ValueComp ::= "eq" | "ne" | "lt" | "le" | "gt" | "ge" .
   * </p>
   * <p>
   * [24] NodeComp ::= "is" | "<<" | ">>" .
   * </p>
   * <p>
   * [22-24] Comp ::= "=" | "!=" | "<" | "<=" | ">" | ">=" |"eq" | "ne" | "lt" |
   * "le" | "gt" | "ge" | "is" | "<<" | ">>" .
   * </p>
   * 
   * @return true, if the current token is a comparison operator.
   */
  private final boolean isComp() {

    return (is(Token.L_SHIFT, true) || is(Token.R_SHIFT, true)
        || is(Token.EQ, true) || is(Token.N_EQ, true) || is(Token.COMP, true) || mToken
        .getType() == Token.TEXT
        && (is("ne", true) || is("eq", true) || is("lt", true)
            || is("le", true) || is("gt", true) || is("ge", true) || is("is",
            true)));
  }

  /**
   * Parses the the rule RangeExpr according to the following production rule:
   * <p>
   * [11] RangeExpr ::= AdditiveExpr ( "to" AdditiveExpr )? .
   * </p>
   */
  private final void parseRangeExpr() {

    parseAdditiveExpr();
    if (is("to", true)) {

      mPipeBuilder.addExpressionSingle();

      parseAdditiveExpr();
      mPipeBuilder.addRangeExpr(getTransaction());

    }
  }

  /**
   * Parses the the rule AdditiveExpr according to the following production
   * rule:
   * <p>
   * [12] AdditiveExpr ::= MultiplicativeExpr(("+" | "-") MultiplicativeExpr)* .
   * </p>
   */
  private void parseAdditiveExpr() {

    parseMultiplicativeExpr();

    String op = mToken.getContent();
    while (is(Token.PLUS, true) || is(Token.MINUS, true)) {

      // identify current operator kind
      // for (Operators op : Operators.values()) {
      // if (is(op.getOpName(), true)) {

      // parse second operand axis

      mPipeBuilder.addExpressionSingle();

      parseMultiplicativeExpr();

      mPipeBuilder.addOperatorExpression(getTransaction(), op);
      op = mToken.getContent();
    }
    // }
    // }
  }

  /**
   * Parses the the rule MultiplicativeExpr according to the following
   * production rule:
   * <p>
   * [13] MultiplicativeExpr ::= UnionExpr ( ("*" | "div" | "idiv" | "mod")
   * UnionExpr )* .
   * </p>
   */
  private void parseMultiplicativeExpr() {

    parseUnionExpr();

    String op = mToken.getContent();
    while (isMultiplication()) {

      // for (Operators op : Operators.values()) {
      // // identify current operator
      // if (is(op.getOpName(), true)) {

      mPipeBuilder.addExpressionSingle();

      // parse second operand axis
      parseUnionExpr();

      mPipeBuilder.addOperatorExpression(getTransaction(), op);
      op = mToken.getContent();
      // }
      // }
    }
  }

  /**
   * Parses the the rule UnionExpr according to the following production rule:
   * <p>
   * [14] UnionExpr ::= IntersectExceptExpr ( ("union" | "|")
   * IntersectExceptExpr )* .
   * </p>
   */
  private final void parseUnionExpr() {

    parseIntersectExceptExpr();

    while (is("union", true) || is(Token.OR, true)) {

      // parse second operand axis

      mPipeBuilder.addExpressionSingle();

      parseIntersectExceptExpr();

      mPipeBuilder.addUnionExpression(getTransaction());

    }
  }

  /**
   * Parses the the rule IntersectExceptExpr according to the following
   * production rule:
   * <p>
   * [15] IntersectExceptExpr ::= InstanceofExpr ( ("intersect" | * "except")
   * InstanceofExpr )* .
   * </p>
   */
  private final void parseIntersectExceptExpr() {

    parseInstanceOfExpr();

    boolean isIntersect = mToken.getContent().equals("intersect");

    while (is("intersect", true) || is("except", true)) {
      // parse second operand axis

      mPipeBuilder.addExpressionSingle();

      parseInstanceOfExpr();

      mPipeBuilder.addIntExcExpression(getTransaction(), isIntersect);

      isIntersect = mToken.getContent().equals("intersect");
    }

  }

  /**
   * Parses the the rule InstanceOfExpr according to the following production
   * rule:
   * <p>
   * [16] InstanceofExpr ::= TreatExpr ( <"instance" "of"> SequenceType )?.
   * </p>
   */
  private final void parseInstanceOfExpr() {

    parseTreatExpr();

    if (is("instance", true)) {
      consume("of", true);

      mPipeBuilder.addInstanceOfExpr(getTransaction(), parseSequenceType());
    }
  }

  /**
   * Parses the the rule TreatExpr according to the following production rule:
   * <p>
   * [17] TreatExpr ::= CastableExpr ( <"treat" "as"> SequenceType )? .
   * </p>
   */
  private final void parseTreatExpr() {

    parseCastableExpr();
    if (is("treat", true)) {
      consume("as", true);
      mPipeBuilder.addTreatExpr(getTransaction(), parseSequenceType());
    }
  }

  /**
   * Parses the the rule CastableExpr according to the following production
   * rule:
   * <p>
   * [18] CastableExpr ::= CastExpr ( <"castable" "as"> SingleType )? .
   * </p>
   */
  private final void parseCastableExpr() {

    parseCastExpr();
    if (is("castable", true)) {

      consume("as", true);
      mPipeBuilder.addCastableExpr(getTransaction(), parseSingleType());

    }
  }

  /**
   * Parses the the rule CastExpr according to the following production rule:
   * <p>
   * [19] CastExpr ::= UnaryExpr ( <"cast" "as"> SingleType )? .
   * </p>
   */
  private final void parseCastExpr() {

    parseUnaryExpr();
    if (is("cast", true)) {

      consume("as", true);
      mPipeBuilder.addCastExpr(getTransaction(), parseSingleType());
    }
  }

  /**
   * Parses the the rule UnaryExpr according to the following production rule:
   * <p>
   * [20] UnaryExpr ::= ("-" | "+")* ValueExpr .
   * </p>
   */
  private final void parseUnaryExpr() {

    boolean isUnaryMinus = false;

    // the plus can be ignored since it does not modify the result
    while ((is(Token.PLUS, true) || mToken.getType() == Token.MINUS)) {

      if (is(Token.MINUS, true)) {
        // two following minuses is a plus and therefore can be ignored,
        // thus only in case of an odd number of minus signs, the unary
        // operation
        // has to be processed
        isUnaryMinus = !isUnaryMinus;
      }
    }

    if (isUnaryMinus) {
      // unary minus has to be processed

      mPipeBuilder.addExpressionSingle();

      parseValueExpr();
      mPipeBuilder.addOperatorExpression(getTransaction(), "unary");

    } else {

      parseValueExpr();
    }
  }

  /**
   * Parses the the rule ValueExpr according to the following production rule:
   * <p>
   * [21] ValueExpr ::= PathExpr .
   * </p>
   */
  private final void parseValueExpr() {

    parsePathExpr();
  }

  /**
   * Parses the the rule PathExpr according to the following production rule:
   * <p>
   * [25] PathExpr ::= ("/" RelativePathExpr?) | ("//" RelativePathExpr) |
   * RelativePathExpr .
   * </p>
   */
  private final void parsePathExpr() {

    if (is(Token.SLASH, true)) {
      // path expression starts from the root
      mPipeBuilder.addStep(new DocumentNodeAxis(getTransaction()));
      final Token type = mToken.getType();

      if (type != Token.END && type != Token.COMMA) {
        // all immediately following keywords or '*' are nametests, not
        // operators
        // leading-lone-slash constrain

        parseRelativePathExpr();
      }

    } else if (is(Token.DESC_STEP, true)) {
      // path expression starts from the root with a descendant-or-self step
      mPipeBuilder.addStep(new DocumentNodeAxis(getTransaction()));

      IAxis axis = new DescendantAxis(getTransaction(), true);

      mPipeBuilder.addStep(axis);

      parseRelativePathExpr();
    } else {
      parseRelativePathExpr();
    }
  }

  /**
   * Parses the the rule RelativePathExpr according to the following production
   * rule:
   * <p>
   * [26] RelativePathExpr ::= StepExpr (("/" | "//") StepExpr)* .
   * </p>
   */
  private final void parseRelativePathExpr() {

    parseStepExpr();

    while (mToken.getType() == Token.SLASH
        || mToken.getType() == Token.DESC_STEP) {

      if (is(Token.DESC_STEP, true)) {
        IAxis axis = new DescendantAxis(getTransaction(), true);

        mPipeBuilder.addStep(axis);
      } else {
        // in this case the slash is just a separator
        consume(Token.SLASH, true);
      }
      parseStepExpr();
    }

  }

  /**
   * Parses the the rule StepExpr according to the following production rule:
   * <p>
   * [27] StepExpr ::= AxisStep | FilterExpr .
   * </p>
   */
  private final void parseStepExpr() {

    if (isFilterExpr()) {
      parseFilterExpr();
    } else {
      parseAxisStep();
    }
  }

  /**
   * @return true, if current token is part of a filter expression
   */
  private boolean isFilterExpr() {

    final Token type = mToken.getType();
    return (type == Token.DOLLAR || type == Token.POINT
        || type == Token.OPEN_BR || isFunctionCall() || isLiteral());
  }

  /**
   * The current token is part of a function call, if it is followed by a open
   * braces (current token is name of the function), or is followed by a colon
   * that is followed by a name an a open braces (current token is prefix of the
   * function name.).
   * 
   * @return true, if the current token is part of a function call
   */
  private boolean isFunctionCall() {

    return (mToken.getType() == Token.TEXT && (!isReservedKeyword()) && (mScanner
        .lookUpTokens(1).getType() == Token.OPEN_BR || (mScanner
        .lookUpTokens(1).getType() == Token.COLON && mScanner.lookUpTokens(3)
        .getType() == Token.OPEN_BR)));
  }

  /**
   * Although XPath is supposed to have no reserved words, some keywords are not
   * allowed as function names in an unprefixed form because expression syntax
   * takes precedence.
   * 
   * @return true if the token is one of the reserved words of XPath 2.0
   */
  private boolean isReservedKeyword() {

    final String content = mToken.getContent();
    return (isKindTest() || "item".equals(content) || "if".equals(content)
        || "empty-sequence".equals(content) || "typeswitch".equals(content));
  }

  /**
   * Parses the the rule AxisStep according to the following production rule:
   * <p>
   * [28] AxisStep ::= (ForwardStep | ReverseStep) PredicateList .
   * </p>
   */
  private final void parseAxisStep() {

    if (isReverceStep()) {
      parseReverceStep();
    } else {
      parseForwardStep();
    }
    parsePredicateList();

  }

  /**
   * Parses the the rule ForwardStep according to the following production rule:
   * <p>
   * [29] ForwardStep ::= (ForwardAxis NodeTest) | AbbrevForwardStep .
   * </p>
   */
  private final void parseForwardStep() {

    IAxis axis;
    IFilter filter;
    if (isForwardAxis()) {
      axis = parseForwardAxis();
      filter = parseNodeTest(axis.getClass() == AttributeAxis.class);

      mPipeBuilder.addStep(axis, filter);
    } else {
      axis = parseAbbrevForwardStep();

      mPipeBuilder.addStep(axis);
    }
  }

  /**
   * Parses the the rule ForwardAxis according to the following production rule:
   * <p>
   * [30] ForwardAxis ::= <"child" "::"> | <"descendant" "::"> | <"attribute"
   * "::"> | <"self" "::"> | <"descendant-or-self" "::"> | <"following-sibling"
   * "::"> | <"following" "::"> | <"namespace" "::"> .
   * </p>
   */
  private final IAxis parseForwardAxis() {

    final IAxis axis;
    if (is("child", true)) {

      axis = new ChildAxis(getTransaction());

    } else if (is("descendant", true)) {

      axis = new DescendantAxis(getTransaction());

    } else if (is("descendant-or-self", true)) {

      axis = new DescendantAxis(getTransaction(), true);

    } else if (is("attribute", true)) {
      axis = new AttributeAxis(getTransaction());

    } else if (is("self", true)) {
      axis = new SelfAxis(getTransaction());

    } else if (is("following", true)) {

      axis = new FollowingAxis(getTransaction());

    } else if (is("following-sibling", true)) {

      axis = new FollowingSiblingAxis(getTransaction());

    } else {
      is("namespace", true);
      throw new XPathError(ErrorType.XPST0010);

    }

    consume(Token.COLON, true);
    consume(Token.COLON, true);

    return axis;

  }

  /**
   * Checks if a given token represents a ForwardAxis.
   * 
   * @return true if the token is a ForwardAxis
   */
  private final boolean isForwardAxis() {

    final String content = mToken.getContent();
    return (mToken.getType() == Token.TEXT && ("child".equals(content) || ("descendant"
        .equals(content)
        || "descendant-or-self".equals(content)
        || "attribute".equals(content)
        || "self".equals(content)
        || "following".equals(content)
        || "following-sibling".equals(content) || "namespace".equals(content))));
  }

  /**
   * Parses the the rule AbrevForwardStep according to the following production
   * rule:
   * <p>
   * [31] AbbrevForwardStep ::= "@"? NodeTest .
   * </p>
   */
  private final IAxis parseAbbrevForwardStep() {

    IAxis axis;
    boolean isAttribute;

    if (is(Token.AT, true) || mToken.getContent().equals("attribute")
        || mToken.getContent().equals("schema-attribute")) {
      // in case of .../attribute(..), or .../schema-attribute() the default
      // axis
      // is the attribute axis
      axis = new AttributeAxis(getTransaction());
      isAttribute = true;
    } else {
      // default axis is the child axis
      axis = new ChildAxis(getTransaction());
      isAttribute = false;
    }

    final IFilter filter = parseNodeTest(isAttribute);

    return new FilterAxis(axis, filter);
  }

  /**
   * Parses the the rule ReverceStep according to the following production rule:
   * <p>
   * [32] ReverseStep ::= (ReverseAxis NodeTest) | AbbrevReverseStep .
   * </p>
   */
  private final void parseReverceStep() {

    IAxis axis;
    if (mToken.getType() == Token.PARENT) {
      axis = parseAbbrevReverseStep();

      mPipeBuilder.addStep(axis);
    } else {
      axis = parseReverceAxis();
      final IFilter filter = parseNodeTest(axis.getClass() == AttributeAxis.class);
      mPipeBuilder.addStep(axis, filter);
    }
  }

  /**
   * Parses the the rule ReverceAxis according to the following production rule:
   * [33] ReverseAxis ::= <"parent" "::"> | <"ancestor" "::"> |
   * <"preceding-sibling" "::">|<"preceding" "::">|<"ancestor-or-self" "::"> .
   */
  private final IAxis parseReverceAxis() {

    IAxis axis;
    if (is("parent", true)) {

      axis = new ParentAxis(getTransaction());

    } else if (is("ancestor", true)) {

      axis = new AncestorAxis(getTransaction());

    } else if (is("ancestor-or-self", true)) {

      axis = new AncestorAxis(getTransaction(), true);

    } else if (is("preceding", true)) {

      axis = new PrecedingAxis(getTransaction());

    } else {
      consume("preceding-sibling", true);

      axis = new PrecedingSiblingAxis(getTransaction());

    }

    consume(Token.COLON, true);
    consume(Token.COLON, true);

    return axis;
  }

  /**
   * Parses the the rule AbbrevReverceStep according to the following production
   * rule:
   * <p>
   * [34] AbbrevReverseStep ::= ".." .
   * </p>
   */
  private final IAxis parseAbbrevReverseStep() {

    consume(Token.PARENT, true);
    return new ParentAxis(getTransaction());
  }

  /**
   * @return true, if current token is part of an reverse axis step
   */
  private final boolean isReverceStep() {

    final Token type = mToken.getType();
    final String content = mToken.getContent();

    return (type == Token.PARENT || (type == Token.TEXT && ("parent"
        .equals(content)
        || "ancestor".equals(content)
        || "preceding".equals(content)
        || "preceding-sibling".equals(content) || "ancestor-or-self"
        .equals(content))));
  }

  /**
   * Parses the the rule NodeTest according to the following production rule:
   * <p>
   * [35] NodeTest ::= KindTest | NameTest .
   * </p>
   */
  private final IFilter parseNodeTest(final boolean isAtt) {

    IFilter filter;
    if (isKindTest()) {
      filter = parseKindTest();
    } else {
      filter = parseNameTest(isAtt);
    }
    return filter;
  }

  /**
   * Parses the the rule NameTest according to the following production rule:
   * <p>
   * [36] NameTest ::= QName | Wildcard .
   * </p>
   */
  private final IFilter parseNameTest(final boolean isAtt) {

    IFilter filter;
    if (isWildcardNameTest()) {

      filter = parseWildcard(isAtt);
    } else {
      filter = new NameFilter(getTransaction(), parseQName());
    }
    return filter;
  }

  /**
   * @return true, if has the structure of a name test containing a wildcard
   *         ("*" | < NCName ":" "*" > | < "*" ":" NCName >)
   */
  private boolean isWildcardNameTest() {

    return mToken.getType() == Token.STAR
        || (mToken.getType() == Token.TEXT
            && mScanner.lookUpTokens(1).getType() == Token.COLON && mScanner
            .lookUpTokens(2).getType() == Token.STAR);
  }

  /**
   * Parses the the rule Wildcard according to the following production rule:
   * <p>
   * [37] Wildcard ::= "*" | < NCName ":" "*" > | < "*" ":" NCName > .
   * <p>
   */
  private final IFilter parseWildcard(final boolean isAtt) {

    IFilter filter;

    if (is(Token.STAR, true)) {

      if (is(Token.COLON, true)) {
        parseNCName();
        throw new IllegalStateException("Wildcard in NS not supported yet");
      } else {
        if (isAtt) {
          filter = new AttributeFilter(getTransaction());
        } else {
          filter = new ElementFilter(getTransaction());
        }
      }
    } else {
      parseNCName();

      consume(Token.COLON, true);
      consume(Token.STAR, true);
      throw new IllegalStateException("Wildcard in NS not supported yet");
    }
    return filter;
  }

  /**
   * Parses the the rule FilterExpr according to the following production rule:
   * <p>
   * [38] FilterExpr ::= PrimaryExpr PredicateList .
   * </p>
   */
  private final void parseFilterExpr() {

    parsePrimaryExpr();
    parsePredicateList();
  }

  /**
   * Parses the the rule PredicateList according to the following production
   * rule:
   * <p>
   * [39] PredicateList ::= Predicate* .
   * </p>
   */
  private final void parsePredicateList() {

    while (mToken.getType() == Token.OPEN_SQP) {
      parsePredicate();
    }
  }

  /**
   * Parses the the rule Predicate according to the following production rule:
   * <p>
   * [40] Predicate ::= "[" Expr "]" .
   * </p>
   * <p>
   * The whole predicate expression is build as a separate expression chain and
   * is then inserted to the main expression chain by a predicate filter.
   * </p>
   */
  private final void parsePredicate() {

    consume(Token.OPEN_SQP, true);

    mPipeBuilder.addExpressionSingle();

    parseExpression();

    consume(Token.CLOSE_SQP, true);

    mPipeBuilder.addPredicate(getTransaction());
  }

  /**
   * Parses the the rule PrimaryExpr according to the following production rule:
   * <p>
   * [41] PrimaryExpr ::= Literal | VarRef | ParenthesizedExpr | ContextItemExpr |
   * FunctionCall .
   * </p>
   */
  private final void parsePrimaryExpr() {

    if (isLiteral()) {
      parseLiteral();
    } else if (mToken.getType() == Token.DOLLAR) {
      parseVarRef();
    } else if (mToken.getType() == Token.OPEN_BR) {
      parseParenthesizedExpr();
    } else if (mToken.getType() == Token.POINT) {
      parseContextItemExpr();
    } else if (!isReservedKeyword()) {
      parseFunctionCall();
    } else {
      throw new IllegalStateException("Found wrong token '"
          + mToken.getContent() + "'. "
          + " Token should be either a literal, a variable,"
          + "a '(', a '.' or a function call.");
    }
  }

  /**
   * @return true, if the current token represents a literal
   */
  private boolean isLiteral() {

    final Token type = mToken.getType();
    return (type == Token.SINGLE_QUOTE || type == Token.DBL_QUOTE || type == Token.VALUE);
  }

  /**
   * Parses the the rule Literal according to the following production rule:
   * <p>
   * [42] Literal ::= NumericLiteral | StringLiteral .
   * </p>
   */
  private final void parseLiteral() {

    int itemKey;

    if (mToken.getType() == Token.VALUE || mToken.getType() == Token.POINT) {
      // is numeric literal
      itemKey = parseNumericLiteral();
    } else {
      // is string literal
      assert (mToken.getType() == Token.DBL_QUOTE || mToken.getType() == Token.SINGLE_QUOTE);
      itemKey = parseStringLiteral();
    }

    mPipeBuilder.addLiteral(getTransaction(), itemKey);

  }

  /**
   * Parses the the rule NumericLiteral according to the following production
   * rule:
   * <p>
   * [43] NumericLiteral ::= IntegerLiteral | DecimalLiteral | DoubleLiteral .
   * </p>
   */
  private final int parseNumericLiteral() {

    return parseIntegerLiteral();

  }

  /**
   * Parses the the rule VarRef according to the following production rule:
   * <p>
   * [44] VarRef ::= "$" VarName .
   * </p>
   */
  private final void parseVarRef() {

    consume(Token.DOLLAR, true);
    final String varName = parseVarName();
    mPipeBuilder.addVarRefExpr(getTransaction(), varName);
  }

  /**
   * Parses the the rule PatenthesizedExpr according to the following production
   * rule:
   * <p>
   * [45] ParenthesizedExpr ::= "(" Expr? ")" .
   * </p>
   */
  private final void parseParenthesizedExpr() {

    consume(Token.OPEN_BR, true);
    if (!(mToken.getType() == Token.CLOSE_BR)) {
      parseExpression();
    }
    consume(Token.CLOSE_BR, true);

  }

  /**
   * Parses the the rule ContextItemExpr according to the following production
   * rule:
   * <p>
   * [46] ContextItemExpr ::= "." .
   * </p>
   */
  private final void parseContextItemExpr() {

    consume(Token.POINT, true);

    mPipeBuilder.addStep(new SelfAxis(getTransaction()));
  }

  /**
   * Parses the the rule FunctionCall according to the following production
   * rule:
   * <p>
   * [47] FunctionCall ::= < QName "(" > (ExprSingle ("," ExprSingle)*)? ")" .
   * </p>
   */
  private final void parseFunctionCall() {

    final String funcName = parseQName();

    consume(Token.OPEN_BR, true);

    int num = 0;
    if (!(mToken.getType() == Token.CLOSE_BR)) {

      do {

        parseExprSingle();
        num++;

      } while (is(Token.COMMA, true));
    }

    consume(Token.CLOSE_BR, true);
    mPipeBuilder.addFunction(getTransaction(), funcName, num);

  }

  /**
   * Parses the the rule SingleType according to the following production rule:
   * <p>
   * [48] SingleType ::= AtomicType "?"? .
   * </p>
   */
  private final SingleType parseSingleType() {

    final String atomicType = parseAtomicType();
    final boolean intero = is(Token.INTERROGATION, true);
    return new SingleType(atomicType, intero);
  }

  /**
   * Parses the the rule SequenceType according to the following production
   * rule: [
   * <p>
   * 49] SequenceType ::= (ItemType OccurrenceIndicator?) | <"void" "(" ")"> .
   * </p>
   */
  private final SequenceType parseSequenceType() {

    if (is("empty-sequence", true)) {
      consume(Token.OPEN_BR, true);
      consume(Token.CLOSE_BR, true);
      return new SequenceType();

    } else {
      final IFilter filter = parseItemType();
      if (isWildcard()) {
        final char wildcard = parseOccuranceIndicator();
        return new SequenceType(filter, wildcard);
      }
      return new SequenceType(filter);
    }
  }

  /**
   * @return true, if the current token is a '?', a '*' or a '+'.
   */
  private boolean isWildcard() {

    final Token type = mToken.getType();
    return (type == Token.STAR || type == Token.PLUS || type == Token.INTERROGATION);
  }

  /**
   * Parses the the rule OccuranceIndicator according to the following
   * production rule:
   * <p>
   * [50] OccurrenceIndicator ::= "?" | "*" | "+" .
   * </p>
   */
  private final char parseOccuranceIndicator() {

    char wildcard;

    if (is(Token.STAR, true)) {
      wildcard = '*';
    } else if (is(Token.PLUS, true)) {
      wildcard = '+';
    } else {
      consume(Token.INTERROGATION, true);
      wildcard = '?';

    }
    return wildcard;

  }

  /**
   * Parses the the rule ItemType according to the following production rule:
   * <p>
   * [51] ItemType ::= AtomicType | KindTest | <"item" "(" ")"> .
   * </p>
   */
  private final IFilter parseItemType() {

    IFilter filter;
    if (isKindTest()) {
      filter = parseKindTest();
    } else if (is("item", true)) {
      consume(Token.OPEN_BR, true);
      consume(Token.CLOSE_BR, true);

      filter = new ItemFilter(getTransaction());
    } else {
      final String atomic = parseAtomicType();
      filter = new TypeFilter(getTransaction(), atomic);
    }
    return filter;
  }

  /**
   * Parses the the rule AtomicTypr according to the following production rule:
   * <p>
   * [52] AtomicType ::= QName .
   * </p>
   */
  private final String parseAtomicType() {

    return parseQName();
  }

  /**
   * Parses the the rule KindTest according to the following production rule:
   * <p>
   * [53] KindTest ::= DocumentTest | ElementTest | AttributeTest |
   * SchemaElementTest | SchemaAttributeTest | PITest | CommentTest | TextTest |
   * AnyKindTest .
   * </p>
   */
  private final IFilter parseKindTest() {

    IFilter filter;
    final String test = mToken.getContent();

    if ("document-node".equals(test)) {
      filter = parseDocumentTest();
    } else if ("element".equals(test)) {
      filter = parseElementTest();
    } else if ("attribute".equals(test)) {
      filter = parseAttributeTest();
    } else if ("schema-element".equals(test)) {
      filter = parseSchemaElementTest();
    } else if ("schema-attribute".equals(test)) {
      filter = parseSchemaAttributeTest();
    } else if ("processing-instruction".equals(test)) {
      filter = parsePITest();
    } else if ("comment".equals(test)) {
      filter = parseCommentTest();
    } else if ("text".equals(test)) {
      filter = parseTextTest();
    } else {
      filter = parseAnyKindTest();
    }
    return filter;
  }

  /**
   * Parses the the rule AnyKindTest according to the following production rule:
   * <p>
   * [54] AnyKindTest ::= <"node" "("> ")" .
   * <p>
   */
  private final IFilter parseAnyKindTest() {

    consume("node", true);
    consume(Token.OPEN_BR, true);
    consume(Token.CLOSE_BR, true);

    return new NodeFilter(getTransaction());
  }

  /**
   * Checks if a given token represents a kind test.
   * 
   * @return true, if the token is a kind test
   */
  private final boolean isKindTest() {

    final String content = mToken.getContent();
    //lookahead is necessary, in a of e.g. a node filter, that filters nodes 
    //with a name like text:bla or node:bla, where text and node are the 
    //namespace prefixes.
    return (("node".equals(content) || "attribute".equals(content)
        || "schema-attribute".equals(content)
        || "schema-element".equals(content) || "element".equals(content)
        || "text".equals(content) || "comment".equals(content)
        || "document-node".equals(content) || "processing-instruction"
        .equals(content)) && mScanner.lookUpTokens(1).getType() == Token.OPEN_BR);
  }

  /**
   * Parses the the rule DocumentTest according to the following production
   * rule:
   * <p>
   * [55] DocumentTest ::= <"document-node" "("> (ElementTest |
   * SchemaElementTest)? ")" .
   * <p>
   */
  private final IFilter parseDocumentTest() {

    consume("document-node", true);
    consume(Token.OPEN_BR, true);
    IFilter filter = new DocumentRootNodeFilter(getTransaction());

    IFilter innerFilter;
    if (mToken.getContent().equals("element")) {
      innerFilter = parseElementTest();
      filter = new NestedFilter(getTransaction(), filter, innerFilter);
    } else if (mToken.getContent().equals("schema-element")) {
      innerFilter = parseSchemaElementTest();
      filter = new NestedFilter(getTransaction(), filter, innerFilter);
    }

    consume(Token.CLOSE_BR, true);

    return filter;
  }

  /**
   * Parses the the rule TextTest according to the following production rule:
   * <p>
   * [56] TextTest ::= <"text" "("> ")" .
   * </p>
   */
  private final IFilter parseTextTest() {

    consume("text", true);
    consume(Token.OPEN_BR, true);
    consume(Token.CLOSE_BR, true);

    return new TextFilter(getTransaction());
  }

  /**
   * Parses the the rule CommentTest according to the following production rule:
   * <p>
   * [57] CommentTest ::= <"comment" "("> ")" .
   * </p>
   */
  private final IFilter parseCommentTest() {

    consume("comment", true);
    consume(Token.OPEN_BR, true);
    consume(Token.CLOSE_BR, true);

    return new CommentFilter(getTransaction());
  }

  /**
   * Parses the the rule PITest according to the following production rule:
   * <p>
   * [58] PITest ::= <"processing-instruction" "("> (NCName | StringLiteral)?
   * ")" .
   * </p>
   */
  private final IFilter parsePITest() {

    consume("processing-instruction", true);
    consume(Token.OPEN_BR, true);

    IFilter filter = new PIFilter(getTransaction());

    if (!is(Token.CLOSE_BR, true)) {
      String stringLiteral;
      if (isQuote()) {
        stringLiteral = (getTransaction().getItemList().getItem(
            parseStringLiteral()).getRawValue().toString());
      } else {
        stringLiteral = parseNCName();
      }

      consume(Token.CLOSE_BR, true);

      filter = new NestedFilter(getTransaction(), filter,
          (IFilter) new NameFilter(getTransaction(), stringLiteral));
    }

    return filter;
  }

  /**
   * @return true, if token is a ' or a "
   */
  private boolean isQuote() {

    final Token type = mToken.getType();
    return (type == Token.SINGLE_QUOTE || type == Token.DBL_QUOTE);
  }

  /**
   * Parses the the rule AttributeTest according to the following production
   * rule:
   * <p>
   * [59] AttributeTest ::= <"attribute" "("> (AttribNameOrWildcard (","
   * TypeName)?)? ")" .
   * </p>
   */
  private final IFilter parseAttributeTest() {

    consume("attribute", true);
    consume(Token.OPEN_BR, true);

    IFilter filter = new AttributeFilter(getTransaction());

    if (!(mToken.getType() == Token.CLOSE_BR)) {
      // add name filter
      final String name = parseAttributeNameOrWildcard();
      if (!name.equals("*")) {
        filter = new NestedFilter(getTransaction(), filter, new NameFilter(
            getTransaction(), name));
      } // if it is '*', all attributes are accepted, so the normal attribute
      // filter is sufficient

      if (is(Token.COMMA, true)) {
        // add type filter
        filter = new NestedFilter(getTransaction(), filter, new TypeFilter(
            getTransaction(), parseTypeName()));
      }
    }

    consume(Token.CLOSE_BR, true);

    return filter;
  }

  /**
   * Parses the the rule AttributeOrWildcard according to the following
   * production rule:
   * <p>
   * [60] AttribNameOrWildcard ::= AttributeName | "*" .
   * </p>
   */
  private final String parseAttributeNameOrWildcard() {

    String name;

    if (is(Token.STAR, true)) {
      name = mToken.getContent();
    } else {
      name = parseAttributeName();
    }

    return name;
  }

  /**
   * Parses the the rule SchemaAttributeTest according to the following
   * production rule:
   * <p>
   * [61] SchemaAttributeTest ::= <"schema-attribute" "("> AttributeDeclaration
   * ")" .
   * </p>
   */
  private final IFilter parseSchemaAttributeTest() {

    consume("schema-attribute", true);
    consume(Token.OPEN_BR, true);

    final IFilter filter = new SchemaAttributeFilter(getTransaction(),
        parseAttributeDeclaration());

    consume(Token.CLOSE_BR, true);

    return filter;
  }

  /**
   * Parses the the rule AttributeDeclaration according to the following
   * production rule:
   * <p>
   * [62] AttributeDeclaration ::= AttributeName .
   * </p>
   */
  private final String parseAttributeDeclaration() {

    return parseAttributeName();
  }

  /**
   * Parses the the rule ElementTest according to the following production rule:
   * <p>
   * [63] ElementTest ::= <"element" "("> (ElementNameOrWildcard ("," TypeName
   * "?"?)?)? ")" .
   * </p>
   */
  private final IFilter parseElementTest() {

    consume("element", true);
    consume(Token.OPEN_BR, true);

    IFilter filter = new ElementFilter(getTransaction());

    if (!(mToken.getType() == Token.CLOSE_BR)) {

      String name = parseElementNameOrWildcard();
      if (!name.equals("*")) {
        filter = new NestedFilter(getTransaction(), filter, new NameFilter(
            getTransaction(), name));
      } // if it is '*', all elements are accepted, so the normal element
      // filter is sufficient

      if (is(Token.COMMA, true)) {

        filter = new NestedFilter(getTransaction(), filter,
            (IFilter) new TypeFilter(getTransaction(), parseTypeName()));

        if (is(Token.INTERROGATION, true)) {
          // TODO: Nilled property of node can be true or false. Without, must
          // be false
          throw new NoSuchElementException("'?' is not supported yet.");
        }
      }
    }

    consume(Token.CLOSE_BR, true);

    return filter;
  }

  /**
   * Parses the the rule ElementNameOrWildcard according to the following
   * production rule:
   * <p>
   * [64] ElementNameOrWildcard ::= ElementName | "*" .
   * </p>
   */
  private String parseElementNameOrWildcard() {

    String name;

    if (is(Token.STAR, true)) {
      name = mToken.getContent();
    } else {
      name = parseElementName();
    }

    return name;
  }

  /**
   * Parses the the rule SchemaElementTest according to the following production
   * rule:
   * <p>
   * [65] SchemaElementTest ::= <"schema-element" "("> ElementDeclaration ")" .
   * </p>
   */
  private final IFilter parseSchemaElementTest() {

    consume("schema-element", true);
    consume(Token.OPEN_BR, true);

    String elementDec = parseElementDeclaration();

    consume(Token.CLOSE_BR, true);

    return new SchemaElementFilter(getTransaction(), elementDec);
  }

  /**
   * Parses the the rule ElementDeclaration according to the following
   * production rule:
   * <p>
   * [66] ElementDeclaration ::= ElementName .
   * </p>
   */
  private final String parseElementDeclaration() {

    return parseElementName();
  }

  /**
   * Parses the the rule AttributeName according to the following production
   * rule:
   * <p>
   * [67] AttributeName ::= QName .
   * </p>
   */
  private final String parseAttributeName() {

    return parseQName();
  }

  /**
   * Parses the the rule ElementName according to the following production rule:
   * <p>
   * [68] ElementName ::= QName .
   * </p>
   */
  private final String parseElementName() {

    return parseQName();
  }

  /**
   * Parses the the rule TypeName according to the following production rule:
   * <p>
   * [69] TypeName ::= QName .
   * </p>
   */
  private final String parseTypeName() {

    return parseQName();
  }

  /**
   * Parses the the rule IntegerLiteral according to the following production
   * rule:
   * <p>
   * [70] IntegerLiteral ::= Digits => IntergerLiteral : Token.Value .
   * </p>
   */
  private final int parseIntegerLiteral() {

    String value = mToken.getContent();
    String type = "xs:integer";

    if (is(Token.VALUE, false)) {

      // is at least decimal literal (could also be a double literal)
      if (mToken.getType() == Token.POINT) {
        // TODO: not so nice, try to find a better solution
        boolean isDouble = mScanner.lookUpTokens(2).getType() == Token.E_NUMBER;
        value = parseDecimalLiteral(value);
        type = isDouble ? "xs:double" : "xs:decimal";
      }

      // values containing an 'e' are double literals
      if (mToken.getType() == Token.E_NUMBER) {
        value = parseDoubleLiteral(value);
        type = "xs:double";
      }

    } else {
      // decimal literal that starts with a "."
      boolean isDouble = mScanner.lookUpTokens(2).getType() == Token.E_NUMBER;
      value = parseDecimalLiteral("");
      type = isDouble ? "xs:double" : "xs:decimal";
    }

    is(Token.SPACE, true);

    IItem intLiteral = new AtomicValue(TypedValue.getBytes(value),
        getTransaction().keyForName(type));
    return getTransaction().getItemList().addItem(intLiteral);
  }

  /**
   * Parses the the rule DecimalLiteral according to the following production
   * rule:
   * <p>
   * [71] DecimalLiteral ::= ("." Digits) | (Digits "." [0-9]*) =>
   * DecimalLiteral : ("." Token.VALUE) | (Token.VALUE "." Token.VALUE?) .
   * </p>
   */
  private final String parseDecimalLiteral(final String value) {

    consume(Token.POINT, false);
    String dValue = value;

    if (mToken.getType() == Token.VALUE) {
      dValue = value + "." + mToken.getContent();
      consume(Token.VALUE, false);

      if (mToken.getType() == Token.E_NUMBER) {
        // TODO: set type to double
        dValue = parseDoubleLiteral(dValue);
      }
    }

    return dValue;
  }

  /**
   * Parses the the rule DoubleLiteral according to the following production
   * rule:
   * <p>
   * [72] DoubleLiteral ::= (("." Digits) | (Digits ("." [0-9]*)?)) [eE] [+-]?
   * Digits . DoubleLiteral : (("." Token.VALUE) | (Token.VALUE ("."
   * Token.VALUE?)?)) ("e" | "E") [+-]? Token.VALUE .
   * </p>
   * 
   * @param integerLiteral
   */
  private final String parseDoubleLiteral(final String value) {

    StringBuilder dValue = new StringBuilder(value);

    if (is(Token.E_NUMBER, false)) {
      dValue.append("E");
      dValue.append(mToken.getContent());
    }

    if (is(Token.PLUS, false) || is(Token.MINUS, false)) {
      dValue.append(mToken.getContent());
    }

    consume(Token.VALUE, true);

    return dValue.toString();
  }

  /**
   * Parses the the rule StringLiteral according to the following production
   * rule:
   * <p>
   * [73] StringLiteral ::= ('"' (('"' '"') | [^"])* '"') | ("'" (("'" "'") |
   * [^'])* "'" .
   * </p>
   */
  private final int parseStringLiteral() {

    StringBuilder value = new StringBuilder();

    if (is(Token.DBL_QUOTE, true)) {

      do {

        while (mToken.getType() != Token.DBL_QUOTE) {
          value.append(mToken.getContent());
          // consume(Token.TEXT, false); // TODO: does not need to be a text
          // could also be a value
          mToken = mScanner.nextToken();
        }

        consume(Token.DBL_QUOTE, true);
      } while (is(Token.DBL_QUOTE, true));

    } else {

      consume(Token.SINGLE_QUOTE, true);

      do {

        while (mToken.getType() != Token.SINGLE_QUOTE) {
          value.append(mToken.getContent());
          // consume(Token.TEXT, false);
          mToken = mScanner.nextToken();
        }

        consume(Token.SINGLE_QUOTE, true);
      } while (is(Token.SINGLE_QUOTE, true));

    }

    IItem stringLiteral = new AtomicValue(
        TypedValue.getBytes(value.toString()), getTransaction().keyForName(
            "xs:string"));
    return (getTransaction().getItemList().addItem(stringLiteral));
  }

  /**
   * Parses the the rule VarName according to the following production rule:
   * <p>
   * [74] VarName ::= QName .
   * </p>
   * 
   * @return string representation of variable name.
   */
  private final String parseVarName() {

    return parseQName();
  }

  /**
   * Specifies whether the current token is a multiplication operator.
   * Multiplication operators are: '*', 'div�, 'idiv' and 'mod'.
   * 
   * @return true, if current token is a multiplication operator
   */
  private final boolean isMultiplication() {

    return (is(Token.STAR, true) || is("div", true) || is("idiv", true) || is(
        "mod", true));

  }

  /**
   * Parses the the rule QName according to the following production rule:
   * http://www.w3.org/TR/REC-xml-names
   * <p>
   * [7] QName ::= PrefixedName | UnprefixedName
   * </p>
   * <p>
   * [8] PrefixedName ::= Prefix ':' LocalPart
   * </p>
   * <p>
   * [9] UnprefixedName ::= LocalPart
   * </p>
   * <p>
   * [10] Prefix ::= NCName
   * </p>
   * <p>
   * [11] LocalPart ::= NCName => QName ::= (NCName ":" )? NCName .
   * </p>
   * 
   * @return string representation of QName
   */
  private final String parseQName() {

    String qName = parseNCName(); // can be prefix or localPartName

    if (is(Token.COLON, false)) {

      qName += ":";
      qName += parseNCName(); // is localPartName
    }

    return qName;
  }

  /**
   * Parses the the rule NCName according to the following production rule:
   * http://www.w3.org/TR/REC-xml-names
   * <p>
   * [4] NCName ::= NCNameStartChar NCNameChar*
   * </p>
   * <p>
   * [5] NCNameChar ::= NameChar - ':'
   * </p>
   * <p>
   * [6] NCNameStartChar ::= Letter | '_' => NCName : Token.TEXT .
   * </p>
   * 
   * @return string representation of NCName
   */
  private final String parseNCName() {

    String ncName = mToken.getContent();

    consume(Token.TEXT, true);

    return ncName;
  }

  /**
   * Consumes a token. Tests if it really has the expected type and if not
   * returns an error message. Otherwise gets a new token from the scanner. If
   * that new token is of type whitespace and the ignoreWhitespace parameter is
   * true, a new token is retrieved, until the current token is not of type
   * whitespace.
   * 
   * @param type
   *          the specified token type
   * @param ignoreWhitespace
   *          if true all new tokens with type whitespace are ignored and the
   *          next token is retrieved from the scanner
   */
  private final void consume(final Token type, final boolean ignoreWhitespace) {

    if (!is(type, ignoreWhitespace)) {
      // error found by parser - stopping
      throw new IllegalStateException("Wrong token after " + mScanner.begin()
          + " at position " + mScanner.getPos() + " found " + mToken.getType()
          + " expected " + type + ".");
    }
  }

  /**
   * Consumes a token. Tests if it really has the expected name and if not
   * returns an error message. Otherwise gets a new token from the scanner. If
   * that new token is of type whitespace and the ignoreWhitespace parameter is
   * true, a new token is retrieved, until the current token is not of type
   * whitespace.
   * 
   * @param name
   *          the specified token content
   * @param ignoreWhitespace
   *          if true all new tokens with type whitespace are ignored and the
   *          next token is retrieved from the scanner
   */
  private final void consume(final String name, final boolean ignoreWhitespace) {

    if (!is(name, ignoreWhitespace)) {
      // error found by parser - stopping
      throw new IllegalStateException("Wrong token after " + mScanner.begin()
          + " found " + mToken.getContent() + ". Expected " + name);
    }
  }

  /**
   * Returns true or false if a token has the expected name. If the token has
   * the given name, it gets a new token from the scanner. If that new token is
   * of type whitespace and the ignoreWhitespace parameter is true, a new token
   * is retrieved, until the current token is not of type whitespace.
   * 
   * @param name
   *          the specified token content
   * @param ignoreWhitespace
   *          if true all new tokens with type whitespace are ignored and the
   *          next token is retrieved from the scanner
   */
  private final boolean is(final String name, final boolean ignoreWhitespace) {

    if (!name.equals(mToken.getContent())) {
      return false;
    }

    if (mToken.getType() == Token.COMP || mToken.getType() == Token.EQ
        || mToken.getType() == Token.N_EQ || mToken.getType() == Token.PLUS
        || mToken.getType() == Token.MINUS || mToken.getType() == Token.STAR) {
      return is(mToken.getType(), ignoreWhitespace);
    } else {
      return is(Token.TEXT, ignoreWhitespace);
    }
  }

  /**
   * Returns true or false if a token has the expected type. If so, a new token
   * is retrieved from the scanner. If that new token is of type whitespace and
   * the ignoreWhitespace parameter is true, a new token is retrieved, until the
   * current token is not of type whitespace.
   * 
   * @param name
   *          the specified token content
   * @param ignoreWhitespace
   *          if true all new tokens with type whitespace are ignored and the
   *          next token is retrieved from the scanner
   */
  private final boolean is(final Token type, final boolean ignoreWhitespace) {

    if (type != mToken.getType()) {

      return false;
    }

    do {
      // scan next token
      mToken = mScanner.nextToken();
    } while (ignoreWhitespace && mToken.getType() == Token.SPACE);

    return true;
  }

  /**
   * Returns a queue containing all pipelines (chains of nested axis and
   * filters) to execute the query.
   * 
   * @return the query pipelines.
   */
  public final IAxis getQueryPipeline() {

    return mPipeBuilder.getPipeline();
  }

  /**
   * Returns the read transaction.
   * 
   * @return the current transaction.
   */
  private final IReadTransaction getTransaction() {

    return mRTX;
  }
}
