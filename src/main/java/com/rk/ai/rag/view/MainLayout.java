package com.rk.ai.rag.view;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Main layout for the RAG application with professional navigation.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Spring AI RAG");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.NONE,
            LumoUtility.FontWeight.BOLD
        );
        logo.getStyle()
            .set("color", "var(--lumo-primary-text-color)")
            .set("font-size", "24px");

        Span badge = new Span("v1.0");
        badge.getElement().getThemeList().add("badge contrast small");
        badge.getStyle()
            .set("margin-left", "10px")
            .set("vertical-align", "middle");

        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        HorizontalLayout logoLayout = new HorizontalLayout(logo, badge);
        logoLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(toggle, logoLayout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassNames(
            LumoUtility.Padding.Vertical.SMALL,
            LumoUtility.Padding.Horizontal.MEDIUM
        );
        header.getStyle()
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
            .set("background", "var(--lumo-base-color)");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        
        SideNavItem uploadItem = new SideNavItem("Upload Documents", 
            DocumentUploadView.class, VaadinIcon.UPLOAD.create());
        uploadItem.getStyle().set("font-size", "16px");
        
        SideNavItem queryItem = new SideNavItem("Query System", 
            QueryView.class, VaadinIcon.QUESTION_CIRCLE.create());
        queryItem.getStyle().set("font-size", "16px");

        nav.addItem(uploadItem);
        nav.addItem(queryItem);

        Div description = new Div();
        description.setText("Retrieval-Augmented Generation powered by Spring AI");
        description.getStyle()
            .set("font-size", "12px")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("padding", "var(--lumo-space-m)")
            .set("border-top", "1px solid var(--lumo-contrast-10pct)")
            .set("margin-top", "auto");

        VerticalLayout drawerLayout = new VerticalLayout(nav, description);
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.getStyle().set("background", "var(--lumo-base-color)");

        addToDrawer(drawerLayout);
    }
}
