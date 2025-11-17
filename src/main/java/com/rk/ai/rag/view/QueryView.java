package com.rk.ai.rag.view;

import com.rk.ai.rag.service.RagQueryService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "query", layout = MainLayout.class)
@PageTitle("Query RAG | Spring AI RAG")
public class QueryView extends VerticalLayout {

    private final RagQueryService ragQueryService;
    private final TextField queryField = new TextField("Your Question");
    private final IntegerField topKField = new IntegerField("Top-K Results");
    private final TextArea answerArea = new TextArea("Answer");
    private final Paragraph responseTime = new Paragraph();

    public QueryView(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Query RAG System");
        Paragraph description = new Paragraph(
            "Ask questions about your uploaded documents. The system will search for relevant " +
            "information and generate an answer using AI."
        );

        queryField.setWidthFull();
        queryField.setPlaceholder("e.g., What are the key features?");

        topKField.setValue(5);
        topKField.setMin(1);
        topKField.setMax(20);
        topKField.setHelperText("Number of document chunks to retrieve (1-20)");

        Button queryButton = new Button("Ask Question");
        queryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        queryButton.addClickListener(event -> performQuery());

        answerArea.setWidthFull();
        answerArea.setMinHeight("300px");
        answerArea.setReadOnly(true);
        answerArea.setVisible(false);

        responseTime.setVisible(false);

        add(title, description, queryField, topKField, queryButton, answerArea, responseTime);
    }

    private void performQuery() {
        String query = queryField.getValue();
        if (query == null || query.trim().isEmpty()) {
            Notification.show(
                "Please enter a question",
                3000,
                Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        queryField.setEnabled(false);
        topKField.setEnabled(false);

        try {
            long startTime = System.currentTimeMillis();
            
            String answer = ragQueryService.query(query, topKField.getValue());
            
            long duration = System.currentTimeMillis() - startTime;

            answerArea.setValue(answer);
            answerArea.setVisible(true);

            responseTime.setText(String.format("Response time: %d ms", duration));
            responseTime.setVisible(true);

        } catch (Exception e) {
            Notification.show(
                "Error: " + e.getMessage(),
                5000,
                Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            queryField.setEnabled(true);
            topKField.setEnabled(true);
        }
    }
}
