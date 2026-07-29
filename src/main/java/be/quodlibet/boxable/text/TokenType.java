package be.quodlibet.boxable.text;

public enum TokenType {
	TEXT, POSSIBLE_WRAP_POINT, WRAP_POINT, OPEN_TAG, CLOSE_TAG, PADDING, BULLET, ORDERING,
	// the marker of a nested list: same width as BULLET, drawn as a dash instead of a square so
	// the level an item belongs to can be told at a glance.
	BULLET_SUB,
	// hyperlink markers: LINK_OPEN carries the target URL in its data, LINK_CLOSE ends it.
	// Zero-width, they let the renderer place a clickable PDAnnotationLink over the run.
	LINK_OPEN, LINK_CLOSE
}
