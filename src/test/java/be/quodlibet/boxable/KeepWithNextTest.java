package be.quodlibet.boxable;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

/**
 * A row can declare that it belongs with the row after it. The page break then looks at the
 * pair rather than at the single row, which is what keeps a heading from being left alone at
 * the bottom of a page while what it introduces starts on the next one.
 */
public class KeepWithNextTest {

    /** Leaves exactly 100 units of usable space on the first page. */
    private static BaseTable tableWithLittleRoomLeft(PDDocument document) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return new BaseTable(200f, 780f, 100f, 400f, 50f, document, page, true, true);
    }

    private static Row<PDPage> addRow(BaseTable table, String text, float height) {
        Row<PDPage> row = table.createRow(height);
        row.createCell(100f, text);
        return row;
    }

    private static String textOfPage(PDDocument document, int page) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        return stripper.getText(document);
    }

    @Test
    public void withoutTheFlagAHeadingIsLeftBehindOnTheFullPage() throws Exception {
        try (PDDocument document = new PDDocument()) {
            BaseTable table = tableWithLittleRoomLeft(document);
            addRow(table, "HEADING", 40f);
            addRow(table, "ITEM", 80f);
            table.draw();

            assertTrue("heading stays on the first page", textOfPage(document, 1).contains("HEADING"));
            assertTrue("item alone on the second", textOfPage(document, 2).contains("ITEM"));
        }
    }

    @Test
    public void aRowCanAskForTheHeaderToBeLeftOutWhenItOpensAPage() throws Exception {
        try (PDDocument document = new PDDocument()) {
            BaseTable table = tableWithLittleRoomLeft(document);
            Row<PDPage> head = table.createRow(4f);
            head.createCell(100f, "REPEATED HEADER");
            table.addHeaderRow(head);

            Row<PDPage> pushed = addRow(table, "SECTION", 40f);
            pushed.setKeepWithNext(true);
            pushed.setOmitHeaderAfterBreak(true);
            addRow(table, "ITEM", 80f);
            table.draw();

            String second = textOfPage(document, 2);
            assertTrue("the section opens the page", second.contains("SECTION"));
            assertTrue("without the header above it", !second.contains("REPEATED HEADER"));
        }
    }

    @Test
    public void withTheFlagBothMoveToTheNextPage() throws Exception {
        try (PDDocument document = new PDDocument()) {
            BaseTable table = tableWithLittleRoomLeft(document);
            addRow(table, "HEADING", 40f).setKeepWithNext(true);
            addRow(table, "ITEM", 80f);
            table.draw();

            assertTrue("nothing of the pair on the first page",
                    !textOfPage(document, 1).contains("HEADING"));
            String second = textOfPage(document, 2);
            assertTrue("heading moved along", second.contains("HEADING"));
            assertTrue("item follows it", second.contains("ITEM"));
        }
    }
}
