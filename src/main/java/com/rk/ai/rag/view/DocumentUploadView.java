package com.rk.ai.rag.view;

import com.rk.ai.rag.model.UploadResponse;
import com.rk.ai.rag.service.DocumentIngestionService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Upload Documents | Spring AI RAG")
public class DocumentUploadView extends VerticalLayout {

    private final DocumentIngestionService ingestionService;
    private final MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
    private final List<SimpleMultipartFile> filesToUpload = new ArrayList<>();
    private final Grid<UploadResponse.DocumentInfo> resultGrid;
    private final Div statsCard;

    public DocumentUploadView(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;

        setMaxWidth("1200px");
        setMargin(true);
        setPadding(false);
        setSpacing(true);
        getStyle().set("margin", "0 auto");

        // Header section
        Div headerCard = createHeaderCard();
        
        // Upload section
        Div uploadCard = createUploadCard();
        
        // Stats section
        statsCard = createStatsCard();
        statsCard.setVisible(false);

        // Results section
        resultGrid = createResultGrid();
        resultGrid.setVisible(false);

        add(headerCard, uploadCard, statsCard, resultGrid);
    }

    private Div createHeaderCard() {
        Div card = new Div();
        card.addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Background.BASE);
        card.getStyle()
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.08)");

        Icon icon = VaadinIcon.CLOUD_UPLOAD.create();
        icon.setSize("48px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        H2 title = new H2("Document Upload");
        title.getStyle()
            .set("margin", "0")
            .set("color", "var(--lumo-header-text-color)");

        Paragraph description = new Paragraph(
            "Upload your documents to enable AI-powered search and question answering. " +
            "Supports PDF, Word, Excel, CSV, JSON, Markdown, and Text files."
        );
        description.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("margin", "8px 0 0 0");

        HorizontalLayout headerLayout = new HorizontalLayout(icon, 
            new VerticalLayout(title, description));
        headerLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        headerLayout.setPadding(false);
        headerLayout.setSpacing(true);

        card.add(headerLayout);
        return card;
    }

    private Div createUploadCard() {
        Div card = new Div();
        card.addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Background.BASE);
        card.getStyle()
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.08)");

        H3 sectionTitle = new H3("Select Files");
        sectionTitle.getStyle()
            .set("margin", "0 0 16px 0")
            .set("font-size", "18px");

        Upload upload = createUpload();
        
        Div supportedFormats = new Div();
        supportedFormats.getStyle()
            .set("display", "flex")
            .set("flex-wrap", "wrap")
            .set("gap", "8px")
            .set("margin-top", "16px");

        String[] formats = {".pdf", ".doc/.docx", ".xls/.xlsx", ".csv", ".json", ".md", ".txt"};
        for (String format : formats) {
            Span badge = new Span(format);
            badge.getElement().getThemeList().add("badge");
            badge.getStyle()
                .set("background", "var(--lumo-contrast-10pct)")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "12px");
            supportedFormats.add(badge);
        }

        Button processButton = createProcessButton();

        card.add(sectionTitle, upload, supportedFormats, processButton);
        return card;
    }

    private Div createStatsCard() {
        Div card = new Div();
        card.addClassNames(LumoUtility.Padding.LARGE, LumoUtility.Background.PRIMARY_10);
        card.getStyle()
            .set("border-radius", "8px")
            .set("border", "1px solid var(--lumo-primary-color-10pct)");

        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        statsLayout.setPadding(false);

        card.add(statsLayout);
        return card;
    }

    private Upload createUpload() {
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(
            ".txt", ".md", ".markdown",
            ".pdf",
            ".doc", ".docx",
            ".xls", ".xlsx",
            ".csv",
            ".json"
        );
        upload.setMaxFileSize(10 * 1024 * 1024);
        upload.setMaxFiles(10);
        
        upload.getStyle()
            .set("border", "2px dashed var(--lumo-contrast-20pct)")
            .set("border-radius", "8px")
            .set("padding", "16px");

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            try {
                byte[] bytes = buffer.getInputStream(fileName).readAllBytes();
                filesToUpload.add(new SimpleMultipartFile(fileName, bytes));
                
                Notification notification = Notification.show(
                    "✓ " + fileName + " ready",
                    3000,
                    Notification.Position.BOTTOM_END
                );
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (IOException e) {
                Notification notification = Notification.show(
                    "✗ Error loading " + fileName,
                    3000,
                    Notification.Position.BOTTOM_END
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        return upload;
    }

    private Button createProcessButton() {
        Icon icon = VaadinIcon.ROCKET.create();
        icon.getStyle().set("margin-right", "8px");
        
        Button button = new Button("Process Documents", icon);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.getStyle()
            .set("margin-top", "24px")
            .set("width", "100%");
        
        button.addClickListener(e -> processDocuments());
        
        return button;
    }

    private void processDocuments() {
        if (filesToUpload.isEmpty()) {
            Notification notification = Notification.show(
                "Please select at least one file to upload",
                3000,
                Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            MultipartFile[] files = filesToUpload.toArray(new MultipartFile[0]);
            UploadResponse response = ingestionService.ingestDocuments(files);
            
            updateStats(response);
            resultGrid.setItems(response.getDocuments());
            resultGrid.setVisible(true);
            statsCard.setVisible(true);
            
            filesToUpload.clear();
            
            Notification notification = Notification.show(
                "✓ Successfully processed " + response.getProcessedFiles() + " document(s)",
                5000,
                Notification.Position.BOTTOM_END
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
        } catch (Exception e) {
            Notification notification = Notification.show(
                "✗ Error processing documents: " + e.getMessage(),
                5000,
                Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Grid<UploadResponse.DocumentInfo> createResultGrid() {
        Grid<UploadResponse.DocumentInfo> grid = new Grid<>(UploadResponse.DocumentInfo.class, false);
        grid.addClassName("upload-result-grid");
        grid.getStyle()
            .set("border-radius", "8px")
            .set("overflow", "hidden");

        grid.addColumn(UploadResponse.DocumentInfo::getFilename)
            .setHeader("File Name")
            .setAutoWidth(true)
            .setFlexGrow(1);

        grid.addColumn(doc -> formatFileSize(doc.getFileSize()))
            .setHeader("Size")
            .setAutoWidth(true);

        grid.addColumn(UploadResponse.DocumentInfo::getChunks)
            .setHeader("Chunks")
            .setAutoWidth(true);

        grid.addComponentColumn(doc -> {
            Span statusBadge = new Span(doc.getStatus() != null ? doc.getStatus() : "SUCCESS");
            statusBadge.getElement().getThemeList().add("badge");
            if ("SUCCESS".equals(doc.getStatus()) || doc.getStatus() == null) {
                statusBadge.getElement().getThemeList().add("success");
            } else {
                statusBadge.getElement().getThemeList().add("error");
            }
            return statusBadge;
        }).setHeader("Status").setAutoWidth(true);

        return grid;
    }

    private void updateStats(UploadResponse response) {
        HorizontalLayout statsLayout = (HorizontalLayout) statsCard.getChildren().findFirst().orElse(null);
        if (statsLayout != null) {
            statsLayout.removeAll();
            
            statsLayout.add(
                createStatItem("Total Files", String.valueOf(response.getTotalFiles()), VaadinIcon.FILE_O),
                createStatItem("Processed", String.valueOf(response.getProcessedFiles()), VaadinIcon.CHECK_CIRCLE),
                createStatItem("Total Chunks", String.valueOf(response.getTotalChunks()), VaadinIcon.SPLIT)
            );
        }
    }

    private VerticalLayout createStatItem(String label, String value, VaadinIcon iconType) {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Icon icon = iconType.create();
        icon.setSize("24px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        H3 valueLabel = new H3(value);
        valueLabel.getStyle()
            .set("margin", "8px 0 0 0")
            .set("color", "var(--lumo-primary-text-color)");

        Paragraph labelText = new Paragraph(label);
        labelText.getStyle()
            .set("margin", "4px 0 0 0")
            .set("font-size", "14px")
            .set("color", "var(--lumo-secondary-text-color)");

        layout.add(icon, valueLabel, labelText);
        return layout;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }

    /**
     * Simple implementation of MultipartFile for Vaadin Upload component
     */
    private static class SimpleMultipartFile implements MultipartFile {
        private final String filename;
        private final byte[] content;

        public SimpleMultipartFile(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }

        @Override
        public String getName() {
            return filename;
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            throw new UnsupportedOperationException("transferTo not supported");
        }
    }
}
