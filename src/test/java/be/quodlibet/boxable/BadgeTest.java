package be.quodlibet.boxable;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

/** Verifies that an inline {@code <badge>} is drawn as its text, without the markup. */
public class BadgeTest {

	@Test
	public void badgeIsDrawnAsItsText() throws IOException {
		try (PDDocument doc = new PDDocument()) {
			PDPage page = new PDPage();
			doc.addPage(page);

			float margin = 50;
			float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
			float yStart = page.getMediaBox().getHeight() - margin;

			BaseTable table = new BaseTable(yStart, yStart, 40, tableWidth, margin, doc, page, true, true);
			Row<PDPage> row = table.createRow(15f);
			row.createCell(100, "Waiting for <badge bg=\"#c6e0f8\" fg=\"#135696\">Geoff</badge> and more.");
			table.draw();

			String text = new PDFTextStripper().getText(doc);
			assertTrue("badge text expected: " + text, text.contains("Waiting for Geoff and more."));
		}
	}
}
