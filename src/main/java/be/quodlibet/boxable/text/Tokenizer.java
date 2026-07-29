package be.quodlibet.boxable.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public final class Tokenizer {

	private static final Token OPEN_TAG_I = new Token(TokenType.OPEN_TAG, "i");
	private static final Token OPEN_TAG_B = new Token(TokenType.OPEN_TAG, "b");
	private static final Token OPEN_TAG_OL = new Token(TokenType.OPEN_TAG, "ol");
	private static final Token OPEN_TAG_UL = new Token(TokenType.OPEN_TAG, "ul");
	/**
	 * {@code <ul type="none">}: a list that keeps the indentation of its items but draws no
	 * bullet. Useful for a single explanatory paragraph that should sit indented under its
	 * heading without reading as a one-item enumeration.
	 */
	private static final Token OPEN_TAG_UL_NONE = new Token(TokenType.OPEN_TAG, "ul-none");
	private static final String TAG_CODE_OPEN = "<code>";
	private static final String TAG_CODE_CLOSE = "</code>";
	/** {@code <code>…</code>}: a span set in the monospaced face. */
	private static final Token OPEN_TAG_CODE = new Token(TokenType.OPEN_TAG, "code");
	private static final Token CLOSE_TAG_CODE = new Token(TokenType.CLOSE_TAG, "code");
	private static final Token CLOSE_TAG_I = new Token(TokenType.CLOSE_TAG, "i");
	private static final Token CLOSE_TAG_B = new Token(TokenType.CLOSE_TAG, "b");
	private static final Token CLOSE_TAG_OL = new Token(TokenType.CLOSE_TAG, "ol");
	private static final Token CLOSE_TAG_UL = new Token(TokenType.CLOSE_TAG, "ul");
	private static final Token CLOSE_TAG_P = new Token(TokenType.CLOSE_TAG, "p");
	private static final Token CLOSE_TAG_LI = new Token(TokenType.CLOSE_TAG, "li");
	private static final Token POSSIBLE_WRAP_POINT = new Token(TokenType.POSSIBLE_WRAP_POINT, "");
	private static final Token WRAP_POINT_P = new Token(TokenType.WRAP_POINT, "p");
	private static final Token WRAP_POINT_LI = new Token(TokenType.WRAP_POINT, "li");
	private static final Token WRAP_POINT_BR = new Token(TokenType.WRAP_POINT, "br");

	private Tokenizer() {
	}

	/**
	 * Tells whether a {@code <ul …>} start tag asks for no bullets, i.e. carries
	 * {@code type="none"} (quotes optional, any case).
	 */
	private static boolean isBulletless(final String tag) {
		final String lower = tag.toLowerCase();
		final int t = lower.indexOf("type");
		if (t < 0) {
			return false;
		}
		int i = lower.indexOf('=', t);
		if (i < 0) {
			return false;
		}
		i++;
		while (i < lower.length() && (lower.charAt(i) == ' ' || lower.charAt(i) == '"' || lower.charAt(i) == '\'')) {
			i++;
		}
		return lower.startsWith("none", i);
	}

	/** Extracts the href value from an anchor start tag like {@code <a href="url">}. */
	private static String extractHref(final String tag) {
		final int h = tag.indexOf("href");
		if (h < 0) {
			return null;
		}
		int i = tag.indexOf('=', h);
		if (i < 0) {
			return null;
		}
		i++;
		while (i < tag.length() && tag.charAt(i) == ' ') {
			i++;
		}
		if (i >= tag.length()) {
			return null;
		}
		final char quote = tag.charAt(i);
		if (quote == '"' || quote == '\'') {
			final int end = tag.indexOf(quote, i + 1);
			return end > 0 ? tag.substring(i + 1, end) : null;
		}
		int end = i;
		while (end < tag.length() && tag.charAt(end) != ' ' && tag.charAt(end) != '>') {
			end++;
		}
		return tag.substring(i, end);
	}

	private static boolean isWrapPointChar(char ch) {
		return
				ch == ' '  ||
				ch == ','  ||
				ch == '.'  ||
				ch == '-'  ||
				ch == '@'  ||
				ch == ':'  ||
				ch == ';'  ||
				ch == '\n' ||
				ch == '\t' ||
				ch == '\r' ||
				ch == '\f' ||
				ch == '\u000B';
	}

	private static Stack<Integer> findWrapPoints(String text) {
		Stack<Integer> result = new Stack<>();
		result.push(text.length());
		for (int i = text.length() - 2; i >= 0; i--) {
			if (isWrapPointChar(text.charAt(i))) {
				result.push(i + 1);
			}
		}
		return result;
	}

	private static Stack<Integer> findWrapPointsWithFunction(String text, WrappingFunction wrappingFunction) {
		final String[] split = wrappingFunction.getLines(text);
		int textIndex = text.length();
		final Stack<Integer> possibleWrapPoints = new Stack<>();
		possibleWrapPoints.push(textIndex);
		for (int i = split.length - 1; i > 0; i--) {
			final int splitLength = split[i].length();
			possibleWrapPoints.push(textIndex - splitLength);
			textIndex -= splitLength;
		}
		return possibleWrapPoints;
	}

	public static List<Token> tokenize(final String text, final WrappingFunction wrappingFunction) {
		final List<Token> tokens = new ArrayList<>();
		if (text != null) {
			final Stack<Integer> possibleWrapPoints = wrappingFunction == null
					? findWrapPoints(text)
					: findWrapPointsWithFunction(text, wrappingFunction);
			int textIndex = 0;
			final StringBuilder sb = new StringBuilder();
			// taking first wrap point
			Integer currentWrapPoint = possibleWrapPoints.pop();
			while (textIndex < text.length()) {
				// Wrap points are absolute indices into the original text, so consuming a tag can
				// jump textIndex past one of them — an attribute such as type="none" contains a
				// space, and a space is itself a wrap point. Drop every point we have already moved
				// beyond, otherwise the pointer stalls behind us and no further wrap point is ever
				// emitted, leaving the rest of the text as one unbreakable run.
				while (currentWrapPoint != null && textIndex >= currentWrapPoint) {
					if (textIndex == currentWrapPoint) {
						if (sb.length() > 0) {
							tokens.add(Token.text(TokenType.TEXT, sb.toString()));
							sb.delete(0, sb.length());
						}
						tokens.add(POSSIBLE_WRAP_POINT);
					}
					currentWrapPoint = possibleWrapPoints.isEmpty() ? null : possibleWrapPoints.pop();
				}
				final char c = text.charAt(textIndex);
				switch (c) {
				case '<':
					boolean consumed = false;
					// <code> / </code>: a monospaced span. Checked before the single-letter tags
					// below because those only ever look one or two characters ahead.
					if (text.startsWith(TAG_CODE_OPEN, textIndex) || text.startsWith(TAG_CODE_CLOSE, textIndex)) {
						final boolean closing = text.charAt(textIndex + 1) == '/';
						if (sb.length() > 0) {
							tokens.add(Token.text(TokenType.TEXT, sb.toString()));
							sb.delete(0, sb.length());
						}
						tokens.add(closing ? CLOSE_TAG_CODE : OPEN_TAG_CODE);
						textIndex += (closing ? TAG_CODE_CLOSE.length() : TAG_CODE_OPEN.length()) - 1;
						consumed = true;
					}
					if (!consumed && textIndex < text.length() - 2) {
						final char lookahead1 = text.charAt(textIndex + 1);
						final char lookahead2 = text.charAt(textIndex + 2);
						if ('i' == lookahead1 && '>' == lookahead2) {
							// <i>
							if (sb.length() > 0) {
								tokens.add(Token.text(TokenType.TEXT, sb.toString()));
								// clean string builder
								sb.delete(0, sb.length());
							}
							tokens.add(OPEN_TAG_I);
							textIndex += 2;
							consumed = true;
						} else if ('b' == lookahead1 && '>' == lookahead2) {
							// <b>
							if (sb.length() > 0) {
								tokens.add(Token.text(TokenType.TEXT, sb.toString()));
								// clean string builder
								sb.delete(0, sb.length());
							}
							tokens.add(OPEN_TAG_B);
							textIndex += 2;
							consumed = true;
						} else if ('b' == lookahead1 && 'r' == lookahead2) {
							if (textIndex < text.length() - 3) {
								// <br>
								final char lookahead3 = text.charAt(textIndex + 3);
								if (lookahead3 == '>') {
									if (sb.length() > 0) {
										tokens.add(Token.text(TokenType.TEXT, sb.toString()));
										// clean string builder
										sb.delete(0, sb.length());
									}
									tokens.add(WRAP_POINT_BR);
									// normal notation <br>
									textIndex += 3;
									consumed = true;
								} else if (textIndex < text.length() - 4) {
									// <br/>
									final char lookahead4 = text.charAt(textIndex + 4);
									if (lookahead3 == '/' && lookahead4 == '>') {
										if (sb.length() > 0) {
											tokens.add(Token.text(TokenType.TEXT, sb.toString()));
											// clean string builder
											sb.delete(0, sb.length());
										}
										tokens.add(WRAP_POINT_BR);
										// normal notation <br/>
										textIndex += 4;
										consumed = true;
									} else if (textIndex < text.length() - 5) {
										final char lookahead5 = text.charAt(textIndex + 5);
										if (lookahead3 == ' ' && lookahead4 == '/' && lookahead5 == '>') {
											if (sb.length() > 0) {
												tokens.add(Token.text(TokenType.TEXT, sb.toString()));
												// clean string builder
												sb.delete(0, sb.length());
											}
											tokens.add(WRAP_POINT_BR);
											// in case it is notation <br />
											textIndex += 5;
											consumed = true;
										}
									}
								}
							}
						} else if ('p' == lookahead1 && '>' == lookahead2) {
							// <p>
							if (sb.length() > 0) {
								tokens.add(Token.text(TokenType.TEXT, sb.toString()));
								// clean string builder
								sb.delete(0, sb.length());
							}
							tokens.add(WRAP_POINT_P);
							textIndex += 2;
							consumed = true;
						} else if ('o' == lookahead1 && 'l' == lookahead2) {
							// <ol>
							if (textIndex < text.length() - 3) {
								final char lookahead3 = text.charAt(textIndex + 3);
								if (lookahead3 == '>') {
									if (sb.length() > 0) {
										tokens.add(Token.text(TokenType.TEXT, sb.toString()));
										// clean string builder
										sb.delete(0, sb.length());
									}
									tokens.add(OPEN_TAG_OL);
									textIndex += 3;
									consumed = true;
								}
							}
						} else if ('u' == lookahead1 && 'l' == lookahead2) {
							// <ul> and <ul type="none">
							if (textIndex < text.length() - 3) {
								final char lookahead3 = text.charAt(textIndex + 3);
								// a plain <ul>, or one carrying attributes: read up to the closing
								// '>' so that type="none" can be recognised
								final int tagEnd = lookahead3 == '>' ? textIndex + 3
										: (lookahead3 == ' ' ? text.indexOf('>', textIndex + 3) : -1);
								if (tagEnd > 0) {
									if (sb.length() > 0) {
										tokens.add(Token.text(TokenType.TEXT, sb.toString()));
										// clean string builder
										sb.delete(0, sb.length());
									}
									tokens.add(isBulletless(text.substring(textIndex, tagEnd + 1))
											? OPEN_TAG_UL_NONE : OPEN_TAG_UL);
									textIndex = tagEnd;
									consumed = true;
								}
							}
						} else if ('l' == lookahead1 && 'i' == lookahead2) {
							// <li>
							if (textIndex < text.length() - 3) {
								final char lookahead3 = text.charAt(textIndex + 3);
								if (lookahead3 == '>') {
									if (sb.length() > 0) {
										tokens.add(Token.text(TokenType.TEXT, sb.toString()));
										// clean string builder
										sb.delete(0, sb.length());
									}
									tokens.add(WRAP_POINT_LI);
									textIndex += 3;
									consumed = true;
								}
							}
						} else if ('a' == lookahead1 && ' ' == lookahead2) {
							// <a href="..."> -> hyperlink open, target URL kept in the token data
							final int gt = text.indexOf('>', textIndex);
							if (gt > 0) {
								final String href = extractHref(text.substring(textIndex, gt));
								if (sb.length() > 0) {
									tokens.add(Token.text(TokenType.TEXT, sb.toString()));
									sb.delete(0, sb.length());
								}
								tokens.add(new Token(TokenType.LINK_OPEN, href == null ? "" : href));
								// jump to the '>'; the loop's textIndex++ then steps past it
								textIndex = gt;
								consumed = true;
							}
						} else if ('/' == lookahead1) {
							// one character tags
							if (textIndex < text.length() - 3) {
								final char lookahead3 = text.charAt(textIndex + 3);
								if ('>' == lookahead3) {
									if ('i' == lookahead2) {
										// </i>
										if (sb.length() > 0) {
											tokens.add(Token.text(TokenType.TEXT, sb.toString()));
											sb.delete(0, sb.length());
										}
										tokens.add(CLOSE_TAG_I);
										textIndex += 3;
										consumed = true;
									} else if ('b' == lookahead2) {
										// </b>
										if (sb.length() > 0) {
											tokens.add(Token.text(TokenType.TEXT, sb.toString()));
											sb.delete(0, sb.length());
										}
										tokens.add(CLOSE_TAG_B);
										textIndex += 3;
										consumed = true;
									} else if ('p' == lookahead2) {
										//</p>
										if (sb.length() > 0) {
											tokens.add(Token.text(TokenType.TEXT, sb.toString()));
											sb.delete(0, sb.length());
										}
										tokens.add(CLOSE_TAG_P);
										textIndex += 3;
										consumed = true;
									} else if ('a' == lookahead2) {
										// </a> -> hyperlink close
										if (sb.length() > 0) {
											tokens.add(Token.text(TokenType.TEXT, sb.toString()));
											sb.delete(0, sb.length());
										}
										tokens.add(new Token(TokenType.LINK_CLOSE, ""));
										textIndex += 3;
										consumed = true;
									}
								}
							}
							if (textIndex < text.length() - 4) {
								// lists
								final char lookahead3 = text.charAt(textIndex + 3);
								final char lookahead4 = text.charAt(textIndex + 4);
								if ('l' == lookahead3) {
									if ('o' == lookahead2 && '>' == lookahead4) {
										// </ol>
										if (sb.length() > 0) {
											tokens.add(Token.text(TokenType.TEXT, sb.toString()));
											sb.delete(0, sb.length());
										}
										tokens.add(CLOSE_TAG_OL);
										textIndex += 4;
										consumed = true;
									} else if ('u' == lookahead2 && '>' == lookahead4) {
										// </ul>
										if (sb.length() > 0) {
											tokens.add(Token.text(TokenType.TEXT, sb.toString()));
											sb.delete(0, sb.length());
										}
										tokens.add(CLOSE_TAG_UL);
										textIndex += 4;
										consumed = true;
									}
								} else if ('l' == lookahead2 && 'i' == lookahead3) {
									// </li>
									if ('>' == lookahead4) {
										if (sb.length() > 0) {
											tokens.add(Token.text(TokenType.TEXT, sb.toString()));
											sb.delete(0, sb.length());
										}
										tokens.add(CLOSE_TAG_LI);
										textIndex += 4;
										consumed = true;
									}
								}
							}
						}
						
					}
					if (!consumed) {
						sb.append('<');
					}
					break;
				default:
					sb.append(c);
					break;
				}
				textIndex++;
			}

			if (sb.length() > 0) {
				tokens.add(Token.text(TokenType.TEXT, sb.toString()));
				sb.delete(0, sb.length());
			}
			tokens.add(POSSIBLE_WRAP_POINT);

			return tokens;
		} else

		{
			return Collections.emptyList();
		}
	}

}
