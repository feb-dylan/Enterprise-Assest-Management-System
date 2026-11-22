package com.eams.service;

import com.eams.config.AppProperties;
import com.eams.entity.Asset;
import com.eams.exception.BadRequestException;
import com.eams.repository.AssetRepository;
import com.eams.service.impl.AssetServiceImpl;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A4 sheet of QR stickers (3 per row) for the selected assets.
 * Each sticker: QR code (opens the asset page when scanned) + code + name.
 */
@Service
@RequiredArgsConstructor
public class QrLabelService {

    private static final int MAX_LABELS = 300;

    private final AssetRepository assetRepository;
    private final AppProperties properties;

    @Transactional(readOnly = true)
    public byte[] labelsPdf(List<Long> assetIds) {
        List<Asset> assets = (assetIds == null || assetIds.isEmpty())
                ? assetRepository.findAll()
                : assetRepository.findAllById(assetIds);
        if (assets.isEmpty()) {
            throw new BadRequestException("No assets selected");
        }
        if (assets.size() > MAX_LABELS) {
            throw new BadRequestException("At most " + MAX_LABELS + " labels per sheet");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 24, 24, 24, 24);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);

            Font codeFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font nameFont = new Font(Font.HELVETICA, 8, Font.NORMAL);

            for (Asset asset : assets) {
                String url = properties.frontendUrl() + "/a/"
                        + URLEncoder.encode(asset.getAssetCode(), StandardCharsets.UTF_8);
                Image qr = Image.getInstance(AssetServiceImpl.renderQr(url, 220));
                qr.scaleToFit(110, 110);
                qr.setAlignment(Element.ALIGN_CENTER);

                PdfPCell cell = new PdfPCell();
                cell.setPadding(8);
                cell.setFixedHeight(175);
                cell.addElement(qr);

                Paragraph code = new Paragraph(asset.getAssetCode(), codeFont);
                code.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(code);

                String name = asset.getName().length() > 40
                        ? asset.getName().substring(0, 40) + "..."
                        : asset.getName();
                Paragraph label = new Paragraph(new Phrase(name, nameFont));
                label.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(label);

                table.addCell(cell);
            }
            table.completeRow();
            document.add(table);
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate label PDF", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }
}
