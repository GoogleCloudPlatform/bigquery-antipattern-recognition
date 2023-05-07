// Generated from com/google/zetasql/toolkit/catalog/typeparser/ZetaSQLTypeGrammar.g4 by ANTLR 4.12.0
package com.google.zetasql.toolkit.catalog.typeparser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link ZetaSQLTypeGrammarParser}.
 */
public interface ZetaSQLTypeGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link ZetaSQLTypeGrammarParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(ZetaSQLTypeGrammarParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetaSQLTypeGrammarParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(ZetaSQLTypeGrammarParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetaSQLTypeGrammarParser#basicType}.
	 * @param ctx the parse tree
	 */
	void enterBasicType(ZetaSQLTypeGrammarParser.BasicTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetaSQLTypeGrammarParser#basicType}.
	 * @param ctx the parse tree
	 */
	void exitBasicType(ZetaSQLTypeGrammarParser.BasicTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetaSQLTypeGrammarParser#arrayType}.
	 * @param ctx the parse tree
	 */
	void enterArrayType(ZetaSQLTypeGrammarParser.ArrayTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetaSQLTypeGrammarParser#arrayType}.
	 * @param ctx the parse tree
	 */
	void exitArrayType(ZetaSQLTypeGrammarParser.ArrayTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetaSQLTypeGrammarParser#structType}.
	 * @param ctx the parse tree
	 */
	void enterStructType(ZetaSQLTypeGrammarParser.StructTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetaSQLTypeGrammarParser#structType}.
	 * @param ctx the parse tree
	 */
	void exitStructType(ZetaSQLTypeGrammarParser.StructTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetaSQLTypeGrammarParser#structFields}.
	 * @param ctx the parse tree
	 */
	void enterStructFields(ZetaSQLTypeGrammarParser.StructFieldsContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetaSQLTypeGrammarParser#structFields}.
	 * @param ctx the parse tree
	 */
	void exitStructFields(ZetaSQLTypeGrammarParser.StructFieldsContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetaSQLTypeGrammarParser#structField}.
	 * @param ctx the parse tree
	 */
	void enterStructField(ZetaSQLTypeGrammarParser.StructFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetaSQLTypeGrammarParser#structField}.
	 * @param ctx the parse tree
	 */
	void exitStructField(ZetaSQLTypeGrammarParser.StructFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link ZetaSQLTypeGrammarParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(ZetaSQLTypeGrammarParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link ZetaSQLTypeGrammarParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(ZetaSQLTypeGrammarParser.TypeParametersContext ctx);
}