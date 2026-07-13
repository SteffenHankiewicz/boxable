package be.quodlibet.boxable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.Test;

/** Verifies that an inline {@code <a href="...">} run becomes a clickable link annotation. */
public class LinkAnnotationTest {

	@Test
	public void anchorBecomesClickableLinkAnnotation() throws IOException {
		try (PDDocument doc = new PDDocument()) {
			PDPage page = new PDPage();
			doc.addPage(page);

			float margin = 50;
			float tableWidth = page.getMediaBox().getWidth() - 2 * margin;
			float yStart = page.getMediaBox().getHeight() - margin;

			BaseTable table = new BaseTable(yStart, yStart, 40, tableWidth, margin, doc, page, true, true);
			Row<PDPage> row = table.createRow(15f);
			row.createCell(100, "Bitte im <a href=\"https://example.com/ticket/42\">Ticket</a> nachsehen.");
			table.draw();

			List<PDAnnotation> annots = page.getAnnotations();
			PDAnnotationLink link = null;
			for (PDAnnotation a : annots) {
				if (a instanceof PDAnnotationLink) {
					link = (PDAnnotationLink) a;
					break;
				}
			}
			assertNotNull("expected a clickable link annotation on the page", link);
			assertTrue("link action should be a URI action", link.getAction() instanceof PDActionURI);
			assertEquals("https://example.com/ticket/42", ((PDActionURI) link.getAction()).getURI());
			assertTrue("link rectangle should have positive width",
					link.getRectangle().getWidth() > 0);
		}
	}
}
